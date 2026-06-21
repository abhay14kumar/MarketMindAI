package com.marketmind.sources.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.marketmind.sources.application.ReachabilityChecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HttpReachabilityClient implements ReachabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpReachabilityClient.class);

    private final ValidationHttpGateway gateway;
    private final Clock clock;

    public HttpReachabilityClient(ValidationHttpGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public ReachabilityResult check(URI sourceUrl) {
        ValidationHttpGateway.validateHttpUrl(sourceUrl);
        Instant startedAt = clock.instant();
        Exception headFailure = null;
        try {
            ValidationHttpGateway.Response headResponse =
                    gateway.execute(sourceUrl, "HEAD", "*/*", 0);
            if (headResponse.statusCode() == 405 || headResponse.statusCode() == 501) {
                return evaluate(
                        gateway.execute(sourceUrl, "GET", "*/*", 0),
                        startedAt);
            }
            return evaluate(headResponse, startedAt);
        } catch (IOException exception) {
            headFailure = exception;
            LOGGER.debug("HEAD reachability check failed for {}", sourceUrl, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(startedAt, "Source reachability check was interrupted.");
        }

        try {
            return evaluate(
                    gateway.execute(sourceUrl, "GET", "*/*", 0),
                    startedAt);
        } catch (HttpTimeoutException exception) {
            LOGGER.debug("GET reachability fallback timed out for {}", sourceUrl, exception);
            return failure(startedAt, "Source reachability checks timed out.");
        } catch (IOException exception) {
            LOGGER.debug("GET reachability fallback failed for {}", sourceUrl, exception);
            return failure(startedAt, userFacingFailure(headFailure, exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(startedAt, "Source reachability check was interrupted.");
        }
    }

    private ReachabilityResult evaluate(
            ValidationHttpGateway.Response response,
            Instant startedAt) {
        long latency = elapsedMillis(startedAt);
        boolean reachable = response.statusCode() >= 200 && response.statusCode() < 500;
        return new ReachabilityResult(
                reachable,
                response.statusCode(),
                latency,
                reachable
                        ? "Source responded to the reachability check."
                        : "Source returned HTTP status " + response.statusCode() + ".");
    }

    private ReachabilityResult failure(Instant startedAt, String message) {
        return new ReachabilityResult(false, null, elapsedMillis(startedAt), message);
    }

    private long elapsedMillis(Instant startedAt) {
        return Math.max(0, Duration.between(startedAt, clock.instant()).toMillis());
    }

    private String userFacingFailure(Exception headFailure, Exception getFailure) {
        if (headFailure instanceof HttpTimeoutException
                || getFailure instanceof HttpTimeoutException) {
            return "Source reachability checks timed out.";
        }
        return "Source did not complete the HTTP reachability checks.";
    }
}
