package com.luispuchol.selfbill.selfbill_api.catalog.article;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.shared.SearchText;
import com.luispuchol.selfbill.selfbill_api.shared.SoftDeletableEntity_;

public final class ArticleSpecification {

    private ArticleSpecification() {
    }

    public static Specification<Article> withFilter(ArticleFilter filter) {
        return Specification.<Article>unrestricted()
                .and(nameLike(filter.name()))
                .and(codeFrom(filter.codeFrom()))
                .and(codeTo(filter.codeTo()))
                .and(notDeleted());
    }

    private static Specification<Article> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank() ? null
                : cb.like(root.get(Article_.searchText), "%" + SearchText.normalize(name) + "%");
    }

    private static Specification<Article> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get(Article_.code), from);
    }

    private static Specification<Article> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get(Article_.code), to);
    }

    private static Specification<Article> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(SoftDeletableEntity_.deletedAt));
    }
}
