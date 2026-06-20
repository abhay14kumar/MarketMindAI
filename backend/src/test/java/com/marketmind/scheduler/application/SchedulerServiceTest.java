package com.marketmind.scheduler.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerRun;
import com.marketmind.scheduler.domain.SchedulerRunStatus;
import com.marketmind.scheduler.domain.SchedulerType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID ACTIVE_JOB_ID =
            UUID.fromString("61000000-0000-0000-0000-000000000001");
    private static final UUID PAUSED_JOB_ID =
            UUID.fromString("61000000-0000-0000-0000-000000000002");

    private InMemorySchedulerRepository repository;
    private SchedulerService service;

    @BeforeEach
    void setUp() {
        repository = new InMemorySchedulerRepository();
        service = new SchedulerService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldReturnPaginatedJobs() {
        PageResult<SchedulerJob> result = service.getJobs(0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldCreateSchedulerJob() {
        SchedulerJob created = service.createJob(command("BSE Filing Discovery"));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("BSE Filing Discovery");
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldUpdateSchedulerJobAndPreserveIdentity() {
        SchedulerJob updated = service.updateJob(
                ACTIVE_JOB_ID,
                new SchedulerJobCommand(
                        "NSE Filing Discovery Updated",
                        "Updated description",
                        SchedulerType.NSE_FILINGS,
                        SchedulerJobStatus.PAUSED,
                        "0 0 * * * *",
                        "Asia/Kolkata",
                        Map.of()));

        assertThat(updated.id()).isEqualTo(ACTIVE_JOB_ID);
        assertThat(updated.status()).isEqualTo(SchedulerJobStatus.PAUSED);
        assertThat(updated.createdAt()).isEqualTo(NOW.minusSeconds(3600));
        assertThat(updated.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldTriggerActiveJobAsQueuedMockRun() {
        SchedulerRun run = service.triggerJob(ACTIVE_JOB_ID);

        assertThat(run.schedulerJobId()).isEqualTo(ACTIVE_JOB_ID);
        assertThat(run.status()).isEqualTo(SchedulerRunStatus.QUEUED);
        assertThat(run.triggerType()).isEqualTo("MANUAL");
        assertThat(run.queuedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectTriggerForPausedJob() {
        assertThatThrownBy(() -> service.triggerJob(PAUSED_JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active");
    }

    @Test
    void shouldRejectInvalidCronExpression() {
        SchedulerJobCommand invalid = new SchedulerJobCommand(
                "Invalid Cron",
                null,
                SchedulerType.CUSTOM,
                SchedulerJobStatus.ACTIVE,
                "not-a-cron",
                "UTC",
                Map.of());

        assertThatThrownBy(() -> service.createJob(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cron");
    }

    @Test
    void shouldReturnNotFoundForUnknownJob() {
        UUID missingId = UUID.fromString("61000000-0000-0000-0000-000000000099");

        assertThatThrownBy(() -> service.getJob(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    private SchedulerJobCommand command(String name) {
        return new SchedulerJobCommand(
                name,
                "Discovers official filings.",
                SchedulerType.BSE_FILINGS,
                SchedulerJobStatus.ACTIVE,
                "0 0/30 * * * *",
                "Asia/Kolkata",
                Map.of("sourceCode", "BSE"));
    }

    private static final class InMemorySchedulerRepository implements SchedulerRepository {

        private final Map<UUID, SchedulerJob> jobs = new LinkedHashMap<>();
        private final List<SchedulerRun> runs = new ArrayList<>();

        private InMemorySchedulerRepository() {
            jobs.put(ACTIVE_JOB_ID, job(
                    ACTIVE_JOB_ID,
                    "NSE Filing Discovery",
                    SchedulerJobStatus.ACTIVE));
            jobs.put(PAUSED_JOB_ID, job(
                    PAUSED_JOB_ID,
                    "SEBI Filing Discovery",
                    SchedulerJobStatus.PAUSED));
        }

        @Override
        public List<SchedulerJob> findAllJobs() {
            return List.copyOf(jobs.values());
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
            return List.copyOf(runs);
        }

        @Override
        public SchedulerRun saveRun(SchedulerRun run) {
            runs.add(run);
            return run;
        }

        private SchedulerJob job(UUID id, String name, SchedulerJobStatus status) {
            return new SchedulerJob(
                    id,
                    name,
                    null,
                    SchedulerType.NSE_FILINGS,
                    status,
                    "0 0/30 * * * *",
                    "Asia/Kolkata",
                    Map.of(),
                    null,
                    null,
                    NOW.minusSeconds(3600),
                    NOW.minusSeconds(1800));
        }
    }
}
