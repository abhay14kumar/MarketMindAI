package com.marketmind.sources.infrastructure;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ValidationHttpGateway {

    private static final int MAX_REDIRECTS = 3;

    private final HttpClient httpClient;
    private final SourceValidationProperties properties;

    @Autowired
    ValidationHttpGateway(SourceValidationProperties properties) {
        this(createHttpClient(properties), properties);
    }

    ValidationHttpGateway(HttpClient httpClient, SourceValidationProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    Response execute(URI uri, String method, String accept, int bodyPrefixBytes)
            throws IOException, InterruptedException {
        return execute(uri, method, accept, bodyPrefixBytes, 0);
    }

    private Response execute(
            URI uri,
            String method,
            String accept,
            int bodyPrefixBytes,
            int redirectCount)
            throws IOException, InterruptedException {
        validateHttpUrl(uri);
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("The source exceeded the maximum redirect count.");
        }

        HttpRequest request = buildRequest(uri, method, accept);
        HttpResponse<byte[]> response = httpClient.send(
                request,
                responseInfo -> bodyPrefixBytes == 0
                        ? HttpResponse.BodySubscribers.replacing(new byte[0])
                        : limitingSubscriber(bodyPrefixBytes));
        if (!isRedirect(response.statusCode())) {
            return new Response(
                    response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(null),
                    response.headers().firstValueAsLong("Content-Length").orElse(-1),
                    response.body(),
                    uri);
        }

        String location = response.headers().firstValue("Location")
                .orElseThrow(() -> new IOException(
                        "The source returned a redirect without a Location header."));
        URI redirectUri;
        try {
            redirectUri = uri.resolve(location);
        } catch (IllegalArgumentException exception) {
            throw new IOException("The source returned an invalid redirect URL.", exception);
        }
        return execute(redirectUri, method, accept, bodyPrefixBytes, redirectCount + 1);
    }

    HttpRequest buildRequest(URI uri, String method, String accept) {
        return HttpRequest.newBuilder(uri)
                .timeout(properties.readTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .header("User-Agent", properties.userAgent())
                .header("Accept", accept)
                .header("Accept-Language", "en-US,en;q=0.9")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
    }

    static void validateHttpUrl(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Source URL is required.");
        }
        String scheme = Optional.ofNullable(uri.getScheme())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS source URLs are supported.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "Source URL must contain a valid host and no user information.");
        }
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static HttpClient createHttpClient(SourceValidationProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private BodySubscriber<byte[]> limitingSubscriber(int maximumBytes) {
        if (maximumBytes < 0) {
            throw new IllegalArgumentException("Maximum response prefix must not be negative.");
        }
        return new LimitedBodySubscriber(maximumBytes);
    }

    record Response(
            int statusCode,
            String contentType,
            long contentLength,
            byte[] bodyPrefix,
            URI finalUri) {
    }

    private static final class LimitedBodySubscriber implements BodySubscriber<byte[]> {

        private final int maximumBytes;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final ByteArrayOutputStream output;
        private Flow.Subscription subscription;

        private LimitedBodySubscriber(int maximumBytes) {
            this.maximumBytes = maximumBytes;
            this.output = new ByteArrayOutputStream(maximumBytes);
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            if (maximumBytes == 0) {
                subscription.cancel();
                body.complete(new byte[0]);
                return;
            }
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                int bytesToRead = Math.min(buffer.remaining(), maximumBytes - output.size());
                if (bytesToRead > 0) {
                    byte[] bytes = new byte[bytesToRead];
                    buffer.get(bytes);
                    output.writeBytes(bytes);
                }
                if (output.size() == maximumBytes) {
                    subscription.cancel();
                    body.complete(output.toByteArray());
                    return;
                }
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }
    }
}
