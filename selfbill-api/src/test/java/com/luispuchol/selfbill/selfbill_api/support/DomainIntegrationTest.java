package com.luispuchol.selfbill.selfbill_api.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLineRequest;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteService;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceService;
import com.luispuchol.selfbill.selfbill_api.catalog.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.article.ArticleService;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientService;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.settings.taxconfig.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.settings.taxconfig.TaxConfigService;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

@SpringBootTest
@ActiveProfiles("test")
public abstract class DomainIntegrationTest {

    @Autowired
    protected InvoiceService invoiceService;
    @Autowired
    protected DeliveryNoteService deliveryNoteService;
    @Autowired
    protected ClientService clientService;
    @Autowired
    protected ArticleService articleService;
    @Autowired
    protected TaxConfigService taxConfigService;
    @Autowired
    protected JdbcTemplate jdbc;

    protected Integer clientId;
    protected Integer articleId;
    private int nextDeliveryNoteCode = 1;

    @BeforeEach
    void setUp() {
        for (String table : List.of("invoice_lines", "delivery_notes_articles", "delivery_notes", "invoices",
                "clients", "articles", "tax_config")) {
            jdbc.update("DELETE FROM " + table);
        }
        taxConfigService.createTaxConfig(taxes("4", "0.5"));
        clientId = createClient(1, DeliveryNoteFormat.VALUED);
        articleId = articleService.createArticle(article(1, "Tomate")).id();
    }

    protected Integer createClient(int code, DeliveryNoteFormat format) {
        return clientService.createClient(clientRequest(code, format, "Client " + code + " SL")).id();
    }

    protected Integer createClient(int code, VatType vatType, SurchargeType surchargeType) {
        return clientService.createClient(clientRequest(code, DeliveryNoteFormat.PLAIN, "Client " + code + " SL",
                vatType, surchargeType)).id();
    }

    protected static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName) {
        return clientRequest(code, format, businessName, VatType.WITH_VAT);
    }

    protected static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName,
            VatType vatType) {
        return clientRequest(code, format, businessName, vatType, SurchargeType.WITHOUT_SURCHARGE);
    }

    protected static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName,
            VatType vatType, SurchargeType surchargeType) {
        return ClientRequest.builder()
                .code(code)
                .businessName(businessName)
                .name("Client " + code)
                .nif("B0000000" + code)
                .address("Calle Mayor " + code)
                .city("Valencia")
                .province("Valencia")
                .postalCode("46001")
                .vatType(vatType)
                .surchargeType(surchargeType)
                .defaultDeliveryNoteFormat(format)
                .build();
    }

    protected static ArticleRequest article(int code, String name) {
        return ArticleRequest.builder().code(code).name(name).build();
    }

    protected static TaxConfigRequest taxes(String vat, String surcharge) {
        return TaxConfigRequest.builder()
                .vatPercentage(new BigDecimal(vat))
                .surchargePercentage(new BigDecimal(surcharge))
                .build();
    }

    protected DeliveryNoteResponse createDeliveryNote(Integer client, String quantity, String price) {
        return deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(nextDeliveryNoteCode++, client, quantity, price));
    }

    protected DeliveryNoteRequest deliveryNoteRequest(Integer code, Integer client, String quantity, String price) {
        return DeliveryNoteRequest.builder()
                .code(code)
                .date(LocalDateTime.of(2026, 10, 1, 8, 0))
                .clientId(client)
                .lines(List.of(DeliveryNoteLineRequest.builder()
                        .articleId(articleId)
                        .traceabilityCode1(1)
                        .quantity(new BigDecimal(quantity))
                        .price(new BigDecimal(price))
                        .build()))
                .build();
    }

    protected InvoiceResponse issue(Integer client, DeliveryNoteResponse deliveryNote) {
        InvoiceResponse draft = createInvoiceFor(client, deliveryNote.id());
        return invoiceService.issueInvoice(draft.id());
    }

    protected InvoiceResponse createInvoice(Integer... deliveryNoteIds) {
        return createInvoiceFor(clientId, deliveryNoteIds);
    }

    protected InvoiceResponse createInvoiceFor(Integer client, Integer... deliveryNoteIds) {
        return invoiceService.createInvoice(InvoiceRequest.builder()
                .clientId(client)
                .deliveryNoteIds(List.of(deliveryNoteIds))
                .build());
    }

    protected InvoiceRequest invoiceRequest(Integer... deliveryNoteIds) {
        return InvoiceRequest.builder().clientId(clientId).deliveryNoteIds(List.of(deliveryNoteIds)).build();
    }

    protected static void assertFails(ErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
