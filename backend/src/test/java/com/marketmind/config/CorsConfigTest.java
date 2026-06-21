package com.marketmind.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class CorsConfigTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CorsConfig configuration = new CorsConfig(List.of(
                "http://localhost:5173",
                "http://localhost:5175"));
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(configuration.corsFilter())
                .build();
    }

    @Test
    void shouldAllowFrontendGetRequestFromPort5175() throws Exception {
        mockMvc.perform(get("/api/v1/test")
                        .header("Origin", "http://localhost:5175"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5175"));
    }

    @Test
    void shouldAllowOptionsPreflightFromPort5175() throws Exception {
        mockMvc.perform(options("/api/v1/test")
                        .header("Origin", "http://localhost:5175")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5175"))
                .andExpect(header().string(
                        "Access-Control-Allow-Methods",
                        Matchers.containsString("GET")));
    }

    @RestController
    @RequestMapping("/api/v1/test")
    static class TestController {

        @GetMapping
        String get() {
            return "ok";
        }
    }
}
