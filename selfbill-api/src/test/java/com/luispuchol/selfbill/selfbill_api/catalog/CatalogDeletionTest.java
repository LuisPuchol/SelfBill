package com.luispuchol.selfbill.selfbill_api.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Catalog deletion")
class CatalogDeletionTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Documents of a deleted client stay readable")
    void deletedClientKeepsItsDocumentsReadable() {
        DeliveryNoteResponse invoiced = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(invoiced.id()).id());

        clientService.deleteClient(clientId);

        assertThat(deliveryNoteService.getDeliveryNoteById(pending.id()).clientName()).isEqualTo("Client 1");
        assertThat(deliveryNoteService.getAllDeliveryNotes(DeliveryNoteFilter.builder().build(), Pageable.unpaged())
                .getContent()).hasSize(2);
        assertThat(invoiceService.getInvoiceById(invoice.id()).clientName()).isEqualTo("Client 1");
        assertThat(invoiceService.getAllInvoices(InvoiceFilter.builder().build(), Pageable.unpaged()).getContent())
                .hasSize(1);
    }

    @Test
    @DisplayName("Delivery notes using a deleted article stay readable")
    void deletedArticleKeepsItsDocumentsReadable() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");

        articleService.deleteArticle(articleId);

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.id()).lines().get(0).articleName())
                .isEqualTo("Tomate");
    }

    @Test
    @DisplayName("The catalog hides deleted clients and articles")
    void catalogHidesDeletedRows() {
        clientService.deleteClient(clientId);
        articleService.deleteArticle(articleId);

        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.getClientById(clientId));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.getClientByCode(1));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.deleteClient(clientId));
        assertThat(clientService.getAllClients(ClientFilter.builder().build(), Pageable.unpaged()).getContent())
                .isEmpty();
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> articleService.getArticleById(articleId));
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> articleService.getArticleByCode(1));
        assertThat(articleService.getAllArticles(ArticleFilter.builder().build(), Pageable.unpaged()).getContent())
                .isEmpty();
    }

    @Test
    @DisplayName("New documents cannot use a deleted client or article")
    void newDocumentsRejectDeletedCatalogRows() {
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");

        articleService.deleteArticle(articleId);
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> createDeliveryNote(clientId, "1", "1"));

        clientService.deleteClient(clientId);
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> createDeliveryNote(clientId, "1", "1"));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> createInvoice(pending.id()));
    }

    @Test
    @DisplayName("Re-saving a delivery note whose article was deleted fails with a not-found error")
    void resavingWithDeletedArticleFailsCleanly() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        articleService.deleteArticle(articleId);

        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> deliveryNoteService.updateDeliveryNote(deliveryNote.id(),
                deliveryNoteRequest(deliveryNote.code(), clientId, "2", "1")));
    }

    @Test
    @DisplayName("Lookups return the live row when a deleted one shares its code, name or NIF")
    void lookupsPreferTheLiveRow() {
        clientService.deleteClient(clientId);
        articleService.deleteArticle(articleId);
        Integer liveClient = createClient(1, DeliveryNoteFormat.PLAIN);
        Integer liveArticle = articleService.createArticle(article(1, "Tomate")).id();

        assertThat(clientService.getClientByCode(1).id()).isEqualTo(liveClient);
        assertThat(clientService.getClientByName("Client 1").id()).isEqualTo(liveClient);
        assertThat(clientService.getClientByNif("B00000001").id()).isEqualTo(liveClient);
        assertThat(articleService.getArticleByCode(1).id()).isEqualTo(liveArticle);
        assertThat(articleService.getArticleByName("Tomate").id()).isEqualTo(liveArticle);
    }
}
