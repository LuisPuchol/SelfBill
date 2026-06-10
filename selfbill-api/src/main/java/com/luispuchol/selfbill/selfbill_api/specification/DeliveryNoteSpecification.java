package com.luispuchol.selfbill.selfbill_api.specification;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;

public class DeliveryNoteSpecification {

    public static Specification<DeliveryNote> withFilter(DeliveryNoteFilter filter) {
        return Specification.<DeliveryNote>unrestricted()
                .and(codeFrom(filter.getCodeFrom()))
                .and(codeTo(filter.getCodeTo()))
                .and(clientId(filter.getClientId()))
                .and(dateFrom(filter.getDateFrom()))
                .and(dateTo(filter.getDateTo()));
    }

    private static Specification<DeliveryNote> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("code"), from);
    }

    private static Specification<DeliveryNote> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("code"), to);
    }

    private static Specification<DeliveryNote> clientId(Integer clientId) {
        return (root, query, cb) -> clientId == null ? null
                : cb.equal(root.get("client").get("id"), clientId);
    }

    private static Specification<DeliveryNote> dateFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    private static Specification<DeliveryNote> dateTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get("date"), to);
    }
}
