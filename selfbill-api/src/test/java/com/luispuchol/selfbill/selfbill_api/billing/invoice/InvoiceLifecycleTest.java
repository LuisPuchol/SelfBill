package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Invoice lifecycle")
class InvoiceLifecycleTest extends DomainIntegrationTest {

    @Test
    @DisplayName("An invoice is created as a draft, without number or date")
    void createsDraftWithoutNumber() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        InvoiceResponse invoice = createInvoice(deliveryNote.id());

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.code()).isNull();
        assertThat(invoice.date()).isNull();
        assertThat(invoice.total()).isEqualByComparingTo("20.80");
        assertThat(invoice.sections()).hasSize(1);
    }

    @Test
    @DisplayName("Issuing assigns correlative numbers and a discarded draft consumes none")
    void issueAssignsCorrelativeNumbers() {
        InvoiceResponse discarded = createInvoice(createDeliveryNote(clientId, "1", "1").id());
        InvoiceResponse first = createInvoice(createDeliveryNote(clientId, "1", "1").id());
        invoiceService.deleteInvoice(discarded.id());

        InvoiceResponse issued = invoiceService.issueInvoice(first.id());
        InvoiceResponse second = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").id()).id());

        assertThat(issued.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(issued.code()).isEqualTo(1);
        assertThat(issued.date()).isNotNull();
        assertThat(second.code()).isEqualTo(2);
    }

    @Test
    @DisplayName("Invoices without VAT have their own series and never consume general numbers")
    void invoicesWithoutVatHaveTheirOwnSeries() {
        Integer withoutVatClientId = clientService.createClient(
                clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL", VatType.WITHOUT_VAT)).id();

        InvoiceResponse general1 = issue(clientId, createDeliveryNote(clientId, "1", "1"));
        InvoiceResponse withoutVat1 = issue(withoutVatClientId, createDeliveryNote(withoutVatClientId, "1", "1"));
        InvoiceResponse withoutVat2 = issue(withoutVatClientId, createDeliveryNote(withoutVatClientId, "1", "1"));
        InvoiceResponse general2 = issue(clientId, createDeliveryNote(clientId, "1", "1"));

        assertThat(general1.series()).isEqualTo(InvoiceSeries.GENERAL);
        assertThat(general1.number()).isEqualTo("1");
        assertThat(withoutVat1.series()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(withoutVat1.number()).isEqualTo("S-1");
        assertThat(withoutVat2.number()).isEqualTo("S-2");
        assertThat(general2.number()).isEqualTo("2");
        assertThat(withoutVat1.vatAmount()).isEqualByComparingTo("0");

        assertThat(invoiceService.getInvoiceByCode(InvoiceSeries.GENERAL, 1).id()).isEqualTo(general1.id());
        assertThat(invoiceService.getInvoiceByCode(InvoiceSeries.WITHOUT_VAT, 1).id())
                .isEqualTo(withoutVat1.id());
        assertThat(invoiceService.exportInvoicePdf(withoutVat1.id()).filename()).endsWith("-S-1.pdf");
    }

    @Test
    @DisplayName("Deleting the last issued invoice does not make the next issue collide with its number")
    void deletedIssuedNumberIsNotReused() {
        InvoiceResponse first = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").id()).id());
        invoiceService.deleteInvoice(first.id());

        InvoiceResponse next = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").id()).id());

        assertThat(next.code()).isEqualTo(2);
    }

    @Test
    @DisplayName("Invoices issued at the same time get consecutive numbers")
    void concurrentIssuesGetConsecutiveNumbers() throws Exception {
        List<Integer> drafts = Stream.generate(() -> createInvoice(createDeliveryNote(clientId, "1", "1").id()).id())
                .limit(3)
                .toList();
        ExecutorService pool = Executors.newFixedThreadPool(drafts.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<InvoiceResponse>> issuing = drafts.stream()
                .map(id -> pool.submit(() -> {
                    start.await();
                    return invoiceService.issueInvoice(id);
                }))
                .toList();

        start.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<InvoiceResponse> issued : issuing) {
            codes.add(issued.get(10, TimeUnit.SECONDS).code());
        }
        pool.shutdown();

        assertThat(codes).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    @DisplayName("An issued invoice can be neither edited nor issued again")
    void issuedInvoiceIsFrozen() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(deliveryNote.id()).id());

        assertFails(ErrorCode.INVOICE_ALREADY_ISSUED,
                () -> invoiceService.updateInvoice(invoice.id(), invoiceRequest(deliveryNote.id())));
        assertFails(ErrorCode.INVOICE_ALREADY_ISSUED, () -> invoiceService.issueInvoice(invoice.id()));
    }

    @Test
    @DisplayName("A draft can change its delivery notes, and the removed ones become free again")
    void draftChangesItsDeliveryNotes() {
        DeliveryNoteResponse kept = createDeliveryNote(clientId, "10", "1");
        DeliveryNoteResponse removed = createDeliveryNote(clientId, "5", "1");
        InvoiceResponse draft = createInvoice(kept.id(), removed.id());

        InvoiceResponse updated = invoiceService.updateInvoice(draft.id(), invoiceRequest(kept.id()));

        assertThat(updated.sections()).hasSize(1);
        assertThat(updated.subtotal()).isEqualByComparingTo("10");
        assertFails(ErrorCode.INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED, () -> createInvoice(kept.id()));
        assertThat(createInvoice(removed.id()).status()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("Editing a delivery note of a draft recalculates the draft")
    void editingDeliveryNoteRecalculatesDraft() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "50", "2");
        InvoiceResponse draft = createInvoice(deliveryNote.id());

        deliveryNoteService.updateDeliveryNote(deliveryNote.id(),
                deliveryNoteRequest(deliveryNote.code(), clientId, "15", "2"));

        InvoiceResponse recalculated = invoiceService.getInvoiceById(draft.id());
        assertThat(recalculated.subtotal()).isEqualByComparingTo("30");
        assertThat(recalculated.sections().get(0).lines().get(0).quantity()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("A delivery note is locked once its invoice is issued")
    void deliveryNoteLockedOnceIssued() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        invoiceService.issueInvoice(createInvoice(deliveryNote.id()).id());

        assertFails(ErrorCode.DELIVERY_NOTE_ALREADY_INVOICED, () -> deliveryNoteService.updateDeliveryNote(
                deliveryNote.id(), deliveryNoteRequest(deliveryNote.code(), clientId, "2", "1")));
    }

    @Test
    @DisplayName("A delivery note in a draft cannot move to another client")
    void deliveryNoteInDraftKeepsItsClient() {
        Integer otherClientId = createClient(2, DeliveryNoteFormat.PLAIN);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        createInvoice(deliveryNote.id());

        assertFails(ErrorCode.INVOICE_MIXED_CLIENTS, () -> deliveryNoteService.updateDeliveryNote(
                deliveryNote.id(), deliveryNoteRequest(deliveryNote.code(), otherClientId, "1", "1")));
    }

    @Test
    @DisplayName("A draft cannot be exported or sent")
    void draftIsNotSendable() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "1", "1").id());

        assertFails(ErrorCode.INVOICE_NOT_ISSUED, () -> invoiceService.exportInvoicePdf(draft.id()));
        assertFails(ErrorCode.INVOICE_NOT_ISSUED, () -> invoiceService.sendInvoiceEmail(draft.id()));
    }

    @Test
    @DisplayName("Sending a document to a client without email fails before contacting the mail server")
    void sendingToClientWithoutEmailFails() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(deliveryNote.id()).id());

        assertFails(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED, () -> invoiceService.sendInvoiceEmail(invoice.id()));
        assertFails(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED,
                () -> deliveryNoteService.sendDeliveryNoteEmail(deliveryNote.id()));
    }

    @Test
    @DisplayName("The PDF of an issued invoice is named after its number")
    void pdfIsNamedAfterInvoiceNumber() {
        InvoiceResponse invoice = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").id()).id());
        jdbc.update("UPDATE invoices SET code = 1234 WHERE id = ?", invoice.id());

        assertThat(invoiceService.exportInvoicePdf(invoice.id()).filename()).endsWith("-1234.pdf");
    }

    @Test
    @DisplayName("An issued invoice keeps its sections when its delivery notes point elsewhere")
    void sectionsComeFromFrozenLines() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(deliveryNote.id()).id());

        jdbc.update("UPDATE delivery_notes SET invoice_id = NULL WHERE id = ?", deliveryNote.id());

        InvoiceResponse reloaded = invoiceService.getInvoiceById(invoice.id());
        assertThat(reloaded.sections()).hasSize(1);
        assertThat(reloaded.sections().get(0).deliveryNoteCode()).isEqualTo(deliveryNote.code());
        assertThat(reloaded.sections().get(0).lines()).hasSize(1);
    }

    @Test
    @DisplayName("Sections follow the dates of their delivery notes, oldest first, whatever order they were stored in")
    void sectionsFollowDeliveryNoteDates() {
        DeliveryNoteResponse late = createDeliveryNoteOn(LocalDateTime.of(2026, 10, 20, 8, 0));
        DeliveryNoteResponse early = createDeliveryNoteOn(LocalDateTime.of(2026, 10, 5, 8, 0));

        InvoiceResponse draft = createInvoice(late.id(), early.id());
        InvoiceResponse issued = invoiceService.issueInvoice(draft.id());
        jdbc.update("UPDATE invoice_lines SET id = id + 1000 WHERE delivery_note_code = ?", early.code());
        InvoiceResponse reloaded = invoiceService.getInvoiceById(draft.id());

        assertThat(sectionCodes(draft)).containsExactly(early.code(), late.code());
        assertThat(sectionCodes(issued)).containsExactly(early.code(), late.code());
        assertThat(sectionCodes(reloaded)).containsExactly(early.code(), late.code());
    }

    @Test
    @DisplayName("A delivery note repeated in the request is included once")
    void repeatedDeliveryNoteIdsCountOnce() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        InvoiceResponse invoice = createInvoice(deliveryNote.id(), deliveryNote.id());

        assertThat(invoice.sections()).hasSize(1);
        assertThat(invoice.subtotal()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("The not-found error names only the delivery notes that do not exist")
    void notFoundNamesOnlyMissingDeliveryNotes() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");

        assertThatThrownBy(() -> createInvoice(deliveryNote.id(), 424242))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DELIVERY_NOTE_NOT_FOUND))
                .hasMessage("Delivery note not found: 424242");
    }

    @Test
    @DisplayName("Invoice listings carry no sections; the detail does")
    void listingsCarryNoSections() {
        InvoiceResponse invoice = createInvoice(createDeliveryNote(clientId, "1", "1").id());

        assertThat(invoiceService.getAllInvoices(InvoiceFilter.builder().build(), Pageable.unpaged()).getContent())
                .singleElement().satisfies(summary -> assertThat(summary.sections()).isNull());
        assertThat(invoiceService.getInvoiceById(invoice.id()).sections()).hasSize(1);
    }

    @Test
    @DisplayName("Issuing freezes the client's fiscal data")
    void issueFreezesClientData() {
        InvoiceResponse invoice = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").id()).id());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.PLAIN, "Renamed SL"));

        String frozenName = jdbc.queryForObject("SELECT client_business_name FROM invoices WHERE id = ?",
                String.class, invoice.id());
        assertThat(frozenName).isEqualTo("Client 1 SL");
    }

    @Test
    @DisplayName("A delivery note keeps the format and tax rates it was created with")
    void deliveryNoteKeepsItsFormatAndTaxes() {
        Integer client = clientService.createClient(
                clientRequest(2, DeliveryNoteFormat.VALUED, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE)).id();
        DeliveryNoteResponse deliveryNote = createDeliveryNote(client, "1", "1");

        clientService.updateClient(client,
                clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE));
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse reloaded = deliveryNoteService.getDeliveryNoteById(deliveryNote.id());
        assertThat(reloaded.format()).isEqualTo(DeliveryNoteFormat.VALUED);
        assertThat(reloaded.vatPercentage()).isEqualByComparingTo("4");
        assertThat(reloaded.surchargePercentage()).isEqualByComparingTo("0.5");
    }

    private static List<Integer> sectionCodes(InvoiceResponse invoice) {
        return invoice.sections().stream().map(InvoiceSectionResponse::deliveryNoteCode).toList();
    }
}
