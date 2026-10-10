package com.luispuchol.selfbill.selfbill_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.service.DeliveryNoteService;

@WebMvcTest(DeliveryNoteController.class)
@DisplayName("Delivery Note Controller Tests")
class DeliveryNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryNoteService deliveryNoteService;

    @Test
    @DisplayName("GET /api/deliveryNotes - Debe pasar el filtro de facturado al servicio")
    void getAllDeliveryNotes_ShouldBindInvoicedFilter() throws Exception {
        when(deliveryNoteService.getAllDeliveryNotes(any(DeliveryNoteFilter.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/deliveryNotes").param("clientId", "1").param("invoiced", "false"))
                .andExpect(status().isOk());

        ArgumentCaptor<DeliveryNoteFilter> filter = ArgumentCaptor.forClass(DeliveryNoteFilter.class);
        verify(deliveryNoteService).getAllDeliveryNotes(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().getClientId()).isEqualTo(1);
        assertThat(filter.getValue().getInvoiced()).isFalse();
    }
}
