package com.luispuchol.selfbill.selfbill_api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Code suggestion")
class CodeSuggestionTest extends DomainIntegrationTest {

    @Test
    @DisplayName("The suggested client code follows the highest live one")
    void suggestsNextClientCode() {
        Integer seventh = createClient(7, DeliveryNoteFormat.PLAIN);
        assertThat(clientService.getNextCode().code()).isEqualTo(8);

        clientService.deleteClient(seventh);
        assertThat(clientService.getNextCode().code()).isEqualTo(2);
    }

    @Test
    @DisplayName("The suggested article code follows the highest live one, starting at 1")
    void suggestsNextArticleCode() {
        assertThat(articleService.getNextCode().code()).isEqualTo(2);

        articleService.deleteArticle(articleId);
        assertThat(articleService.getNextCode().code()).isEqualTo(1);
    }

    @Test
    @DisplayName("A delivery note without code gets the next free one")
    void deliveryNoteWithoutCodeGetsTheNextOne() {
        deliveryNoteService.createDeliveryNote(deliveryNoteRequest(5, clientId, "1", "1"));

        DeliveryNoteResponse automatic = deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(null, clientId, "1", "1"));

        assertThat(automatic.getCode()).isEqualTo(6);
    }

    @Test
    @DisplayName("Editing a delivery note without code keeps its current one")
    void editingWithoutCodeKeepsIt() {
        DeliveryNoteResponse deliveryNote = deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(5, clientId, "1", "1"));

        DeliveryNoteResponse edited = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(null, clientId, "2", "1"));

        assertThat(edited.getCode()).isEqualTo(5);
    }
}
