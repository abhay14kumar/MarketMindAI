package com.marketmind.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.marketmind.documents.application.StorageProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageProviderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldCreateChecksumPartitionedStoragePath() throws Exception {
        byte[] content = "marketmind".getBytes(StandardCharsets.UTF_8);
        String checksum = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        LocalFileStorageProvider storage = new LocalFileStorageProvider(
                new DocumentStorageProperties(temporaryDirectory));

        StorageProvider.StoredObject stored = storage.store(new StorageProvider.StoreRequest(
                "../Annual Report 2026.pdf",
                "application/pdf",
                content.length,
                checksum,
                Instant.parse("2026-06-19T12:00:00Z"),
                new ByteArrayInputStream(content)));

        assertThat(stored.storageReference())
                .isEqualTo("2026/06/" + checksum + "/Annual_Report_2026.pdf");
        assertThat(Files.readAllBytes(temporaryDirectory.resolve(stored.storageReference())))
                .isEqualTo(content);
    }
}
