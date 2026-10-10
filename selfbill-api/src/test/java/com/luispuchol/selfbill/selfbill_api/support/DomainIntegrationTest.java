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

import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteLineRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;
import com.luispuchol.selfbill.selfbill_api.service.ClientService;
import com.luispuchol.selfbill.selfbill_api.service.DeliveryNoteService;
import com.luispuchol.selfbill.selfbill_api.service.InvoiceService;
import com.luispuchol.selfbill.selfbill_api.service.TaxConfigService;

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
        articleId = articleService.createArticle(article(1, "Tomate")).getId();
    }

    protected Integer createClient(int code, DeliveryNoteFormat format) {
        return clientService.createClient(clientRequest(code, format, "Client " + code + " SL")).getId();
    }

    protected Integer createClient(int code, VatType vatType, SurchargeType surchargeType) {
        return clientService.createClient(clientRequest(code, DeliveryNoteFormat.PLAIN, "Client " + code + " SL",
                vatType, surchargeType)).getId();
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
        TaxConfigRequest request = new TaxConfigRequest();
        request.setVatPercentage(new BigDecimal(vat));
        request.setSurchargePercentage(new BigDecimal(surcharge));
        return request;
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
                        .trazabilityCode1(1)
                        .quantity(new BigDecimal(quantity))
                        .price(new BigDecimal(price))
                        .build()))
                .build();
    }

    protected InvoiceResponse issue(Integer client, DeliveryNoteResponse deliveryNote) {
        InvoiceResponse draft = createInvoiceFor(client, deliveryNote.getId());
        return invoiceService.issueInvoice(draft.getId());
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
