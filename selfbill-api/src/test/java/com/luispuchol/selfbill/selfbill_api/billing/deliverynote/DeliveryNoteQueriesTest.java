package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

import jakarta.persistence.EntityManagerFactory;

@DisplayName("Delivery note queries")
class DeliveryNoteQueriesTest extends DomainIntegrationTest {

    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("Delivery notes can be filtered by whether they are invoiced")
    void filtersByInvoiced() {
        Integer otherClient = createClient(2, DeliveryNoteFormat.PLAIN);
        DeliveryNoteResponse free = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inIssued = createDeliveryNote(clientId, "1", "1");
        createDeliveryNote(otherClient, "1", "1");
        createInvoice(inDraft.id());
        invoiceService.issueInvoice(createInvoice(inIssued.id()).id());

        assertThat(codes(clientId, false)).containsExactly(free.code());
        assertThat(codes(clientId, true)).containsExactlyInAnyOrder(inDraft.code(), inIssued.code());
        assertThat(codes(clientId, null)).hasSize(3);
    }

    @Test
    @DisplayName("Delivery notes can be found by article and date, as an inspection asks for them")
    void filtersByArticleAndDate() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        Integer otherClient = createClient(2, DeliveryNoteFormat.PLAIN);
        deliver(101, clientId, OCT_1, line(pepper, "01210401"), line(articleId, "01210402"));
        deliver(102, clientId, OCT_1, line(pepper, "01210403"));
        deliver(103, clientId, OCT_1.plusDays(1), line(articleId, "01210404"));
        deliver(104, otherClient, OCT_1.plusDays(1), line(articleId, "01210405"));
        deliver(105, clientId, OCT_1.plusDays(2), line(articleId, "01210406"));

        DeliveryNoteFilter.DeliveryNoteFilterBuilder tomatoes = DeliveryNoteFilter.builder()
                .articleId(articleId).dateFrom(OCT_1.toLocalDate()).dateTo(OCT_1.plusDays(1).toLocalDate());
        assertThat(codes(tomatoes.build())).containsExactlyInAnyOrder(101, 103, 104);
        assertThat(codes(tomatoes.clientId(clientId).build())).containsExactlyInAnyOrder(101, 103);
    }

    @Test
    @DisplayName("A traceability code is found in any of the three slots, once per delivery note")
    void filtersByTraceabilityCodeInAnySlot() {
        deliver(101, clientId, OCT_1, line(articleId, "12241012"));
        deliver(102, clientId, OCT_1, line(articleId, "15241011", "12241012"));
        deliver(103, clientId, OCT_1, line(articleId, "15241011", "16241010", "12241012"));
        deliver(104, clientId, OCT_1, line(articleId, "15241011", "16241010"));
        deliver(105, clientId, OCT_1, line(articleId, "12241012"), line(articleId, "12241012"));

        DeliveryNoteFilter lot = DeliveryNoteFilter.builder().traceabilityCode("12241012").build();
        assertThat(codes(lot)).containsExactlyInAnyOrder(101, 102, 103, 105);
        assertThat(deliveryNoteService.getAllDeliveryNotes(lot, PageRequest.of(0, 2)).getTotalElements())
                .isEqualTo(4);
    }

    @Test
    @DisplayName("Article and traceability code must be on the same line")
    void articleAndCodeMatchTheSameLine() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        deliver(101, clientId, OCT_1, line(articleId, "12241012"), line(pepper, "15241011"));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode("12241012").build()))
                .containsExactly(101);
        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode("15241011").build()))
                .isEmpty();
    }

    @Test
    @DisplayName("Lines replaced by an edit no longer match")
    void ignoresReplacedLines() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        DeliveryNoteResponse edited = deliver(101, clientId, OCT_1, line(articleId, "12241012"));
        deliveryNoteService.updateDeliveryNote(edited.id(),
                deliveryNote(101, clientId, OCT_1, line(pepper, "15241011")));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode("12241012").build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode("15241011").build())).containsExactly(101);
    }

    @Test
    @DisplayName("A traceability code keeps its leading zero")
    void keepsTheLeadingZero() {
        DeliveryNoteResponse created = deliver(101, clientId, OCT_1, line(articleId, "01210426"));

        assertThat(deliveryNoteService.getDeliveryNoteById(created.id()).lines().get(0).traceabilityCode1())
                .isEqualTo("01210426");
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode("01210426").build())).containsExactly(101);
    }

    @Test
    @DisplayName("A delivery note outside any invoice has no invoice state")
    void invoiceStateIsNullWithoutInvoice() {
        DeliveryNoteResponse free = createDeliveryNote(clientId, "1", "1");

        DeliveryNoteResponse found = deliveryNoteService.getDeliveryNoteById(free.id());
        assertThat(found.invoiceStatus()).isNull();
        assertThat(found.invoiceNumber()).isNull();
    }

    @Test
    @DisplayName("A delivery note in a draft invoice shows DRAFT and no number")
    void invoiceStateIsDraftWithoutNumber() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "1", "1");
        createInvoice(note.id());

        DeliveryNoteResponse found = deliveryNoteService.getDeliveryNoteById(note.id());
        assertThat(found.invoiceStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(found.invoiceNumber()).isNull();
    }

    @Test
    @DisplayName("A delivery note in an issued invoice shows ISSUED and the formatted number")
    void invoiceStateIsIssuedWithNumber() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse general = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse series = createDeliveryNote(withoutVat, "1", "1");
        issue(clientId, general);
        issue(withoutVat, series);

        DeliveryNoteResponse foundGeneral = deliveryNoteService.getDeliveryNoteById(general.id());
        assertThat(foundGeneral.invoiceStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(foundGeneral.invoiceNumber()).isEqualTo("1");
        assertThat(deliveryNoteService.getDeliveryNoteById(series.id()).invoiceNumber()).isEqualTo("S-1");
    }

    @Test
    @DisplayName("q finds by trade name ignoring case and accents")
    void qFindsByTradeNameWithoutAccents() {
        Integer garcia = searchableClient(2, "Frutería García", "Hortofrutícola del Sur SL", "B11111111");
        searchableClient(3, "Verduras Pérez", "Pérez e Hijos SL", "B22222222");
        deliver(101, garcia, OCT_1, line(articleId, "01210401"));
        deliver(102, clientId, OCT_1, line(articleId, "01210402"));

        assertThat(codes(DeliveryNoteFilter.builder().q("garcia").build())).containsExactly(101);
        assertThat(codes(DeliveryNoteFilter.builder().q("FRUTERÍA").build())).containsExactly(101);
    }

    @Test
    @DisplayName("q finds by business name")
    void qFindsByBusinessName() {
        Integer garcia = searchableClient(2, "Frutería García", "Hortofrutícola del Sur SL", "B11111111");
        deliver(101, garcia, OCT_1, line(articleId, "01210401"));
        deliver(102, clientId, OCT_1, line(articleId, "01210402"));

        assertThat(codes(DeliveryNoteFilter.builder().q("hortofruticola").build())).containsExactly(101);
    }

    @Test
    @DisplayName("q finds by NIF")
    void qFindsByNif() {
        Integer garcia = searchableClient(2, "Frutería García", "Hortofrutícola del Sur SL", "B11111111");
        deliver(101, garcia, OCT_1, line(articleId, "01210401"));
        deliver(102, clientId, OCT_1, line(articleId, "01210402"));

        assertThat(codes(DeliveryNoteFilter.builder().q("b11111111").build())).containsExactly(101);
    }

    @Test
    @DisplayName("q combines with the invoiced filter")
    void qCombinesWithInvoiced() {
        Integer garcia = searchableClient(2, "Frutería García", "Hortofrutícola del Sur SL", "B11111111");
        DeliveryNoteResponse pending = deliver(101, garcia, OCT_1, line(articleId, "01210401"));
        DeliveryNoteResponse invoiced = deliver(102, garcia, OCT_1, line(articleId, "01210402"));
        deliver(103, clientId, OCT_1, line(articleId, "01210403"));
        createInvoiceFor(garcia, invoiced.id());

        assertThat(codes(DeliveryNoteFilter.builder().q("garcia").invoiced(false).build()))
                .containsExactly(pending.code());
        assertThat(codes(DeliveryNoteFilter.builder().q("garcia").invoiced(true).build()))
                .containsExactly(invoiced.code());
    }

    @Test
    @DisplayName("The list shows the invoice state of each row without a query per invoice")
    void listCarriesInvoiceStateWithoutExtraQueries() {
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inIssued = createDeliveryNote(clientId, "1", "1");
        createInvoice(inDraft.id());
        issue(clientId, inIssued);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            List<DeliveryNoteResponse> page = deliveryNoteService
                    .getAllDeliveryNotes(DeliveryNoteFilter.builder().build(), PageRequest.of(0, 20)).getContent();

            assertThat(page).extracting(DeliveryNoteResponse::code, DeliveryNoteResponse::invoiceStatus,
                    DeliveryNoteResponse::invoiceNumber).containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(pending.code(), null, null),
                            org.assertj.core.groups.Tuple.tuple(inDraft.code(), InvoiceStatus.DRAFT, null),
                            org.assertj.core.groups.Tuple.tuple(inIssued.code(), InvoiceStatus.ISSUED, "1"));
            // Invoices and clients arrive with the page; none is fetched on its own
            assertThat(statistics.getEntityStatistics(Invoice.class.getName()).getFetchCount()).isZero();
            assertThat(statistics.getEntityStatistics(Client.class.getName()).getFetchCount()).isZero();
        } finally {
            // Shared by the whole context: a failure here must not leave it on for other tests
            statistics.setStatisticsEnabled(false);
        }
    }

    private Integer searchableClient(int code, String name, String businessName, String nif) {
        return clientService.createClient(ClientRequest.builder()
                .code(code)
                .businessName(businessName)
                .name(name)
                .nif(nif)
                .address("Calle Mayor " + code)
                .city("Valencia")
                .postalCode("46001")
                .vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN)
                .build()).id();
    }

    private DeliveryNoteResponse deliver(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return deliveryNoteService.createDeliveryNote(deliveryNote(code, client, date, lines));
    }

    private static DeliveryNoteRequest deliveryNote(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return DeliveryNoteRequest.builder().code(code).date(date).clientId(client).lines(List.of(lines)).build();
    }

    private static DeliveryNoteLineRequest line(Integer article, String... traceabilityCodes) {
        return DeliveryNoteLineRequest.builder()
                .articleId(article)
                .traceabilityCode1(traceabilityCodes[0])
                .traceabilityCode2(traceabilityCodes.length > 1 ? traceabilityCodes[1] : null)
                .traceabilityCode3(traceabilityCodes.length > 2 ? traceabilityCodes[2] : null)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal.ONE)
                .build();
    }

    private List<Integer> codes(Integer client, Boolean invoiced) {
        return codes(DeliveryNoteFilter.builder().clientId(client).invoiced(invoiced).build());
    }

    private List<Integer> codes(DeliveryNoteFilter filter) {
        return deliveryNoteService.getAllDeliveryNotes(filter, Pageable.unpaged())
                .map(DeliveryNoteResponse::code).getContent();
    }
}
