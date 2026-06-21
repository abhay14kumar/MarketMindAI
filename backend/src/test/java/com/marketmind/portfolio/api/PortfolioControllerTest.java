package com.marketmind.portfolio.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.portfolio.application.PortfolioImportResult;
import com.marketmind.portfolio.application.PortfolioSummary;
import com.marketmind.portfolio.application.PortfolioService;
import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.ImportStatus;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;
import com.marketmind.portfolio.mapper.PortfolioMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PortfolioControllerTest {

    private MockMvc mockMvc;
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new StubPortfolioService();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PortfolioController(service, new PortfolioMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldAcceptXlsxMultipartImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holdings.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/portfolio/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importJob.status").value("COMPLETED"))
                .andExpect(jsonPath("$.summary.totalHoldings").value(1));
    }

    @Test
    void shouldRejectNonXlsxFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "holdings.csv", "text/csv", "symbol".getBytes());

        mockMvc.perform(multipart("/api/v1/portfolio/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.legacyCode").value("BAD_REQUEST"));
    }

    private static final class StubPortfolioService extends PortfolioService {

        private StubPortfolioService() {
            super(null, null, null, null);
        }

        @Override
        public PortfolioImportResult importHoldings(
                String originalFileName,
                java.io.InputStream input) {
            UUID portfolioId = UUID.randomUUID();
            UUID jobId = UUID.randomUUID();
            Instant now = Instant.now();
            return new PortfolioImportResult(
                    new PortfolioImportJob(
                            jobId, portfolioId, BrokerType.ZERODHA, originalFileName,
                            ImportStatus.COMPLETED, 1, 1, 0, List.of(), null,
                            now, now, now),
                    new PortfolioSnapshot(
                            UUID.randomUUID(), portfolioId, jobId,
                            BigDecimal.valueOf(1000), BigDecimal.valueOf(1200),
                            BigDecimal.valueOf(200), BigDecimal.valueOf(20),
                            1, now, now));
        }

        @Override
        public PortfolioSummary getSummary() {
            return new PortfolioSummary(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    0, null, null);
        }
    }
}
