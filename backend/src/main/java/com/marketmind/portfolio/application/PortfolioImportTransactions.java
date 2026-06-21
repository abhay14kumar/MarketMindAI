package com.marketmind.portfolio.application;

import java.util.List;
import java.util.UUID;

import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PortfolioImportTransactions {

    private final PortfolioRepository repository;

    public PortfolioImportTransactions(PortfolioRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(PortfolioImportJob job) {
        repository.saveImportJob(job);
    }

    @Transactional
    public void complete(
            UUID portfolioId,
            List<PortfolioHolding> holdings,
            PortfolioSnapshot snapshot,
            PortfolioImportJob completedJob) {
        repository.replaceHoldings(portfolioId, holdings);
        repository.saveSnapshot(snapshot);
        repository.saveImportJob(completedJob);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(PortfolioImportJob failedJob) {
        repository.saveImportJob(failedJob);
    }
}
