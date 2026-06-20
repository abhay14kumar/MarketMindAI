package com.marketmind.documents.application;

import java.util.Map;

public interface Parser {

    ParseResult parse(ParseRequest request);

    record ParseRequest(byte[] content, String contentType) {
        public ParseRequest {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    record ParseResult(String text, Map<String, String> metadata) {
        public ParseResult {
            metadata = Map.copyOf(metadata);
        }
    }
}
