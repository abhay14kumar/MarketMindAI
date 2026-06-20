package com.marketmind.documents.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.marketmind.documents.application.DocumentPipelineException;
import com.marketmind.documents.application.StorageProvider;

import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageProvider implements StorageProvider {

    private final Path rootPath;

    public LocalFileStorageProvider(DocumentStorageProperties properties) {
        this.rootPath = properties.rootPath().toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(StoreRequest request) {
        validateChecksum(request.checksumSha256());
        ZonedDateTime acquiredAt = request.acquiredAt().atZone(ZoneOffset.UTC);
        String safeFileName = sanitizeFileName(request.objectName());
        Path destinationDirectory = rootPath
                .resolve(String.valueOf(acquiredAt.getYear()))
                .resolve("%02d".formatted(acquiredAt.getMonthValue()))
                .resolve(request.checksumSha256())
                .normalize();
        ensureInsideRoot(destinationDirectory);
        Path destination = destinationDirectory.resolve(safeFileName).normalize();
        ensureInsideRoot(destination);

        try {
            Files.createDirectories(destinationDirectory);
            Path temporary = Files.createTempFile(destinationDirectory, ".upload-", ".tmp");
            try {
                long written;
                try (var output = Files.newOutputStream(
                        temporary,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    written = request.content().transferTo(output);
                }
                if (written != request.contentLength()) {
                    throw DocumentPipelineException.storageFailure(
                            "Stored document size does not match the downloaded size.",
                            new IOException("Expected " + request.contentLength() + " bytes, wrote " + written));
                }
                moveAtomically(temporary, destination);
            } finally {
                Files.deleteIfExists(temporary);
            }
            String storageReference = rootPath.relativize(destination)
                    .toString()
                    .replace(destination.getFileSystem().getSeparator(), "/");
            return new StoredObject(
                    storageReference,
                    request.checksumSha256(),
                    request.contentLength());
        } catch (DocumentPipelineException exception) {
            throw exception;
        } catch (IOException exception) {
            throw DocumentPipelineException.storageFailure(
                    "Unable to store the downloaded document.", exception);
        }
    }

    @Override
    public InputStream load(String storageReference) {
        Path storedObject = rootPath.resolve(storageReference).normalize();
        ensureInsideRoot(storedObject);
        try {
            return Files.newInputStream(storedObject);
        } catch (IOException exception) {
            throw DocumentPipelineException.storageFailure(
                    "Unable to load the stored document.", exception);
        }
    }

    private void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private String sanitizeFileName(String value) {
        String normalized = value == null ? "" : value.replace('\\', '/');
        int lastSeparator = normalized.lastIndexOf('/');
        String fileName = lastSeparator >= 0
                ? normalized.substring(lastSeparator + 1)
                : normalized;
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("..")) {
            return "document.bin";
        }
        return sanitized.length() > 180 ? sanitized.substring(sanitized.length() - 180) : sanitized;
    }

    private void validateChecksum(String checksum) {
        if (checksum == null || !checksum.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("A valid lowercase SHA-256 checksum is required.");
        }
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(rootPath)) {
            throw DocumentPipelineException.storageFailure(
                    "The storage path escapes the configured root.",
                    new IOException("Unsafe storage path"));
        }
    }
}
