package com.marketmind.discovery.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.discovery.dto.DiscoveryRunRequest;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.mapper.DiscoveryMapper;
import com.marketmind.discovery.support.DiscoveryTestFixtures;
import com.marketmind.discovery.support.InMemoryDiscoveryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DiscoveryControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var service = DiscoveryTestFixtures.service(
                new InMemoryDiscoveryRepository());
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DiscoveryController(service, new DiscoveryMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldRunTestDiscoveryAndListDocuments() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DiscoveryRunRequest(
                                        DiscoverySourceType.TEST_SOURCE,
                                        null,
                                        "RELIANCE",
                                        20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalDiscovered").value(4));

        mockMvc.perform(get("/api/v1/discovery/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }
}
