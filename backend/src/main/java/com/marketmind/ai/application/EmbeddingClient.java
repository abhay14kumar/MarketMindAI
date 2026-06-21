package com.marketmind.ai.application;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);
}
