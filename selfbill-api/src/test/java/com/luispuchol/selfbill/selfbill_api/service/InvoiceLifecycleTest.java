package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceSeries;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Invoice lifecycle")
class InvoiceLifecycleTest extends DomainIntegrationTest {

    @Test
    @DisplayName("An invoice is created as a draft, without number or date")
    void createsDraftWithoutNumber() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        InvoiceResponse invoice = createInvoice(deliveryNote.getId());

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getCode()).isNull();
        assertThat(invoice.getDate()).isNull();
        assertThat(invoice.getTotal()).isEqualByComparingTo("20.80");
        assertThat(invoice.getSections()).hasSize(1);
    }

    @Test
    @DisplayName("Issuing assigns correlative numbers and a discarded draft consumes none")
    void issueAssignsCorrelativeNumbers() {
        InvoiceResponse discarded = createInvoice(createDeliveryNote(clientId, "1", "1").getId());
        InvoiceResponse first = createInvoice(createDeliveryNote(clientId, "1", "1").getId());
        invoiceService.deleteInvoice(discarded.getId());

        InvoiceResponse issued = invoiceService.issueInvoice(first.getId());
        InvoiceResponse second = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").getId()).getId());

        assertThat(issued.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(issued.getCode()).isEqualTo(1);
        assertThat(issued.getDate()).isNotNull();
        assertThat(second.getCode()).isEqualTo(2);
    }

    @Test
    @DisplayName("Invoices without VAT have their own series and never consume general numbers")
    void invoicesWithoutVatHaveTheirOwnSeries() {
        Integer withoutVatClientId = clientService.createClient(
                clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL", VatType.WITHOUT_VAT)).getId();

        InvoiceResponse general1 = issue(clientId, createDeliveryNote(clientId, "1", "1"));
        InvoiceResponse withoutVat1 = issue(withoutVatClientId, createDeliveryNote(withoutVatClientId, "1", "1"));
        InvoiceResponse withoutVat2 = issue(withoutVatClientId, createDeliveryNote(withoutVatClientId, "1", "1"));
        InvoiceResponse general2 = issue(clientId, createDeliveryNote(clientId, "1", "1"));

        assertThat(general1.getSeries()).isEqualTo(InvoiceSeries.GENERAL);
        assertThat(general1.getNumber()).isEqualTo("1");
        assertThat(withoutVat1.getSeries()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(withoutVat1.getNumber()).isEqualTo("S-1");
        assertThat(withoutVat2.getNumber()).isEqualTo("S-2");
        assertThat(general2.getNumber()).isEqualTo("2");
        assertThat(withoutVat1.getVatAmount()).isEqualByComparingTo("0");

        assertThat(invoiceService.getInvoiceByCode(InvoiceSeries.GENERAL, 1).getId()).isEqualTo(general1.getId());
        assertThat(invoiceService.getInvoiceByCode(InvoiceSeries.WITHOUT_VAT, 1).getId())
                .isEqualTo(withoutVat1.getId());
        assertThat(invoiceService.exportInvoicePdf(withoutVat1.getId()).filename()).endsWith("-S-1.pdf");
    }

    @Test
    @DisplayName("Deleting the last issued invoice does not make the next issue collide with its number")
    void deletedIssuedNumberIsNotReused() {
        InvoiceResponse first = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").getId()).getId());
        invoiceService.deleteInvoice(first.getId());

        InvoiceResponse next = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").getId()).getId());

        assertThat(next.getCode()).isEqualTo(2);
    }

    @Test
    @DisplayName("An issued invoice can be neither edited nor issued again")
    void issuedInvoiceIsFrozen() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(deliveryNote.getId()).getId());

        assertFails(ErrorCode.INVOICE_ALREADY_ISSUED,
                () -> invoiceService.updateInvoice(invoice.getId(), invoiceRequest(deliveryNote.getId())));
        assertFails(ErrorCode.INVOICE_ALREADY_ISSUED, () -> invoiceService.issueInvoice(invoice.getId()));
    }

    @Test
    @DisplayName("A draft can change its delivery notes, and the removed ones become free again")
    void draftChangesItsDeliveryNotes() {
        DeliveryNoteResponse kept = createDeliveryNote(clientId, "10", "1");
        DeliveryNoteResponse removed = createDeliveryNote(clientId, "5", "1");
        InvoiceResponse draft = createInvoice(kept.getId(), removed.getId());

        InvoiceResponse updated = invoiceService.updateInvoice(draft.getId(), invoiceRequest(kept.getId()));

        assertThat(updated.getSections()).hasSize(1);
        assertThat(updated.getSubtotal()).isEqualByComparingTo("10");
        assertFails(ErrorCode.INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED, () -> createInvoice(kept.getId()));
        assertThat(createInvoice(removed.getId()).getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("Editing a delivery note of a draft recalculates the draft")
    void editingDeliveryNoteRecalculatesDraft() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "50", "2");
        InvoiceResponse draft = createInvoice(deliveryNote.getId());

        deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), clientId, "15", "2"));

        InvoiceResponse recalculated = invoiceService.getInvoiceById(draft.getId());
        assertThat(recalculated.getSubtotal()).isEqualByComparingTo("30");
        assertThat(recalculated.getSections().get(0).getLines().get(0).getQuantity()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("A delivery note is locked once its invoice is issued")
    void deliveryNoteLockedOnceIssued() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        invoiceService.issueInvoice(createInvoice(deliveryNote.getId()).getId());

        assertFails(ErrorCode.DELIVERY_NOTE_ALREADY_INVOICED, () -> deliveryNoteService.updateDeliveryNote(
                deliveryNote.getId(), deliveryNoteRequest(deliveryNote.getCode(), clientId, "2", "1")));
    }

    @Test
    @DisplayName("A delivery note in a draft cannot move to another client")
    void deliveryNoteInDraftKeepsItsClient() {
        Integer otherClientId = createClient(2, DeliveryNoteFormat.PLAIN);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        createInvoice(deliveryNote.getId());

        assertFails(ErrorCode.INVOICE_MIXED_CLIENTS, () -> deliveryNoteService.updateDeliveryNote(
                deliveryNote.getId(), deliveryNoteRequest(deliveryNote.getCode(), otherClientId, "1", "1")));
    }

    @Test
    @DisplayName("A draft cannot be exported or sent")
    void draftIsNotSendable() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "1", "1").getId());

        assertFails(ErrorCode.INVOICE_NOT_ISSUED, () -> invoiceService.exportInvoicePdf(draft.getId()));
        assertFails(ErrorCode.INVOICE_NOT_ISSUED, () -> invoiceService.sendInvoiceEmail(draft.getId()));
    }

    @Test
    @DisplayName("The PDF of an issued invoice is named after its number")
    void pdfIsNamedAfterInvoiceNumber() {
        InvoiceResponse invoice = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").getId()).getId());
        jdbc.update("UPDATE invoices SET code = 1234 WHERE id = ?", invoice.getId());

        assertThat(invoiceService.exportInvoicePdf(invoice.getId()).filename()).endsWith("-1234.pdf");
    }

    @Test
    @DisplayName("An issued invoice keeps its sections when its delivery notes point elsewhere")
    void sectionsComeFromFrozenLines() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(deliveryNote.getId()).getId());

        jdbc.update("UPDATE delivery_notes SET invoice_id = NULL WHERE id = ?", deliveryNote.getId());

        InvoiceResponse reloaded = invoiceService.getInvoiceById(invoice.getId());
        assertThat(reloaded.getSections()).hasSize(1);
        assertThat(reloaded.getSections().get(0).getDeliveryNoteCode()).isEqualTo(deliveryNote.getCode());
        assertThat(reloaded.getSections().get(0).getLines()).hasSize(1);
    }

    @Test
    @DisplayName("Issuing freezes the client's fiscal data")
    void issueFreezesClientData() {
        InvoiceResponse invoice = invoiceService.issueInvoice(
                createInvoice(createDeliveryNote(clientId, "1", "1").getId()).getId());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.PLAIN, "Renamed SL"));

        String frozenName = jdbc.queryForObject("SELECT client_business_name FROM invoices WHERE id = ?",
                String.class, invoice.getId());
        assertThat(frozenName).isEqualTo("Client 1 SL");
    }

    @Test
    @DisplayName("A delivery note keeps the format and tax rates it was created with")
    void deliveryNoteKeepsItsFormatAndTaxes() {
        Integer client = clientService.createClient(
                clientRequest(2, DeliveryNoteFormat.VALUED, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE)).getId();
        DeliveryNoteResponse deliveryNote = createDeliveryNote(client, "1", "1");

        clientService.updateClient(client,
                clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE));
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse reloaded = deliveryNoteService.getDeliveryNoteById(deliveryNote.getId());
        assertThat(reloaded.getFormat()).isEqualTo(DeliveryNoteFormat.VALUED);
        assertThat(reloaded.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(reloaded.getSurchargePercentage()).isEqualByComparingTo("0.5");
    }
}
