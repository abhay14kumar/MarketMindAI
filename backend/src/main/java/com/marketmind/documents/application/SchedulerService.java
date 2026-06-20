package com.marketmind.documents.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SchedulerService {

    List<UUID> scheduleDueSources(Instant asOf);
}
