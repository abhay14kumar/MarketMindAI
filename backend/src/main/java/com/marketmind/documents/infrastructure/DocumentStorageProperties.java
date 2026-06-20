package com.marketmind.documents.infrastructure;

import java.nio.file.Path;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "document.storage")
public record DocumentStorageProperties(@NotNull Path rootPath) {
}
