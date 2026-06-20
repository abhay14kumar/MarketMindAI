package com.marketmind.scheduler.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.marketmind.scheduler.application.SchedulerRepository;
import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerRun;
import com.marketmind.scheduler.domain.SchedulerRunStatus;
import com.marketmind.scheduler.domain.SchedulerType;

import org.springframework.stereotype.Component;

@Component
public class MockSchedulerRepository implements SchedulerRepository {

    private static final Instant MOCK_TIME = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID NSE_JOB_ID =
            UUID.fromString("61000000-0000-0000-0000-000000000001");

    private final Map<UUID, SchedulerJob> jobs = new ConcurrentHashMap<>();
    private final Map<UUID, SchedulerRun> runs = new ConcurrentHashMap<>();

    public MockSchedulerRepository() {
        SchedulerJob nseJob = new SchedulerJob(
                NSE_JOB_ID,
                "NSE Filing Discovery",
                "Discovers official NSE company filings.",
                SchedulerType.NSE_FILINGS,
                SchedulerJobStatus.ACTIVE,
                "0 0/30 * * * *",
                "Asia/Kolkata",
                Map.of("sourceCode", "NSE"),
                MOCK_TIME.plusSeconds(1800),
                MOCK_TIME.minusSeconds(1800),
                MOCK_TIME.minusSeconds(86_400),
                MOCK_TIME);
        SchedulerJob sebiJob = new SchedulerJob(
                UUID.fromString("61000000-0000-0000-0000-000000000002"),
                "SEBI Filing Discovery",
                "Reserved for future SEBI filing discovery.",
                SchedulerType.SEBI_FILINGS,
                SchedulerJobStatus.PAUSED,
                "0 0 6 * * *",
                "Asia/Kolkata",
                Map.of("sourceCode", "SEBI"),
                null,
                null,
                MOCK_TIME.minusSeconds(43_200),
                MOCK_TIME);
        jobs.put(nseJob.id(), nseJob);
        jobs.put(sebiJob.id(), sebiJob);

        SchedulerRun completedRun = new SchedulerRun(
                UUID.fromString("62000000-0000-0000-0000-000000000001"),
                NSE_JOB_ID,
                SchedulerRunStatus.COMPLETED,
                "SCHEDULED",
                MOCK_TIME.minusSeconds(1860),
                MOCK_TIME.minusSeconds(1800),
                MOCK_TIME.minusSeconds(1740),
                12,
                "mock-scheduler-run-001",
                MOCK_TIME.minusSeconds(1860),
                MOCK_TIME.minusSeconds(1740));
        runs.put(completedRun.id(), completedRun);
    }

    @Override
    public List<SchedulerJob> findAllJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(SchedulerJob::name))
                .toList();
    }

    @Override
    public Optional<SchedulerJob> findJobById(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public boolean existsJobByName(String name, UUID excludedId) {
        return jobs.values().stream()
                .anyMatch(job -> !job.id().equals(excludedId)
                        && job.name().equalsIgnoreCase(name));
    }

    @Override
    public SchedulerJob saveJob(SchedulerJob job) {
        jobs.put(job.id(), job);
        return job;
    }

    @Override
    public List<SchedulerRun> findAllRuns() {
        List<SchedulerRun> result = new ArrayList<>(runs.values());
        result.sort(Comparator.comparing(SchedulerRun::queuedAt).reversed());
        return List.copyOf(result);
    }

    @Override
    public SchedulerRun saveRun(SchedulerRun run) {
        runs.put(run.id(), run);
        return run;
    }
}
