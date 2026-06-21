package com.marketmind.ai.application;

public interface ChatClient {

    String answer(String question, String groundedContext);
}
