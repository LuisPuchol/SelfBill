package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteLineRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Invoice lifecycle")
class InvoiceLifecycleTest {

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private DeliveryNoteService deliveryNoteService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private TaxConfigService taxConfigService;
    @Autowired
    private JdbcTemplate jdbc;

    private Integer clientId;
    private Integer articleId;
    private int nextDeliveryNoteCode = 1;

    @BeforeEach
    void setUp() {
        for (String table : List.of("invoice_lines", "delivery_notes_articles", "delivery_notes", "invoices",
                "clients", "articles", "tax_config")) {
            jdbc.update("DELETE FROM " + table);
        }
        taxConfigService.createTaxConfig(taxes("4", "0.5"));
        clientId = createClient(1, DeliveryNoteFormat.VALUED);
        articleId = articleService.createArticle(ArticleRequest.builder().code(1).name("Tomate").build()).getId();
    }

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

        // Simulates a rectifying draft taking over the delivery note (CONCEPTO.md §9.1)
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
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.PLAIN, "Client 1 SL"));
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse reloaded = deliveryNoteService.getDeliveryNoteById(deliveryNote.getId());
        assertThat(reloaded.getFormat()).isEqualTo(DeliveryNoteFormat.VALUED);
        assertThat(reloaded.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(reloaded.getSurchargePercentage()).isEqualByComparingTo("0.5");
    }

    private Integer createClient(int code, DeliveryNoteFormat format) {
        return clientService.createClient(clientRequest(code, format, "Client " + code + " SL")).getId();
    }

    private static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName) {
        return ClientRequest.builder()
                .code(code)
                .businessName(businessName)
                .name("Client " + code)
                .nif("B0000000" + code)
                .address("Calle Mayor " + code)
                .city("Valencia")
                .province("Valencia")
                .postalCode("46001")
                .vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(format)
                .build();
    }

    private static TaxConfigRequest taxes(String vat, String surcharge) {
        TaxConfigRequest request = new TaxConfigRequest();
        request.setVatPercentage(new BigDecimal(vat));
        request.setSurchargePercentage(new BigDecimal(surcharge));
        return request;
    }

    private DeliveryNoteResponse createDeliveryNote(Integer client, String quantity, String price) {
        return deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(nextDeliveryNoteCode++, client, quantity, price));
    }

    private DeliveryNoteRequest deliveryNoteRequest(Integer code, Integer client, String quantity, String price) {
        return DeliveryNoteRequest.builder()
                .code(code)
                .date(LocalDateTime.of(2026, 10, 1, 8, 0))
                .clientId(client)
                .lines(List.of(DeliveryNoteLineRequest.builder()
                        .articleId(articleId)
                        .trazabilityCode1(1)
                        .quantity(new BigDecimal(quantity))
                        .price(new BigDecimal(price))
                        .build()))
                .build();
    }

    private InvoiceResponse createInvoice(Integer... deliveryNoteIds) {
        return invoiceService.createInvoice(invoiceRequest(deliveryNoteIds));
    }

    private InvoiceRequest invoiceRequest(Integer... deliveryNoteIds) {
        return InvoiceRequest.builder().clientId(clientId).deliveryNoteIds(List.of(deliveryNoteIds)).build();
    }

    private static void assertFails(ErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
