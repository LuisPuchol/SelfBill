package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.hamcrest.Matchers.greaterThan;
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
@DisplayName("Client summary")
class InvoiceSummaryTest extends DomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private InvoiceResponse issuedOn(Integer client, String quantity, String price, LocalDateTime date) {
        InvoiceResponse invoice = issue(client, createDeliveryNote(client, quantity, price));
        // SQLite keeps dates as epoch millis, which a Timestamp binds to
        jdbc.update("UPDATE invoices SET date = ? WHERE id = ?", Timestamp.valueOf(date), invoice.id());
        return invoice;
    }

    private ResultActions summary() throws Exception {
        return mockMvc.perform(get("/api/invoices/summary")
                .param("dateFrom", "2026-04-01").param("dateTo", "2026-04-30"));
    }

    @Test
    @DisplayName("Sums the issued invoices of each client, by client code")
    void sumsIssuedInvoicesPerClient() throws Exception {
        Integer second = createClient(2, DeliveryNoteFormat.VALUED);
        issuedOn(second, "10", "1", LocalDateTime.of(2026, 4, 3, 9, 0));
        issuedOn(second, "20", "1", LocalDateTime.of(2026, 4, 30, 18, 0));
        issuedOn(clientId, "5", "1", LocalDateTime.of(2026, 4, 10, 9, 0));

        summary()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientId").value(clientId))
                .andExpect(jsonPath("$[0].clientCode").value(1))
                .andExpect(jsonPath("$[0].clientName").value("Client 1"))
                .andExpect(jsonPath("$[0].invoiceCount").value(1))
                .andExpect(jsonPath("$[0].subtotal").value(5.0))
                .andExpect(jsonPath("$[0].vatAmount").value(0.2))
                .andExpect(jsonPath("$[0].total").value(5.2))
                .andExpect(jsonPath("$[1].clientId").value(second))
                .andExpect(jsonPath("$[1].clientCode").value(2))
                .andExpect(jsonPath("$[1].clientName").value("Client 2"))
                .andExpect(jsonPath("$[1].invoiceCount").value(2))
                .andExpect(jsonPath("$[1].subtotal").value(30.0))
                .andExpect(jsonPath("$[1].vatAmount").value(1.2))
                .andExpect(jsonPath("$[1].surchargeAmount").value(0.0))
                .andExpect(jsonPath("$[1].total").value(31.2));
    }

    @Test
    @DisplayName("Leaves out drafts, deleted invoices and other dates")
    void leavesOutDraftsDeletedAndOtherDates() throws Exception {
        createInvoiceFor(clientId, createDeliveryNote(clientId, "1", "1").id());
        Integer deletedClient = createClient(2, DeliveryNoteFormat.VALUED);
        InvoiceResponse deleted = issuedOn(deletedClient, "1", "1", LocalDateTime.of(2026, 4, 10, 9, 0));
        invoiceService.deleteInvoice(deleted.id());
        Integer before = createClient(3, DeliveryNoteFormat.VALUED);
        issuedOn(before, "1", "1", LocalDateTime.of(2026, 3, 31, 23, 59));
        Integer after = createClient(4, DeliveryNoteFormat.VALUED);
        issuedOn(after, "1", "1", LocalDateTime.of(2026, 5, 1, 0, 0));

        summary().andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("A deleted client is marked, after the live one with the same code")
    void deletedClientIsMarked() throws Exception {
        Integer gone = createClient(2, DeliveryNoteFormat.VALUED);
        issuedOn(gone, "1", "1", LocalDateTime.of(2026, 4, 10, 9, 0));
        clientService.deleteClient(gone);
        Integer live = createClient(2, DeliveryNoteFormat.VALUED);
        issuedOn(live, "1", "1", LocalDateTime.of(2026, 4, 11, 9, 0));

        summary()
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientId").value(live))
                .andExpect(jsonPath("$[0].clientDeleted").value(false))
                .andExpect(jsonPath("$[1].clientId").value(gone))
                .andExpect(jsonPath("$[1].clientDeleted").value(true));
    }

    @Test
    @DisplayName("Counts both series")
    void bothSeries() throws Exception {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        issuedOn(withoutVat, "10", "1", LocalDateTime.of(2026, 4, 10, 9, 0));
        issuedOn(clientId, "10", "1", LocalDateTime.of(2026, 4, 10, 9, 0));

        summary()
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].clientCode").value(2))
                .andExpect(jsonPath("$[1].invoiceCount").value(1))
                .andExpect(jsonPath("$[1].vatAmount").value(0.0))
                .andExpect(jsonPath("$[1].total").value(10.0));
    }

    @Test
    @DisplayName("Sums the surcharge")
    void sumsSurcharge() throws Exception {
        Integer surcharged = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        issuedOn(surcharged, "10", "1", LocalDateTime.of(2026, 4, 10, 9, 0));

        summary()
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].surchargeAmount").value(greaterThan(0.0)));
    }

    @Test
    @DisplayName("A deleted client still shows")
    void deletedClientStillShows() throws Exception {
        Integer gone = createClient(2, DeliveryNoteFormat.VALUED);
        issuedOn(gone, "10", "1", LocalDateTime.of(2026, 4, 10, 9, 0));
        clientService.deleteClient(gone);

        summary()
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientId").value(gone))
                .andExpect(jsonPath("$[0].invoiceCount").value(1));
    }

    @Test
    @DisplayName("Requires both dates")
    void requiresBothDates() throws Exception {
        mockMvc.perform(get("/api/invoices/summary").param("dateTo", "2026-04-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/invoices/summary").param("dateFrom", "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
