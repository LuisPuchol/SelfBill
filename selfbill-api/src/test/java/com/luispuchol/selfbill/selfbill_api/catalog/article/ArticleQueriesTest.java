package com.luispuchol.selfbill.selfbill_api.catalog.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

class ArticleQueriesTest extends DomainIntegrationTest {

    @Test
    void findsByNameIgnoringAccentsAndCase() {
        articleService.createArticle(article(2, "Plátano canario"));
        for (String name : List.of("platano", "PLÁTANO")) {
            assertThat(articleService.getAllArticles(ArticleFilter.builder().name(name).build(), PageRequest.of(0, 20))
                    .map(ArticleResponse::code).getContent()).containsExactly(2);
        }
    }

    @Test
    void deletedFilterListsOnlyDeleted() {
        Integer gone = articleService.createArticle(article(2, "Plátano canario")).id();
        articleService.createArticle(article(3, "Plátano verde"));
        articleService.deleteArticle(gone);

        var deleted = articleService.getAllArticles(ArticleFilter.builder().deleted(true).name("platano").build(),
                PageRequest.of(0, 20)).getContent();
        assertThat(deleted).extracting(ArticleResponse::code).containsExactly(2);
        assertThat(deleted.get(0).deletedAt()).isNotNull();
        assertThat(articleService.getAllArticles(ArticleFilter.builder().deleted(true).codeFrom(3).build(),
                PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    @Test
    void liveListIsUnchanged() {
        Integer gone = articleService.createArticle(article(2, "Plátano canario")).id();
        articleService.deleteArticle(gone);

        for (Boolean deleted : new Boolean[] { null, false }) {
            var live = articleService.getAllArticles(ArticleFilter.builder().deleted(deleted).build(),
                    PageRequest.of(0, 20)).getContent();
            assertThat(live).extracting(ArticleResponse::code).containsExactly(1);
            assertThat(live.get(0).deletedAt()).isNull();
        }
    }
}
