package com.marketmind.common.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CorrelationIdFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void shouldReuseSafeCallerCorrelationId() throws Exception {
        String correlationId = "portfolio-import-42";

        mockMvc.perform(get("/probe")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER,
                                correlationId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        CorrelationIdFilter.CORRELATION_ID_HEADER,
                        correlationId))
                .andExpect(header().string(
                        CorrelationIdFilter.REQUEST_ID_HEADER,
                        correlationId));

        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdForUnsafeCallerValue() throws Exception {
        mockMvc.perform(get("/probe")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER,
                                "unsafe value\n"))
                .andExpect(status().isOk())
                .andExpect(header().exists(
                        CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(header().exists(
                        CorrelationIdFilter.REQUEST_ID_HEADER));
    }

    @RestController
    private static final class ProbeController {

        @GetMapping("/probe")
        String probe() {
            return "ok";
        }
    }
}
