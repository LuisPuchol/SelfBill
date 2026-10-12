package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(DeliveryNoteController.class)
@DisplayName("Delivery Note Controller Tests")
class DeliveryNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryNoteService deliveryNoteService;

    @Test
    @DisplayName("GET /api/delivery-notes - Debe pasar el filtro de facturado al servicio")
    void getAllDeliveryNotes_ShouldBindInvoicedFilter() throws Exception {
        when(deliveryNoteService.getAllDeliveryNotes(any(DeliveryNoteFilter.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/delivery-notes").param("clientId", "1").param("invoiced", "false"))
                .andExpect(status().isOk());

        ArgumentCaptor<DeliveryNoteFilter> filter = ArgumentCaptor.forClass(DeliveryNoteFilter.class);
        verify(deliveryNoteService).getAllDeliveryNotes(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().clientId()).isEqualTo(1);
        assertThat(filter.getValue().invoiced()).isFalse();
    }

    @Test
    @DisplayName("POST /api/delivery-notes - Debe aceptar un albarán sin código")
    void createDeliveryNote_WithoutCode_ShouldReturnCreated() throws Exception {
        DeliveryNoteRequest request = DeliveryNoteRequest.builder()
                .date(LocalDateTime.of(2026, 10, 1, 8, 0)).clientId(1).build();
        when(deliveryNoteService.createDeliveryNote(any(DeliveryNoteRequest.class)))
                .thenReturn(DeliveryNoteResponse.builder().id(1).code(6).build());

        mockMvc.perform(post("/api/delivery-notes").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(6));
    }

    @Test
    @DisplayName("POST /api/delivery-notes - Debe rechazar un código no positivo")
    void createDeliveryNote_WithNonPositiveCode_ShouldReturnBadRequest() throws Exception {
        DeliveryNoteRequest request = DeliveryNoteRequest.builder()
                .code(-1).date(LocalDateTime.of(2026, 10, 1, 8, 0)).clientId(1).build();

        mockMvc.perform(post("/api/delivery-notes").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(deliveryNoteService, never()).createDeliveryNote(any());
    }
}
