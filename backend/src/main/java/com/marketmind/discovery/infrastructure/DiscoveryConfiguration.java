package com.marketmind.discovery.infrastructure;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DiscoveryProperties.class)
public class DiscoveryConfiguration {

    @Bean
    HttpClient discoveryHttpClient(DiscoveryProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
}
