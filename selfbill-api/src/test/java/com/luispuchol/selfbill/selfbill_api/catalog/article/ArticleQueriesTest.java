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
}
