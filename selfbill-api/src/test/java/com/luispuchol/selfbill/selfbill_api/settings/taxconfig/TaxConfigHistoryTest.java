package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tax config history")
class TaxConfigHistoryTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM tax_config");
    }

    @Test
    @DisplayName("The history is empty when there are no configurations")
    void historyIsEmptyWithoutConfigs() throws Exception {
        mockMvc.perform(get("/api/tax-config/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("The history lists every configuration, newest first")
    void historyListsNewestFirst() throws Exception {
        create("2023-01-01", "21", "5.2");
        create("2027-01-01", "10", "1.4");
        create("2026-01-01", "4", "0.5");

        mockMvc.perform(get("/api/tax-config/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].validFrom").value("2027-01-01"))
                .andExpect(jsonPath("$[0].vatPercentage").value(10))
                .andExpect(jsonPath("$[0].surchargePercentage").value(1.4))
                .andExpect(jsonPath("$[1].validFrom").value("2026-01-01"))
                .andExpect(jsonPath("$[1].vatPercentage").value(4))
                .andExpect(jsonPath("$[1].surchargePercentage").value(0.5))
                .andExpect(jsonPath("$[2].validFrom").value("2023-01-01"))
                .andExpect(jsonPath("$[2].vatPercentage").value(21))
                .andExpect(jsonPath("$[2].surchargePercentage").value(5.2));
    }

    @Test
    @DisplayName("A VAT percentage above 100 is rejected, 100 is accepted")
    void vatAbove100IsRejected() throws Exception {
        mockMvc.perform(post("/api/tax-config").contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-01-01", "100.01", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.vatPercentage").exists());

        mockMvc.perform(post("/api/tax-config").contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-01-01", "100", "0")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A surcharge percentage above 100 is rejected, 100 is accepted")
    void surchargeAbove100IsRejected() throws Exception {
        mockMvc.perform(post("/api/tax-config").contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-01-01", "4", "100.01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.surchargePercentage").exists());

        mockMvc.perform(post("/api/tax-config").contentType(MediaType.APPLICATION_JSON)
                .content(body("2026-01-01", "4", "100")))
                .andExpect(status().isCreated());
    }

    private void create(String validFrom, String vat, String surcharge) throws Exception {
        mockMvc.perform(post("/api/tax-config").contentType(MediaType.APPLICATION_JSON)
                .content(body(validFrom, vat, surcharge)))
                .andExpect(status().isCreated());
    }

    private static String body(String validFrom, String vat, String surcharge) {
        return """
                {"validFrom": "%s", "vatPercentage": %s, "surchargePercentage": %s}""".formatted(validFrom, vat,
                surcharge);
    }
}
