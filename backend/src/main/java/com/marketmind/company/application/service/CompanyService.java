package com.marketmind.company.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.company.application.dto.CompanyDTO;
import com.marketmind.company.application.dto.CompanyPageDTO;
import com.marketmind.company.application.mapper.CompanyMapper;
import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.PageResult;
import com.marketmind.company.application.port.out.CompanyRepository;
import com.marketmind.company.domain.model.Company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "isin",
            "nseSymbol",
            "bseSymbol",
            "companyName",
            "sector",
            "industry",
            "marketCapCategory",
            "country",
            "currency",
            "website",
            "listingDate",
            "active",
            "createdAt",
            "updatedAt");

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final Clock clock;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanyMapper companyMapper,
            Clock clock) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.clock = clock;
    }

    public CompanyPageDTO findAll(CompanyPageQuery query) {
        CompanyPageQuery validatedQuery = validateQuery(query);
        return toPageDTO(companyRepository.findAll(validatedQuery), validatedQuery);
    }

    public CompanyDTO findById(UUID id) {
        return companyMapper.toDTO(getCompany(id));
    }

    public CompanyPageDTO searchByName(String name, CompanyPageQuery query) {
        CompanyPageQuery validatedQuery = validateQuery(query);
        return toPageDTO(companyRepository.searchByName(name.trim(), validatedQuery), validatedQuery);
    }

    @Transactional
    public CompanyDTO create(CompanyDTO request) {
        ensureUnique(request, null);
        Instant now = clock.instant();
        Company company = companyMapper.toNewCompany(request, UUID.randomUUID(), now);
        return companyMapper.toDTO(companyRepository.save(company));
    }

    @Transactional
    public CompanyDTO update(UUID id, CompanyDTO request) {
        Company existing = getCompany(id);
        ensureUnique(request, id);
        Company company = companyMapper.toUpdatedCompany(existing, request, clock.instant());
        return companyMapper.toDTO(companyRepository.save(company));
    }

    @Transactional
    public void delete(UUID id) {
        getCompany(id);
        companyRepository.deleteById(id);
    }

    private Company getCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    private void ensureUnique(CompanyDTO request, UUID excludedId) {
        if (companyRepository.existsByIsin(request.isin(), excludedId)) {
            throw new ConflictException("A company with the same ISIN already exists.");
        }
        if (hasText(request.nseSymbol())
                && companyRepository.existsByNseSymbol(request.nseSymbol(), excludedId)) {
            throw new ConflictException("A company with the same NSE symbol already exists.");
        }
        if (hasText(request.bseSymbol())
                && companyRepository.existsByBseSymbol(request.bseSymbol(), excludedId)) {
            throw new ConflictException("A company with the same BSE symbol already exists.");
        }
    }

    private CompanyPageQuery validateQuery(CompanyPageQuery query) {
        if (query.page() < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        if (!ALLOWED_SORT_FIELDS.contains(query.sortBy())) {
            throw new IllegalArgumentException("Unsupported company sort field: " + query.sortBy());
        }
        return query;
    }

    private CompanyPageDTO toPageDTO(
            PageResult<Company> page,
            CompanyPageQuery query) {
        return new CompanyPageDTO(
                page.content().stream().map(companyMapper::toDTO).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                query.sortBy(),
                query.direction());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
