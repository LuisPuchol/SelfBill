package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Delivery note queries")
class DeliveryNoteQueriesTest extends DomainIntegrationTest {

    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 8, 0);

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
        deliver(101, clientId, OCT_1, line(pepper, 1), line(articleId, 2));
        deliver(102, clientId, OCT_1, line(pepper, 3));
        deliver(103, clientId, OCT_1.plusDays(1), line(articleId, 4));
        deliver(104, otherClient, OCT_1.plusDays(1), line(articleId, 5));
        deliver(105, clientId, OCT_1.plusDays(2), line(articleId, 6));

        DeliveryNoteFilter.DeliveryNoteFilterBuilder tomatoes = DeliveryNoteFilter.builder()
                .articleId(articleId).dateFrom(OCT_1).dateTo(OCT_1.plusDays(1));
        assertThat(codes(tomatoes.build())).containsExactlyInAnyOrder(101, 103, 104);
        assertThat(codes(tomatoes.clientId(clientId).build())).containsExactlyInAnyOrder(101, 103);
    }

    @Test
    @DisplayName("A traceability code is found in any of the three slots, once per delivery note")
    void filtersByTraceabilityCodeInAnySlot() {
        deliver(101, clientId, OCT_1, line(articleId, 12241012));
        deliver(102, clientId, OCT_1, line(articleId, 15241011, 12241012));
        deliver(103, clientId, OCT_1, line(articleId, 15241011, 16241010, 12241012));
        deliver(104, clientId, OCT_1, line(articleId, 15241011, 16241010));
        deliver(105, clientId, OCT_1, line(articleId, 12241012), line(articleId, 12241012));

        DeliveryNoteFilter lot = DeliveryNoteFilter.builder().traceabilityCode(12241012).build();
        assertThat(codes(lot)).containsExactlyInAnyOrder(101, 102, 103, 105);
        assertThat(deliveryNoteService.getAllDeliveryNotes(lot, PageRequest.of(0, 2)).getTotalElements())
                .isEqualTo(4);
    }

    @Test
    @DisplayName("Article and traceability code must be on the same line")
    void articleAndCodeMatchTheSameLine() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        deliver(101, clientId, OCT_1, line(articleId, 12241012), line(pepper, 15241011));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode(12241012).build()))
                .containsExactly(101);
        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode(15241011).build()))
                .isEmpty();
    }

    @Test
    @DisplayName("Lines replaced by an edit no longer match")
    void ignoresReplacedLines() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        DeliveryNoteResponse edited = deliver(101, clientId, OCT_1, line(articleId, 12241012));
        deliveryNoteService.updateDeliveryNote(edited.id(),
                deliveryNote(101, clientId, OCT_1, line(pepper, 15241011)));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode(12241012).build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode(15241011).build())).containsExactly(101);
    }

    private DeliveryNoteResponse deliver(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return deliveryNoteService.createDeliveryNote(deliveryNote(code, client, date, lines));
    }

    private static DeliveryNoteRequest deliveryNote(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return DeliveryNoteRequest.builder().code(code).date(date).clientId(client).lines(List.of(lines)).build();
    }

    private static DeliveryNoteLineRequest line(Integer article, Integer... traceabilityCodes) {
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
