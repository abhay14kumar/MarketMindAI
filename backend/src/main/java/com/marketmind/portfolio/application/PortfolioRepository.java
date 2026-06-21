package com.marketmind.portfolio.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.Portfolio;
import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;

public interface PortfolioRepository {

    Portfolio getOrCreatePortfolio(BrokerType brokerType, String name);

    PortfolioImportJob saveImportJob(PortfolioImportJob job);

    void replaceHoldings(UUID portfolioId, List<PortfolioHolding> holdings);

    PortfolioSnapshot saveSnapshot(PortfolioSnapshot snapshot);

    Optional<PortfolioSnapshot> findLatestSnapshot(UUID portfolioId);

    PageResult<PortfolioHolding> findHoldings(UUID portfolioId, int page, int size);

    PageResult<PortfolioImportJob> findImportJobs(UUID portfolioId, int page, int size);
}
