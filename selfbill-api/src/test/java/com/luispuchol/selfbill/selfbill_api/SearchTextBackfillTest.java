package com.luispuchol.selfbill.selfbill_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.luispuchol.selfbill.selfbill_api.shared.SearchText;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;
import db.migration.V4__Search_text;

class SearchTextBackfillTest extends DomainIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void backfillNormalizesExistingRows() throws Exception {
        jdbc.update("UPDATE clients SET search_text = ''");
        jdbc.update("UPDATE articles SET search_text = ''");

        try (Connection connection = dataSource.getConnection()) {
            V4__Search_text.backfill(connection);
        }

        String expected = jdbc.queryForObject("SELECT name || '|' || business_name || '|' || nif FROM clients",
                String.class);
        String[] parts = expected.split("[|]");
        assertThat(jdbc.queryForObject("SELECT search_text FROM clients", String.class))
                .isEqualTo(SearchText.normalize(parts[0], parts[1], parts[2]));
        assertThat(jdbc.queryForObject("SELECT search_text FROM articles", String.class)).isEqualTo("tomate");
    }
}
