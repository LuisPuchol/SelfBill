package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V5 migration: invoice issuer snapshot")
class InvoiceSnapshotMigrationTest {

    @Test
    @DisplayName("Fills the issued invoices, deleted ones included, and leaves drafts alone")
    void fillsTheIssuedInvoices() throws Exception {
        String url = freshDatabase();
        flyway(url).target("4").load().migrate();
        try (Connection connection = DriverManager.getConnection(url); Statement sql = connection.createStatement()) {
            sql.execute("insert into user_profile (id, full_name, fiscal_name, nif, address, created_at, updated_at) "
                    + "values (1, 'Luis', 'Luis SL', 'B00000009', 'Mercavalencia, nave 3', "
                    + "current_timestamp, current_timestamp)");
            insertClient(sql, 1, 41);
            insertClient(sql, 2, 42);
            insertInvoice(sql, 1, "ISSUED", 1, false);
            insertInvoice(sql, 2, "ISSUED", 2, true);
            insertInvoice(sql, 3, "DRAFT", 1, false);
        }

        flyway(url).load().migrate();

        List<Map<String, Object>> rows = query(url,
                "select issuer_fiscal_name, issuer_nif, issuer_address, client_code from invoices order by id");
        assertThat(rows.get(0)).containsEntry("issuer_fiscal_name", "Luis SL").containsEntry("issuer_nif", "B00000009")
                .containsEntry("issuer_address", "Mercavalencia, nave 3").containsEntry("client_code", 41);
        assertThat(rows.get(1)).containsEntry("issuer_fiscal_name", "Luis SL").containsEntry("issuer_nif", "B00000009")
                .containsEntry("issuer_address", "Mercavalencia, nave 3").containsEntry("client_code", 42);
        assertThat(rows.get(2).values()).containsOnlyNulls();
    }

    @Test
    @DisplayName("Without a profile the issued invoices get the client code and no issuer")
    void fillsWithoutAProfile() throws Exception {
        String url = freshDatabase();
        flyway(url).target("4").load().migrate();
        try (Connection connection = DriverManager.getConnection(url); Statement sql = connection.createStatement()) {
            insertClient(sql, 1, 41);
            insertInvoice(sql, 1, "ISSUED", 1, false);
        }

        flyway(url).load().migrate();

        Map<String, Object> row = query(url,
                "select issuer_fiscal_name, issuer_nif, issuer_address, client_code from invoices").get(0);
        assertThat(row).containsEntry("client_code", 41).containsEntry("issuer_fiscal_name", null)
                .containsEntry("issuer_nif", null).containsEntry("issuer_address", null);
    }

    private static FluentConfiguration flyway(String url) {
        return Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration");
    }

    private static String freshDatabase() throws Exception {
        Path dir = Path.of("target");
        Files.createDirectories(dir);
        return "jdbc:sqlite:" + dir.resolve("migration-test-" + UUID.randomUUID() + ".db");
    }

    private static void insertClient(Statement sql, int id, int code) throws SQLException {
        sql.execute("insert into clients (id, code, business_name, name, vat_type, surcharge_type, "
                + "default_delivery_note_format, created_at, updated_at) values (" + id + ", " + code
                + ", 'Client SL', 'Client', 'WITH_VAT', 'WITHOUT_SURCHARGE', 'PLAIN', "
                + "current_timestamp, current_timestamp)");
    }

    private static void insertInvoice(Statement sql, int id, String status, int clientId, boolean deleted)
            throws SQLException {
        sql.execute("insert into invoices (id, series, code, status, client_id, created_at, updated_at, deleted_at) "
                + "values (" + id + ", 'GENERAL', " + ("ISSUED".equals(status) ? id : "null") + ", '" + status + "', "
                + clientId + ", current_timestamp, current_timestamp, "
                + (deleted ? "current_timestamp" : "null") + ")");
    }

    private static List<Map<String, Object>> query(String url, String select) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
                Statement sql = connection.createStatement();
                var rs = sql.executeQuery(select)) {
            var meta = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }
}
