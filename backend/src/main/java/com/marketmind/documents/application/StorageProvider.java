package com.marketmind.documents.application;

import java.io.InputStream;

public interface StorageProvider {

    StoredObject store(StoreRequest request);

    InputStream load(String storageReference);

    record StoreRequest(
            String objectName,
            String contentType,
            long contentLength,
            InputStream content) {
    }

    record StoredObject(String storageReference, String checksumSha256, long sizeBytes) {
    }
}
