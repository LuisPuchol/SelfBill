package com.luispuchol.selfbill.selfbill_api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Catalog restore")
class CatalogRestoreTest extends DomainIntegrationTest {

    @Test
    @DisplayName("A restored client can be invoiced again")
    void restoresADeletedClient() {
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");
        clientService.deleteClient(clientId);
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> createInvoice(pending.id()));

        ClientResponse restored = clientService.restoreClient(clientId);

        assertThat(restored.deletedAt()).isNull();
        assertThat(clientService.getClientById(clientId).code()).isEqualTo(1);
        assertThat(createInvoice(pending.id()).id()).isNotNull();
    }

    @Test
    @DisplayName("A pending note using a restored article saves again")
    void restoresADeletedArticle() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "1", "1");
        articleService.deleteArticle(articleId);

        assertThat(articleService.restoreArticle(articleId).deletedAt()).isNull();

        assertThat(deliveryNoteService.updateDeliveryNote(note.id(),
                deliveryNoteRequest(note.code(), clientId, "2", "1")).id()).isEqualTo(note.id());
    }

    @Test
    @DisplayName("Restoring a live row changes nothing")
    void restoringALiveOneChangesNothing() {
        assertThat(clientService.restoreClient(clientId).id()).isEqualTo(clientId);
        assertThat(articleService.restoreArticle(articleId).id()).isEqualTo(articleId);
        assertThat(clientService.getAllClients(ClientFilter.builder().build(), Pageable.unpaged()).getContent())
                .hasSize(1);
        assertThat(articleService.getAllArticles(ArticleFilter.builder().build(), Pageable.unpaged()).getContent())
                .hasSize(1);
    }

    @Test
    @DisplayName("A code taken by a live row refuses the restore and leaves the row deleted")
    void restoreRefusesATakenCode() {
        clientService.deleteClient(clientId);
        articleService.deleteArticle(articleId);
        createClient(1, DeliveryNoteFormat.PLAIN);
        articleService.createArticle(article(1, "Otro"));

        assertFails(ErrorCode.CLIENT_DUPLICATE_CODE, () -> clientService.restoreClient(clientId));
        assertFails(ErrorCode.ARTICLE_DUPLICATE_CODE, () -> articleService.restoreArticle(articleId));

        assertThat(clientService.getAllClients(ClientFilter.builder().deleted(true).build(), Pageable.unpaged())
                .getContent()).extracting(ClientResponse::id).containsExactly(clientId);
        assertThat(articleService.getAllArticles(ArticleFilter.builder().deleted(true).build(), Pageable.unpaged())
                .getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Restoring an id that does not exist is not found")
    void restoreOfAMissingIdIsNotFound() {
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.restoreClient(9999));
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> articleService.restoreArticle(9999));
    }
}
