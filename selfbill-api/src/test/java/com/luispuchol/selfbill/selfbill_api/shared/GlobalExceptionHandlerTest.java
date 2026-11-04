package com.luispuchol.selfbill.selfbill_api.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("Global exception handler")
class GlobalExceptionHandlerTest extends DomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GlobalExceptionHandler handler;
    @Autowired
    private ClientRepository clientRepository;

    @Test
    void unknownSortIsAValidationError() throws Exception {
        for (String path : new String[] { "/api/clients", "/api/invoices" }) {
            mockMvc.perform(get(path).param("sort", "nope"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.sort").exists());
        }
    }

    @Test
    void uniqueClashIsAConflict() {
        Client clash = Client.builder().code(1).businessName("Other SL").name("Other").build();

        assertThatThrownBy(() -> clientRepository.saveAndFlush(clash))
                .isInstanceOfSatisfying(DataAccessException.class, ex -> {
                    ProblemDetail problem = handler.handleDataAccess(ex, new MockHttpServletRequest());
                    assertThat(problem.getStatus()).isEqualTo(409);
                    assertThat(problem.getProperties()).containsEntry("errorCode", "DATA_CONFLICT");
                });
    }

    @Test
    void otherIntegrityErrorsStayInternal() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into delivery_notes (code, date, format, vat_percentage, surcharge_percentage, client_id,
                    created_at, updated_at)
                values (99, current_timestamp, 'PLAIN', 4, 0, 987654, current_timestamp, current_timestamp)"""))
                .isInstanceOfSatisfying(DataAccessException.class, ex -> {
                    ProblemDetail problem = handler.handleDataAccess(ex, new MockHttpServletRequest());
                    assertThat(problem.getStatus()).isEqualTo(500);
                    assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_ERROR");
                });
    }
}
