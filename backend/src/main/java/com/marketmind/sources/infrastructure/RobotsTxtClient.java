package com.marketmind.sources.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.marketmind.sources.application.RobotsTxtChecker;

import org.springframework.stereotype.Component;

@Component
public class RobotsTxtClient implements RobotsTxtChecker {

    private final ValidationHttpGateway gateway;

    public RobotsTxtClient(ValidationHttpGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public RobotsTxtResult check(URI baseUrl) {
        URI robotsUrl = buildRobotsTxtUrl(baseUrl);
        try {
            ValidationHttpGateway.Response response =
                    gateway.execute(robotsUrl, "GET", "text/plain,*/*;q=0.5", 0);
            boolean available = response.statusCode() >= 200 && response.statusCode() < 300;
            return new RobotsTxtResult(
                    available,
                    response.statusCode(),
                    available
                            ? "robots.txt is available."
                            : "robots.txt returned HTTP status " + response.statusCode() + ".");
        } catch (IOException exception) {
            return new RobotsTxtResult(false, null, "robots.txt could not be checked.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new RobotsTxtResult(false, null, "robots.txt check was interrupted.");
        }
    }

    public URI buildRobotsTxtUrl(URI baseUrl) {
        ValidationHttpGateway.validateHttpUrl(baseUrl);
        try {
            return new URI(
                    baseUrl.getScheme(),
                    null,
                    baseUrl.getHost(),
                    baseUrl.getPort(),
                    "/robots.txt",
                    null,
                    null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Unable to construct robots.txt URL.", exception);
        }
    }
}
