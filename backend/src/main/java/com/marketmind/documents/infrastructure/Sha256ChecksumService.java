package com.marketmind.documents.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.marketmind.documents.application.ChecksumService;
import com.marketmind.documents.application.DocumentPipelineException;

import org.springframework.stereotype.Component;

@Component
public class Sha256ChecksumService implements ChecksumService {

    @Override
    public String sha256(InputStream content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = content.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw DocumentPipelineException.storageFailure(
                    "Unable to calculate the document checksum.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
