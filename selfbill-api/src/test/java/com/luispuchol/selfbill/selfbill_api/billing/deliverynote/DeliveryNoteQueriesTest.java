package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Delivery note queries")
class DeliveryNoteQueriesTest extends DomainIntegrationTest {

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

    private List<Integer> codes(Integer client, Boolean invoiced) {
        DeliveryNoteFilter filter = DeliveryNoteFilter.builder().clientId(client).invoiced(invoiced).build();
        return deliveryNoteService.getAllDeliveryNotes(filter, Pageable.unpaged())
                .map(DeliveryNoteResponse::code).getContent();
    }
}
