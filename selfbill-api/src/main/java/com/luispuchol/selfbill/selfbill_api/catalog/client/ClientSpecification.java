package com.luispuchol.selfbill.selfbill_api.catalog.client;

import org.springframework.data.jpa.domain.Specification;

public final class ClientSpecification {

    private ClientSpecification() {
    }

    public static Specification<Client> withFilter(ClientFilter filter) {
        return Specification.<Client>unrestricted()
                .and(nameLike(filter.getName()))
                .and(businessNameLike(filter.getBusinessName()))
                .and(codeFrom(filter.getCodeFrom()))
                .and(codeTo(filter.getCodeTo()))
                .and(notDeleted());
    }

    private static Specification<Client> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank() ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Client> businessNameLike(String businessName) {
        return (root, query, cb) -> businessName == null || businessName.isBlank() ? null
                : cb.like(cb.lower(root.get("businessName")), "%" + businessName.toLowerCase() + "%");
    }

    private static Specification<Client> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("code"), from);
    }

    private static Specification<Client> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("code"), to);
    }

    private static Specification<Client> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
