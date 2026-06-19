package com.marketmind.company.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.PageResult;
import com.marketmind.company.application.port.out.CompanyRepository;
import com.marketmind.company.domain.model.Company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class CompanyPersistenceAdapter implements CompanyRepository {

    private final SpringDataCompanyRepository repository;

    CompanyPersistenceAdapter(SpringDataCompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<Company> findAll(CompanyPageQuery query) {
        return toPageResult(repository.findAll(toPageRequest(query)));
    }

    @Override
    public PageResult<Company> searchByName(String name, CompanyPageQuery query) {
        return toPageResult(repository.findByCompanyNameContainingIgnoreCase(name, toPageRequest(query)));
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Company save(Company company) {
        return toDomain(repository.saveAndFlush(toEntity(company)));
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
        repository.flush();
    }

    @Override
    public boolean existsByIsin(String isin, UUID excludedId) {
        return excludedId == null
                ? repository.existsByIsinIgnoreCase(isin)
                : repository.existsByIsinIgnoreCaseAndIdNot(isin, excludedId);
    }

    @Override
    public boolean existsByNseSymbol(String nseSymbol, UUID excludedId) {
        return excludedId == null
                ? repository.existsByNseSymbolIgnoreCase(nseSymbol)
                : repository.existsByNseSymbolIgnoreCaseAndIdNot(nseSymbol, excludedId);
    }

    @Override
    public boolean existsByBseSymbol(String bseSymbol, UUID excludedId) {
        return excludedId == null
                ? repository.existsByBseSymbolIgnoreCase(bseSymbol)
                : repository.existsByBseSymbolIgnoreCaseAndIdNot(bseSymbol, excludedId);
    }

    private PageRequest toPageRequest(CompanyPageQuery query) {
        Sort.Direction direction = query.direction() == CompanyPageQuery.SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(query.page(), query.size(), Sort.by(direction, query.sortBy()));
    }

    private PageResult<Company> toPageResult(Page<CompanyJpaEntity> page) {
        return new PageResult<>(
                page.getContent().stream().map(this::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Company toDomain(CompanyJpaEntity entity) {
        return new Company(
                entity.getId(),
                entity.getIsin(),
                entity.getNseSymbol(),
                entity.getBseSymbol(),
                entity.getCompanyName(),
                entity.getSector(),
                entity.getIndustry(),
                entity.getMarketCapCategory(),
                entity.getCountry(),
                entity.getCurrency(),
                entity.getWebsite(),
                entity.getListingDate(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private CompanyJpaEntity toEntity(Company company) {
        return new CompanyJpaEntity(
                company.id(),
                company.isin(),
                company.nseSymbol(),
                company.bseSymbol(),
                company.companyName(),
                company.sector(),
                company.industry(),
                company.marketCapCategory(),
                company.country(),
                company.currency(),
                company.website(),
                company.listingDate(),
                company.active(),
                company.createdAt(),
                company.updatedAt());
    }
}
