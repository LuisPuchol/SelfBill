package com.luispuchol.selfbill.selfbill_api.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;

public class InvoiceSpecification {

    private InvoiceSpecification() {}

    public static Specification<Invoice> withFilter(InvoiceFilter filter) {
        return Specification.<Invoice>unrestricted()
                .and(codeFrom(filter.getCodeFrom()))
                .and(codeTo(filter.getCodeTo()))
                .and(clientId(filter.getClientId()))
                .and(deliveryNoteId(filter.getDeliveryNoteId()))
                .and(status(filter.getStatus()))
                .and(dateFrom(filter.getDateFrom()))
                .and(dateTo(filter.getDateTo()));
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

    // Finds invoices that contain the given delivery note
    private static Specification<Invoice> deliveryNoteId(Integer deliveryNoteId) {
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
