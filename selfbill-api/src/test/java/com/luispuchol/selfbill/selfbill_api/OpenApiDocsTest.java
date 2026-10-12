package com.luispuchol.selfbill.selfbill_api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SelfBill API"))
                .andExpect(jsonPath("$.paths['/api/delivery-notes']").exists())
                .andExpect(jsonPath("$.paths['/api/delivery-notes'].get.parameters[?(@.name == 'traceabilityCode')]")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/clients'].get.parameters[?(@.name == 'codeFrom')]").exists())
                .andExpect(jsonPath("$.paths['/api/invoices/{id}/pdf'].get.responses['200'].content['application/pdf']")
                        .exists());
    }

    @Test
    void documentsErrorResponsesByStatus() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/invoices/{id}/issue'].post.responses['409'].description",
                        containsString("INVOICE_ALREADY_ISSUED")))
                .andExpect(jsonPath("$.paths['/api/invoices/{id}/issue'].post.responses['409'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/Problem"))
                .andExpect(jsonPath("$.components.schemas.Problem.properties.errorCode").exists());
    }

    @Test
    void documentsValidationErrorsOnlyWhereInputIsValidated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/clients/{id}'].get.responses['400'].description",
                        containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.paths['/api/clients'].post.responses['400'].description",
                        containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.paths['/api/clients/next-code'].get.responses['400']").doesNotExist());
    }
}
