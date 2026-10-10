package com.luispuchol.selfbill.selfbill_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Schema")
class SchemaTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Foreign keys are enforced")
    void foreignKeysAreEnforced() {
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);

        String insert = "INSERT INTO invoices (series, status, client_id, created_at, updated_at) "
                + "VALUES ('GENERAL', 'DRAFT', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        assertThatThrownBy(() -> jdbc.update(insert, 999999))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    @Test
    @DisplayName("The schema comes from the Flyway migrations")
    void schemaComesFromMigrations() {
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("The code of a deleted article, client or delivery note can be reused")
    void deletedCodesCanBeReused() {
        articleService.deleteArticle(articleService.createArticle(article(10, "Pera")).getId());
        assertThat(articleService.createArticle(article(10, "Pera")).getCode()).isEqualTo(10);

        clientService.deleteClient(createClient(5, DeliveryNoteFormat.PLAIN));
        assertThat(createClient(5, DeliveryNoteFormat.PLAIN)).isNotNull();

        DeliveryNoteResponse deleted = createDeliveryNote(clientId, "1", "1");
        deliveryNoteService.deleteDeliveryNote(deleted.getId());
        assertThat(deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(deleted.getCode(), clientId, "1", "1")).getCode()).isEqualTo(deleted.getCode());
    }

    @Test
    @DisplayName("Live codes stay unique")
    void liveCodesStayUnique() {
        articleService.createArticle(article(10, "Pera"));

        assertFails(ErrorCode.ARTICLE_DUPLICATE_CODE, () -> articleService.createArticle(article(10, "Manzana")));

        String insert = "INSERT INTO articles (code, name, created_at, updated_at) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        assertThatThrownBy(() -> jdbc.update(insert, 10, "Manzana"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    @DisplayName("Invoice numbers are unique within a series")
    void invoiceNumbersAreUniqueWithinASeries() {
        String insert = "INSERT INTO invoices (series, code, status, client_id, created_at, updated_at) "
                + "VALUES (?, ?, 'ISSUED', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(insert, "GENERAL", 1, clientId);
        jdbc.update(insert, "WITHOUT_VAT", 1, clientId);

        assertThatThrownBy(() -> jdbc.update(insert, "GENERAL", 1, clientId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }
}
