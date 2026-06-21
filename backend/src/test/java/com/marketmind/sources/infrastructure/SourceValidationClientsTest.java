package com.marketmind.sources.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.marketmind.sources.domain.CapabilityStatus;

import org.junit.jupiter.api.Test;

class SourceValidationClientsTest {

    private static final SourceValidationProperties PROPERTIES =
            new SourceValidationProperties(10, 15, 10, "MarketMindAI-Test/1.0");

    @Test
    void shouldValidateReachableHttpUrl() {
        StubGateway gateway = new StubGateway(response(200, "text/html", new byte[0]));
        HttpReachabilityClient client = new HttpReachabilityClient(gateway, Clock.systemUTC());

        var result = client.check(URI.create("https://example.com"));

        assertThat(result.reachable()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(gateway.lastMethod).isEqualTo("HEAD");
    }

    @Test
    void shouldRejectUnsupportedUrlProtocol() {
        HttpReachabilityClient client = new HttpReachabilityClient(
                new StubGateway(response(200, "text/html", new byte[0])),
                Clock.systemUTC());

        assertThatThrownBy(() -> client.check(URI.create("file:///etc/passwd")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP and HTTPS");
    }

    @Test
    void shouldConstructRobotsTxtUrlFromOrigin() {
        RobotsTxtClient client = new RobotsTxtClient(
                new StubGateway(response(200, "text/plain", new byte[0])));

        URI robotsUrl = client.buildRobotsTxtUrl(
                URI.create("https://example.com:8443/investors/reports?year=2026"));

        assertThat(robotsUrl).isEqualTo(URI.create("https://example.com:8443/robots.txt"));
    }

    @Test
    void shouldDetectPdfByContentType() {
        PdfCapabilityClient client = new PdfCapabilityClient(
                new StubGateway(response(200, "application/pdf; charset=binary", new byte[0])),
                PROPERTIES);

        var result = client.check(URI.create("https://example.com/report"));

        assertThat(result.status()).isEqualTo(CapabilityStatus.SUPPORTED);
    }

    @Test
    void shouldDetectPdfByMagicBytes() {
        PdfCapabilityClient client = new PdfCapabilityClient(
                new StubGateway(response(
                        200,
                        "application/octet-stream",
                        "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII))),
                PROPERTIES);

        var result = client.check(URI.create("https://example.com/report.bin"));

        assertThat(result.status()).isEqualTo(CapabilityStatus.SUPPORTED);
    }

    @Test
    void shouldHandleTimeoutWithoutThrowing() {
        StubGateway gateway = new StubGateway(null);
        gateway.addFailure(new HttpTimeoutException("HEAD timeout"));
        gateway.addFailure(new HttpTimeoutException("GET timeout"));
        HttpReachabilityClient client = new HttpReachabilityClient(gateway, Clock.systemUTC());

        var result = client.check(URI.create("https://example.com"));

        assertThat(result.reachable()).isFalse();
        assertThat(result.httpStatus()).isNull();
        assertThat(result.message()).contains("timed out");
    }

    @Test
    void shouldFallbackToGetWhenHeadFails() {
        StubGateway gateway = new StubGateway(null);
        gateway.addFailure(new IOException("HEAD request rejected"));
        gateway.addResponse(response(200, "text/html", new byte[0]));
        HttpReachabilityClient client = new HttpReachabilityClient(gateway, Clock.systemUTC());

        var result = client.check(URI.create("https://example.com"));

        assertThat(result.reachable()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(gateway.methods).containsExactly("HEAD", "GET");
    }

    @Test
    void shouldSanitizeHttp2StreamCancellationWhenBothAttemptsFail() {
        StubGateway gateway = new StubGateway(null);
        gateway.addFailure(new IOException("Stream 1 cancelled"));
        gateway.addFailure(new IOException("Stream 3 cancelled"));
        HttpReachabilityClient client = new HttpReachabilityClient(gateway, Clock.systemUTC());

        var result = client.check(URI.create("https://example.com"));

        assertThat(result.reachable()).isFalse();
        assertThat(result.message())
                .isEqualTo("Source did not complete the HTTP reachability checks.")
                .doesNotContain("Stream", "cancelled");
        assertThat(gateway.methods).containsExactly("HEAD", "GET");
    }

    @Test
    void shouldUseHttp11AndSafeBrowserLikeHeaders() {
        ValidationHttpGateway gateway = new ValidationHttpGateway(PROPERTIES);

        var request = gateway.buildRequest(
                URI.create("https://example.com"),
                "HEAD",
                "text/html,*/*;q=0.8");

        assertThat(request.version()).contains(HttpClient.Version.HTTP_1_1);
        assertThat(request.headers().firstValue("User-Agent"))
                .contains("MarketMindAI-Test/1.0");
        assertThat(request.headers().firstValue("Accept"))
                .contains("text/html,*/*;q=0.8");
        assertThat(request.headers().firstValue("Accept-Language"))
                .contains("en-US,en;q=0.9");
        assertThat(request.headers().firstValue("Connection")).isEmpty();
    }

    private static ValidationHttpGateway.Response response(
            int status,
            String contentType,
            byte[] body) {
        return new ValidationHttpGateway.Response(
                status,
                contentType,
                body.length,
                body,
                URI.create("https://example.com"));
    }

    private static final class StubGateway extends ValidationHttpGateway {

        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<String> methods = new ArrayList<>();
        private String lastMethod;

        private StubGateway(ValidationHttpGateway.Response response) {
            super(PROPERTIES);
            if (response != null) {
                outcomes.add(response);
            }
        }

        private void addResponse(ValidationHttpGateway.Response response) {
            outcomes.add(response);
        }

        private void addFailure(IOException failure) {
            outcomes.add(failure);
        }

        @Override
        Response execute(URI uri, String method, String accept, int bodyPrefixBytes)
                throws IOException {
            lastMethod = method;
            methods.add(method);
            Object outcome = outcomes.removeFirst();
            if (outcome instanceof IOException failure) {
                throw failure;
            }
            return (Response) outcome;
        }
    }
}
