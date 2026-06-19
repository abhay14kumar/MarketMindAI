package com.marketmind.company.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.PageResult;
import com.marketmind.company.domain.model.Company;

public interface CompanyRepository {

    PageResult<Company> findAll(CompanyPageQuery query);

    PageResult<Company> searchByName(String name, CompanyPageQuery query);

    Optional<Company> findById(UUID id);

    Company save(Company company);

    void deleteById(UUID id);

    boolean existsByIsin(String isin, UUID excludedId);

    boolean existsByNseSymbol(String nseSymbol, UUID excludedId);

    boolean existsByBseSymbol(String bseSymbol, UUID excludedId);
}
