package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("Invoice totals")
class InvoiceTotalsTest extends DomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private InvoiceResponse issuedOn(Integer client, String quantity, String price, LocalDateTime date) {
        InvoiceResponse invoice = issue(client, createDeliveryNote(client, quantity, price));
        jdbc.update("UPDATE invoices SET date = ? WHERE id = ?", Timestamp.valueOf(date), invoice.id());
        return invoice;
    }

    private ResultActions totals(String... params) throws Exception {
        var request = get("/api/invoices/totals");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    @Test
    @DisplayName("Sums every invoice of the period, and only those")
    void totalsCoverTheWholePeriod() throws Exception {
        Integer second = createClient(2, DeliveryNoteFormat.VALUED);
        issuedOn(clientId, "10", "1.11", LocalDateTime.of(2026, 4, 3, 9, 0));
        issuedOn(second, "20", "2.22", LocalDateTime.of(2026, 4, 15, 9, 0));
        issuedOn(second, "5", "0.33", LocalDateTime.of(2026, 4, 30, 18, 0));
        issuedOn(clientId, "1", "1", LocalDateTime.of(2026, 5, 1, 0, 0));
        createInvoiceFor(clientId, createDeliveryNote(clientId, "1", "1").id());
        InvoiceResponse deleted = issuedOn(clientId, "1", "1", LocalDateTime.of(2026, 4, 10, 9, 0));
        invoiceService.deleteInvoice(deleted.id());

        // 11.10 + 44.40 + 1.65 = 57.15; VAT 4% each: 0.44 + 1.78 + 0.07
        totals("status", "ISSUED", "dateFrom", "2026-04-01", "dateTo", "2026-04-30")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.subtotal").value(57.15))
                .andExpect(jsonPath("$.vatAmount").value(2.29))
                .andExpect(jsonPath("$.surchargeAmount").value(0.0))
                .andExpect(jsonPath("$.total").value(59.44));
    }

    @Test
    @DisplayName("Zeros when nothing matches")
    void zerosWhenNothingMatches() throws Exception {
        totals("status", "ISSUED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.subtotal").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("Filters as the list does")
    void filtersAsTheList() throws Exception {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        issuedOn(clientId, "10", "1", LocalDateTime.of(2026, 4, 3, 9, 0));
        issuedOn(withoutVat, "20", "1", LocalDateTime.of(2026, 4, 4, 9, 0));
        createInvoiceFor(clientId, createDeliveryNote(clientId, "7", "1").id());

        totals("status", "DRAFT").andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.subtotal").value(7.0));
        totals("series", "WITHOUT_VAT").andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.subtotal").value(20.0));
        totals("clientId", clientId.toString(), "status", "ISSUED").andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.subtotal").value(10.0));
        totals("status", "ISSUED", "dateFrom", "2026-04-04", "dateTo", "2026-04-04")
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.subtotal").value(20.0));
        totals().andExpect(jsonPath("$.count").value(3));
    }
}
