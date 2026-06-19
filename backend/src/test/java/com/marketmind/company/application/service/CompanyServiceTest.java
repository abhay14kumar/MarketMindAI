package com.marketmind.company.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.common.exception.ConflictException;
import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.company.application.dto.CompanyDTO;
import com.marketmind.company.application.mapper.CompanyMapper;
import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.CompanyPageQuery.SortDirection;
import com.marketmind.company.application.model.PageResult;
import com.marketmind.company.application.port.out.CompanyRepository;
import com.marketmind.company.domain.model.Company;
import com.marketmind.company.domain.model.MarketCapCategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID COMPANY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InMemoryCompanyRepository companyRepository;
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyRepository = new InMemoryCompanyRepository();
        CompanyMapper mapper = new CompanyMapper();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        companyService = new CompanyService(companyRepository, mapper, clock);
    }

    @Test
    void shouldReturnPaginatedCompanies() {
        companyRepository.save(company());
        CompanyPageQuery query = new CompanyPageQuery(0, 20, "companyName", SortDirection.ASC);

        var result = companyService.findAll(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().companyName()).isEqualTo("MarketMind Industries");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldCreateCompanyWithApplicationManagedIdentityAndTimestamps() {
        CompanyDTO created = companyService.create(request());

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isEqualTo(NOW);
        assertThat(created.isin()).isEqualTo("INE000A01001");
    }

    @Test
    void shouldRejectDuplicateIsin() {
        companyRepository.save(company());

        assertThatThrownBy(() -> companyService.create(request()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ISIN");

        assertThat(companyRepository.companies).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundWhenCompanyDoesNotExist() {
        assertThatThrownBy(() -> companyService.findById(COMPANY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(COMPANY_ID.toString());
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        CompanyPageQuery query = new CompanyPageQuery(0, 20, "unsupported", SortDirection.ASC);

        assertThatThrownBy(() -> companyService.findAll(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void shouldDeleteExistingCompany() {
        companyRepository.save(company());

        companyService.delete(COMPANY_ID);

        assertThat(companyRepository.findById(COMPANY_ID)).isEmpty();
    }

    private Company company() {
        return new Company(
                COMPANY_ID,
                "INE000A01001",
                "MARKETMIND",
                null,
                "MarketMind Industries",
                "Financial Services",
                "Investment Technology",
                MarketCapCategory.MID_CAP,
                "IN",
                "INR",
                "https://marketmind.local",
                LocalDate.of(2020, 1, 1),
                true,
                NOW.minusSeconds(3600),
                NOW);
    }

    private CompanyDTO request() {
        return new CompanyDTO(
                null,
                "INE000A01001",
                "MARKETMIND",
                null,
                "MarketMind Industries",
                "Financial Services",
                "Investment Technology",
                MarketCapCategory.MID_CAP,
                "IN",
                "INR",
                "https://marketmind.local",
                LocalDate.of(2020, 1, 1),
                true,
                null,
                null);
    }

    private static final class InMemoryCompanyRepository implements CompanyRepository {

        private final Map<UUID, Company> companies = new LinkedHashMap<>();

        @Override
        public PageResult<Company> findAll(CompanyPageQuery query) {
            List<Company> content = companies.values().stream()
                    .skip((long) query.page() * query.size())
                    .limit(query.size())
                    .toList();
            return page(content, query);
        }

        @Override
        public PageResult<Company> searchByName(String name, CompanyPageQuery query) {
            List<Company> content = companies.values().stream()
                    .filter(company -> company.companyName().toLowerCase().contains(name.toLowerCase()))
                    .skip((long) query.page() * query.size())
                    .limit(query.size())
                    .toList();
            return page(content, query);
        }

        @Override
        public Optional<Company> findById(UUID id) {
            return Optional.ofNullable(companies.get(id));
        }

        @Override
        public Company save(Company company) {
            companies.put(company.id(), company);
            return company;
        }

        @Override
        public void deleteById(UUID id) {
            companies.remove(id);
        }

        @Override
        public boolean existsByIsin(String isin, UUID excludedId) {
            return companies.values().stream()
                    .anyMatch(company -> !company.id().equals(excludedId)
                            && company.isin().equalsIgnoreCase(isin));
        }

        @Override
        public boolean existsByNseSymbol(String nseSymbol, UUID excludedId) {
            return companies.values().stream()
                    .anyMatch(company -> !company.id().equals(excludedId)
                            && company.nseSymbol() != null
                            && company.nseSymbol().equalsIgnoreCase(nseSymbol));
        }

        @Override
        public boolean existsByBseSymbol(String bseSymbol, UUID excludedId) {
            return companies.values().stream()
                    .anyMatch(company -> !company.id().equals(excludedId)
                            && company.bseSymbol() != null
                            && company.bseSymbol().equalsIgnoreCase(bseSymbol));
        }

        private PageResult<Company> page(List<Company> content, CompanyPageQuery query) {
            int totalPages = companies.isEmpty()
                    ? 0
                    : (int) Math.ceil((double) companies.size() / query.size());
            return new PageResult<>(
                    content,
                    query.page(),
                    query.size(),
                    companies.size(),
                    totalPages);
        }
    }
}
