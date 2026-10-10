package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
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
        createInvoice(inDraft.getId());
        invoiceService.issueInvoice(createInvoice(inIssued.getId()).getId());

        assertThat(codes(clientId, false)).containsExactly(free.getCode());
        assertThat(codes(clientId, true)).containsExactlyInAnyOrder(inDraft.getCode(), inIssued.getCode());
        assertThat(codes(clientId, null)).hasSize(3);
    }

    private List<Integer> codes(Integer client, Boolean invoiced) {
        DeliveryNoteFilter filter = new DeliveryNoteFilter();
        filter.setClientId(client);
        filter.setInvoiced(invoiced);
        return deliveryNoteService.getAllDeliveryNotes(filter, Pageable.unpaged())
                .map(DeliveryNoteResponse::getCode).getContent();
    }
}
