package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
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
}
