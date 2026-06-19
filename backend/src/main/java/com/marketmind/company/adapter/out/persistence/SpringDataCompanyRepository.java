package com.marketmind.company.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, UUID> {

    Page<CompanyJpaEntity> findByCompanyNameContainingIgnoreCase(String companyName, Pageable pageable);

    boolean existsByIsinIgnoreCase(String isin);

    boolean existsByIsinIgnoreCaseAndIdNot(String isin, UUID id);

    boolean existsByNseSymbolIgnoreCase(String nseSymbol);

    boolean existsByNseSymbolIgnoreCaseAndIdNot(String nseSymbol, UUID id);

    boolean existsByBseSymbolIgnoreCase(String bseSymbol);

    boolean existsByBseSymbolIgnoreCaseAndIdNot(String bseSymbol, UUID id);
}
