package com.marketmind.documents.application;

import java.io.InputStream;

public interface ChecksumService {

    String sha256(InputStream content);
}
