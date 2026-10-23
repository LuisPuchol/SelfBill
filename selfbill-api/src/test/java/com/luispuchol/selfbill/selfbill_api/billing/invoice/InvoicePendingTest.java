package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("Pending invoices")
class InvoicePendingTest extends DomainIntegrationTest {

    private static final LocalDate END_OF_OCTOBER = LocalDate.of(2026, 10, 31);

    @Autowired
    private MockMvc mockMvc;

    private int nextCode = 100;

    @Test
    @DisplayName("Groups by client and month, ordered by client code, month and date")
    void groupsByClientMonthAndRates() {
        Integer clientB = createClient(2, DeliveryNoteFormat.VALUED);
        // Created out of order on purpose
        DeliveryNoteResponse b = noteOn(clientB, LocalDateTime.of(2026, 10, 2, 8, 0));
        DeliveryNoteResponse a15 = noteOn(clientId, LocalDateTime.of(2026, 10, 15, 8, 0));
        DeliveryNoteResponse a01 = noteOn(clientId, LocalDateTime.of(2026, 10, 1, 8, 0));
        DeliveryNoteResponse a30 = noteOn(clientId, LocalDateTime.of(2026, 9, 30, 8, 0));

        List<PendingInvoiceResponse> rows = invoiceService.getPendingInvoices(END_OF_OCTOBER);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(PendingInvoiceResponse::clientId).containsExactly(clientId, clientId, clientB);
        assertThat(rows).extracting(PendingInvoiceResponse::month).containsExactly("2026-09", "2026-10", "2026-10");
        assertThat(rows.get(0).clientCode()).isEqualTo(1);
        assertThat(rows.get(0).clientName()).isEqualTo("Client 1");
        assertThat(rows.get(2).clientCode()).isEqualTo(2);
        assertThat(rows.get(0).deliveryNotes()).extracting(PendingDeliveryNoteResponse::id)
                .containsExactly(a30.id());
        assertThat(rows.get(1).deliveryNotes()).extracting(PendingDeliveryNoteResponse::id)
                .containsExactly(a01.id(), a15.id());
        assertThat(rows.get(2).deliveryNotes()).extracting(PendingDeliveryNoteResponse::id)
                .containsExactly(b.id());
        assertThat(rows.get(1).deliveryNotes().get(0).code()).isEqualTo(a01.code());
        assertThat(rows.get(1).deliveryNotes().get(0).total()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Splits the rows of a month whose notes have different rates")
    void splitsRowsWithDifferentRates() {
        taxConfigService.createTaxConfig(taxes(LocalDate.of(2026, 10, 15), "10", "1.4"));
        noteOn(clientId, LocalDateTime.of(2026, 10, 3, 8, 0));
        noteOn(clientId, LocalDateTime.of(2026, 10, 20, 8, 0));

        List<PendingInvoiceResponse> rows = invoiceService.getPendingInvoices(END_OF_OCTOBER);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(PendingInvoiceResponse::month).containsOnly("2026-10");
        assertThat(rows.get(0).vatPercentage()).isEqualByComparingTo("4");
        assertThat(rows.get(1).vatPercentage()).isEqualByComparingTo("10");
        assertThat(rows.get(0).deliveryNotes()).hasSize(1);
        assertThat(rows.get(1).deliveryNotes()).hasSize(1);
    }

    @Test
    @DisplayName("Includes the whole limit day and nothing after it")
    void includesTheLimitDayOnly() {
        DeliveryNoteResponse in = noteOn(clientId, LocalDateTime.of(2026, 10, 31, 18, 0));
        noteOn(clientId, LocalDateTime.of(2026, 11, 1, 8, 0));

        List<PendingInvoiceResponse> rows = invoiceService.getPendingInvoices(END_OF_OCTOBER);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).deliveryNotes()).extracting(PendingDeliveryNoteResponse::id).containsExactly(in.id());
    }

    @Test
    @DisplayName("Leaves out invoiced and deleted notes, drafts included")
    void leavesOutInvoicedAndDeletedNotes() {
        DeliveryNoteResponse free = noteOn(clientId, LocalDateTime.of(2026, 10, 1, 8, 0));
        DeliveryNoteResponse inDraft = noteOn(clientId, LocalDateTime.of(2026, 10, 2, 8, 0));
        DeliveryNoteResponse inIssued = noteOn(clientId, LocalDateTime.of(2026, 10, 3, 8, 0));
        DeliveryNoteResponse deleted = noteOn(clientId, LocalDateTime.of(2026, 10, 4, 8, 0));
        createInvoice(inDraft.id());
        issue(clientId, inIssued);
        deliveryNoteService.deleteDeliveryNote(deleted.id());

        List<PendingInvoiceResponse> rows = invoiceService.getPendingInvoices(END_OF_OCTOBER);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).deliveryNotes()).extracting(PendingDeliveryNoteResponse::id).containsExactly(free.id());
    }

    @Test
    @DisplayName("Shows the same amounts as issuing the row")
    void amountsMatchIssuing() {
        Integer client = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        DeliveryNoteResponse first = noteOn(client, "10", "2.33", LocalDateTime.of(2026, 10, 1, 8, 0));
        DeliveryNoteResponse second = noteOn(client, "3", "1.07", LocalDateTime.of(2026, 10, 9, 8, 0));

        PendingInvoiceResponse row = invoiceService.getPendingInvoices(END_OF_OCTOBER).get(0);
        InvoiceResponse issued = invoiceService.createAndIssue(InvoiceRequest.builder()
                .clientId(row.clientId())
                .deliveryNoteIds(row.deliveryNotes().stream().map(PendingDeliveryNoteResponse::id).toList())
                .build());

        assertThat(row.deliveryNotes()).extracting(PendingDeliveryNoteResponse::id)
                .containsExactly(first.id(), second.id());
        assertThat(row.surchargeAmount()).isNotEqualByComparingTo("0");
        assertThat(row.subtotal()).isEqualByComparingTo(issued.subtotal());
        assertThat(row.vatAmount()).isEqualByComparingTo(issued.vatAmount());
        assertThat(row.surchargeAmount()).isEqualByComparingTo(issued.surchargeAmount());
        assertThat(row.total()).isEqualByComparingTo(issued.total());
    }

    @Test
    @DisplayName("Reports the fiscal data that blocks issuing and a deleted client")
    void reportsWhatBlocksIssuing() {
        Integer incomplete = clientService.createClient(ClientRequest.builder()
                .code(2)
                .businessName("Client 2 SL")
                .name("Client 2")
                .city("Valencia")
                .address("Calle Mayor 2")
                .vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN)
                .build()).id();
        Integer removed = createClient(3, DeliveryNoteFormat.VALUED);
        noteOn(clientId, LocalDateTime.of(2026, 10, 1, 8, 0));
        noteOn(incomplete, LocalDateTime.of(2026, 10, 1, 8, 0));
        noteOn(removed, LocalDateTime.of(2026, 10, 1, 8, 0));
        clientService.deleteClient(removed);

        List<PendingInvoiceResponse> rows = invoiceService.getPendingInvoices(END_OF_OCTOBER);

        assertThat(rows).extracting(PendingInvoiceResponse::clientId).containsExactly(clientId, incomplete, removed);
        assertThat(rows.get(0).missingFiscalData()).isEmpty();
        assertThat(rows.get(0).clientDeleted()).isFalse();
        assertThat(rows.get(1).missingFiscalData()).containsExactly("nif", "postalCode");
        assertThat(rows.get(1).clientDeleted()).isFalse();
        assertThat(rows.get(2).clientDeleted()).isTrue();
    }

    @Test
    @DisplayName("A missing limit date is a 400, not a 500")
    void missingDateToIs400() throws Exception {
        mockMvc.perform(get("/api/invoices/pending"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private DeliveryNoteResponse noteOn(Integer client, LocalDateTime date) {
        return noteOn(client, "1", "1", date);
    }

    private DeliveryNoteResponse noteOn(Integer client, String quantity, String price, LocalDateTime date) {
        return deliveryNoteService.createDeliveryNote(deliveryNoteRequest(nextCode++, client, quantity, price, date));
    }
}
