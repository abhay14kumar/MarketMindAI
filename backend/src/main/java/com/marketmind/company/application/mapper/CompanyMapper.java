package com.marketmind.company.application.mapper;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.company.application.dto.CompanyDTO;
import com.marketmind.company.domain.model.Company;

import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyDTO toDTO(Company company) {
        return new CompanyDTO(
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

    public Company toNewCompany(CompanyDTO source, UUID id, Instant now) {
        return new Company(
                id,
                normalizeUpper(source.isin()),
                normalizeUpper(source.nseSymbol()),
                normalizeUpper(source.bseSymbol()),
                normalizeRequired(source.companyName()),
                normalizeOptional(source.sector()),
                normalizeOptional(source.industry()),
                source.marketCapCategory(),
                normalizeUpper(source.country()),
                normalizeUpper(source.currency()),
                normalizeOptional(source.website()),
                source.listingDate(),
                source.active(),
                now,
                now);
    }

    public Company toUpdatedCompany(Company existing, CompanyDTO source, Instant now) {
        return new Company(
                existing.id(),
                normalizeUpper(source.isin()),
                normalizeUpper(source.nseSymbol()),
                normalizeUpper(source.bseSymbol()),
                normalizeRequired(source.companyName()),
                normalizeOptional(source.sector()),
                normalizeOptional(source.industry()),
                source.marketCapCategory(),
                normalizeUpper(source.country()),
                normalizeUpper(source.currency()),
                normalizeOptional(source.website()),
                source.listingDate(),
                source.active(),
                existing.createdAt(),
                now);
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
