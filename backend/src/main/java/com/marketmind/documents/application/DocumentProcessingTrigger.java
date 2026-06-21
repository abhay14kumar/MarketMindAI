package com.marketmind.documents.application;

import java.util.UUID;

/**
 * Output port used by document acquisition to notify downstream processors.
 */
public interface DocumentProcessingTrigger {

    void documentDownloaded(UUID documentId);
}
