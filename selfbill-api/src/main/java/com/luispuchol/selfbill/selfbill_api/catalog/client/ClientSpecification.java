package com.luispuchol.selfbill.selfbill_api.catalog.client;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.shared.SoftDeletableEntity_;

public final class ClientSpecification {

    private ClientSpecification() {
    }

    public static Specification<Client> withFilter(ClientFilter filter) {
        return Specification.<Client>unrestricted()
                .and(nameLike(filter.name()))
                .and(businessNameLike(filter.businessName()))
                .and(codeFrom(filter.codeFrom()))
                .and(codeTo(filter.codeTo()))
                .and(notDeleted());
    }

    private static Specification<Client> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank() ? null
                : cb.like(cb.lower(root.get(Client_.name)), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Client> businessNameLike(String businessName) {
        return (root, query, cb) -> businessName == null || businessName.isBlank() ? null
                : cb.like(cb.lower(root.get(Client_.businessName)), "%" + businessName.toLowerCase() + "%");
    }

    private static Specification<Client> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get(Client_.code), from);
    }

    private static Specification<Client> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get(Client_.code), to);
    }

    private static Specification<Client> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get(SoftDeletableEntity_.deletedAt));
    }
}
