package com.luispuchol.selfbill.selfbill_api.specification;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.entity.Article;

public final class ArticleSpecification {

    private ArticleSpecification() {
    }

    public static Specification<Article> withFilter(ArticleFilter filter) {
        return Specification.<Article>unrestricted()
                .and(nameLike(filter.getName()))
                .and(codeFrom(filter.getCodeFrom()))
                .and(codeTo(filter.getCodeTo()));
    }

    private static Specification<Article> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank() ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Article> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("code"), from);
    }

    private static Specification<Article> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("code"), to);
    }
}
