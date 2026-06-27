package com.marketmind.pipeline.application;

import java.time.Duration;

public interface PipelineSleeper {

    void sleep(Duration duration);
}
