package com.marketmind.company.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.company.application.dto.CompanyDTO;
import com.marketmind.company.application.dto.CompanyPageDTO;
import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.CompanyPageQuery.SortDirection;
import com.marketmind.company.application.service.CompanyService;
import com.marketmind.company.domain.model.MarketCapCategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanyControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private StubCompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new StubCompanyService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CompanyController(companyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListCompaniesWithPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/companies")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "companyName")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        assertThat(companyService.lastQuery)
                .isEqualTo(new CompanyPageQuery(0, 20, "companyName", SortDirection.ASC));
    }

    @Test
    void shouldCreateCompany() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.companyName").value("MarketMind Industries"));
    }

    @Test
    void shouldRejectInvalidCompanyRequest() throws Exception {
        CompanyDTO invalid = new CompanyDTO(
                null,
                "",
                null,
                null,
                "",
                null,
                null,
                null,
                "India",
                "Rupees",
                "not-a-url",
                LocalDate.now().plusDays(1),
                null,
                null,
                null);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.legacyCode").value("VALIDATION_ERROR"));
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

    private CompanyDTO response() {
        return new CompanyDTO(
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
                Instant.parse("2026-06-19T12:00:00Z"),
                Instant.parse("2026-06-19T12:00:00Z"));
    }

    private final class StubCompanyService extends CompanyService {

        private CompanyPageQuery lastQuery;

        private StubCompanyService() {
            super(null, null, Clock.systemUTC());
        }

        @Override
        public CompanyPageDTO findAll(CompanyPageQuery query) {
            lastQuery = query;
            return new CompanyPageDTO(
                    List.of(response()),
                    query.page(),
                    query.size(),
                    1,
                    1,
                    query.sortBy(),
                    query.direction());
        }

        @Override
        public CompanyDTO create(CompanyDTO request) {
            return response();
        }
    }
}
