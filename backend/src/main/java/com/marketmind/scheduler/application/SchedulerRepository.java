package com.marketmind.scheduler.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerRun;

public interface SchedulerRepository {

    List<SchedulerJob> findAllJobs();

    Optional<SchedulerJob> findJobById(UUID id);

    boolean existsJobByName(String name, UUID excludedId);

    SchedulerJob saveJob(SchedulerJob job);

    List<SchedulerRun> findAllRuns();

    SchedulerRun saveRun(SchedulerRun run);
}
