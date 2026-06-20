package com.marketmind.scheduler.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerRun;
import com.marketmind.scheduler.domain.SchedulerRunStatus;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.support.CronExpression;

@Service
public class SchedulerService {

    private final SchedulerRepository schedulerRepository;
    private final Clock clock;

    public SchedulerService(SchedulerRepository schedulerRepository, Clock clock) {
        this.schedulerRepository = schedulerRepository;
        this.clock = clock;
    }

    public PageResult<SchedulerJob> getJobs(int page, int size) {
        return page(schedulerRepository.findAllJobs(), page, size);
    }

    public SchedulerJob getJob(UUID id) {
        return schedulerRepository.findJobById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler job not found: " + id));
    }

    public SchedulerJob createJob(SchedulerJobCommand command) {
        validateCommand(command);
        String normalizedName = command.name().trim();
        ensureUniqueName(normalizedName, null);
        Instant now = clock.instant();
        return schedulerRepository.saveJob(new SchedulerJob(
                UUID.randomUUID(),
                normalizedName,
                trimToNull(command.description()),
                command.schedulerType(),
                command.status(),
                command.cronExpression().trim(),
                command.timeZone().trim(),
                command.configuration(),
                null,
                null,
                now,
                now));
    }

    public SchedulerJob updateJob(UUID id, SchedulerJobCommand command) {
        SchedulerJob existing = getJob(id);
        validateCommand(command);
        String normalizedName = command.name().trim();
        ensureUniqueName(normalizedName, id);
        return schedulerRepository.saveJob(new SchedulerJob(
                existing.id(),
                normalizedName,
                trimToNull(command.description()),
                command.schedulerType(),
                command.status(),
                command.cronExpression().trim(),
                command.timeZone().trim(),
                command.configuration(),
                existing.nextRunAt(),
                existing.lastRunAt(),
                existing.createdAt(),
                clock.instant()));
    }

    public SchedulerRun triggerJob(UUID id) {
        SchedulerJob job = getJob(id);
        if (job.status() != SchedulerJobStatus.ACTIVE) {
            throw new ConflictException("Only active scheduler jobs can be triggered.");
        }
        Instant now = clock.instant();
        return schedulerRepository.saveRun(new SchedulerRun(
                UUID.randomUUID(),
                job.id(),
                SchedulerRunStatus.QUEUED,
                "MANUAL",
                now,
                null,
                null,
                0,
                UUID.randomUUID().toString(),
                now,
                now));
    }

    public PageResult<SchedulerRun> getRuns(int page, int size) {
        return page(schedulerRepository.findAllRuns(), page, size);
    }

    private void validateCommand(SchedulerJobCommand command) {
        if (command.schedulerType() == null || command.status() == null) {
            throw new IllegalArgumentException("Scheduler type and status are required.");
        }
        try {
            ZoneId.of(command.timeZone().trim());
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException(
                    "Unsupported scheduler time zone: " + command.timeZone(), exception);
        }
        if (!CronExpression.isValidExpression(command.cronExpression().trim())) {
            throw new IllegalArgumentException(
                    "Invalid scheduler cron expression: " + command.cronExpression());
        }
    }

    private void ensureUniqueName(String name, UUID excludedId) {
        if (schedulerRepository.existsJobByName(name.toLowerCase(Locale.ROOT), excludedId)) {
            throw new ConflictException("A scheduler job with the same name already exists.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> PageResult<T> page(List<T> items, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        int fromIndex = (int) Math.min((long) page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PageResult<>(
                items.subList(fromIndex, toIndex),
                page,
                size,
                items.size(),
                totalPages);
    }
}
