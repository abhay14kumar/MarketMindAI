package com.marketmind.pipeline.infrastructure;

import java.time.Duration;

import com.marketmind.pipeline.application.PipelineSleeper;

import org.springframework.stereotype.Component;

@Component
public class ThreadPipelineSleeper implements PipelineSleeper {

    @Override
    public void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pipeline retry wait was interrupted.", exception);
        }
    }
}
