package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

public final class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> withFilter(InvoiceFilter filter) {
        return Specification.<Invoice>unrestricted()
                .and(series(filter.series()))
                .and(codeFrom(filter.codeFrom()))
                .and(codeTo(filter.codeTo()))
                .and(clientId(filter.clientId()))
                .and(containsDeliveryNote(filter.deliveryNoteId()))
                .and(status(filter.status()))
                .and(dateFrom(filter.dateFrom()))
                .and(dateTo(filter.dateTo()));
    }

    private static Specification<Invoice> series(InvoiceSeries series) {
        return (root, query, cb) -> series == null ? null
                : cb.equal(root.get("series"), series);
    }

    private static Specification<Invoice> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("code"), from);
    }

    private static Specification<Invoice> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("code"), to);
    }

    private static Specification<Invoice> clientId(Integer clientId) {
        return (root, query, cb) -> clientId == null ? null
                : cb.equal(root.get("client").get("id"), clientId);
    }

    private static Specification<Invoice> status(InvoiceStatus status) {
        return (root, query, cb) -> status == null ? null
                : cb.equal(root.get("status"), status);
    }

    private static Specification<Invoice> containsDeliveryNote(Integer deliveryNoteId) {
        return (root, query, cb) -> {
            if (deliveryNoteId == null) return null;
            if (query != null) query.distinct(true);
            return cb.equal(root.join("deliveryNotes").get("id"), deliveryNoteId);
        };
    }

    private static Specification<Invoice> dateFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    private static Specification<Invoice> dateTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("date"), to);
    }
}
