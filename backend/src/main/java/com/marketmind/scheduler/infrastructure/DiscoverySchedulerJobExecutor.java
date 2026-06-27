package com.marketmind.scheduler.infrastructure;

import java.net.URI;

import com.marketmind.discovery.application.DiscoveryRunCommand;
import com.marketmind.discovery.application.DiscoveryService;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.scheduler.application.SchedulerJobExecutor;
import com.marketmind.scheduler.domain.SchedulerJob;
import com.marketmind.scheduler.domain.SchedulerRunStatus;
import com.marketmind.scheduler.domain.SchedulerType;

import org.springframework.stereotype.Component;

@Component
public class DiscoverySchedulerJobExecutor implements SchedulerJobExecutor {

    private static final String NSE_URL =
            "https://www.nseindia.com/companies-listing/corporate-filings-announcements";

    private final DiscoveryService discoveryService;

    public DiscoverySchedulerJobExecutor(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public ExecutionResult execute(SchedulerJob job) {
        DiscoverySourceType sourceType = sourceType(job.schedulerType());
        if (sourceType == null) {
            return new ExecutionResult(
                    SchedulerRunStatus.FAILED,
                    "No executable implementation is wired for this scheduler type.",
                    "Scheduler job implementation is not available.",
                    0,
                    0);
        }
        URI sourceUrl = URI.create(job.configuration().getOrDefault(
                "sourceUrl",
                job.schedulerType() == SchedulerType.NSE_FILINGS ? NSE_URL : ""));
        if (sourceUrl.toString().isBlank()) {
            return new ExecutionResult(
                    SchedulerRunStatus.FAILED,
                    "A discovery source URL is required before this job can run.",
                    "Missing scheduler configuration: sourceUrl.",
                    0,
                    0);
        }
        var details = discoveryService.run(new DiscoveryRunCommand(
                sourceType,
                sourceUrl,
                job.configuration().get("companySymbol"),
                20));
        var discovery = details.job();
        String summary = discovery.message();
        if (job.schedulerType() == SchedulerType.NSE_FILINGS
                && discovery.totalDiscovered() == 0) {
            summary = "Generic HTML crawler could not discover documents from NSE because "
                    + "the page may be dynamic or protected. NSE-specific crawler is planned.";
        }
        return new ExecutionResult(
                discovery.status().name().equals("FAILED")
                        ? SchedulerRunStatus.FAILED
                        : SchedulerRunStatus.COMPLETED,
                summary,
                discovery.errorMessage(),
                discovery.totalDiscovered(),
                discovery.newDocuments());
    }

    private DiscoverySourceType sourceType(SchedulerType type) {
        return switch (type) {
            case NSE_FILINGS -> DiscoverySourceType.NSE;
            case BSE_FILINGS -> DiscoverySourceType.BSE;
            case SEBI_FILINGS -> DiscoverySourceType.SEBI;
            case COMPANY_FILINGS -> DiscoverySourceType.COMPANY_IR;
            case CUSTOM -> null;
        };
    }
}
