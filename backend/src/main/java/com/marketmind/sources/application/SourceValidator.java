package com.marketmind.sources.application;

import com.marketmind.sources.domain.SourceRegistry;
import com.marketmind.sources.domain.SourceValidationHistory;

public interface SourceValidator {

    SourceValidationHistory validate(SourceRegistry source);
}
