package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.luispuchol.selfbill.selfbill_api.shared.BaseEntity_;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class DeliveryNoteSpecification {

    private DeliveryNoteSpecification() {
    }

    public static Specification<DeliveryNote> withFilter(DeliveryNoteFilter filter) {
        return Specification.<DeliveryNote>unrestricted()
                .and(codeFrom(filter.codeFrom()))
                .and(codeTo(filter.codeTo()))
                .and(clientId(filter.clientId()))
                .and(dateFrom(filter.dateFrom()))
                .and(dateTo(filter.dateTo()))
                .and(invoiced(filter.invoiced()))
                .and(withLine(filter.articleId(), filter.traceabilityCode()));
    }

    private static Specification<DeliveryNote> codeFrom(Integer from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get(DeliveryNote_.code), from);
    }

    private static Specification<DeliveryNote> codeTo(Integer to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get(DeliveryNote_.code), to);
    }

    private static Specification<DeliveryNote> clientId(Integer clientId) {
        return (root, query, cb) -> clientId == null ? null
                : cb.equal(root.get(DeliveryNote_.client).get(BaseEntity_.id), clientId);
    }

    private static Specification<DeliveryNote> dateFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.get(DeliveryNote_.date), from);
    }

    private static Specification<DeliveryNote> dateTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.get(DeliveryNote_.date), to);
    }

    private static Specification<DeliveryNote> invoiced(Boolean invoiced) {
        return (root, query, cb) -> invoiced == null ? null
                : invoiced ? cb.isNotNull(root.get(DeliveryNote_.invoice))
                        : cb.isNull(root.get(DeliveryNote_.invoice));
    }

    // Delivery notes with at least one line that matches the article and the code at once (CONCEPTO.md §10)
    private static Specification<DeliveryNote> withLine(Integer articleId, String traceabilityCode) {
        return (root, query, cb) -> {
            if (articleId == null && traceabilityCode == null) return null;
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<DeliveryNoteLine> line = subquery.from(DeliveryNoteLine.class);
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(line.get(DeliveryNoteLine_.deliveryNote), root));
            if (articleId != null) {
                conditions.add(cb.equal(line.get(DeliveryNoteLine_.article).get(BaseEntity_.id), articleId));
            }
            if (traceabilityCode != null) {
                conditions.add(cb.or(
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode1), traceabilityCode),
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode2), traceabilityCode),
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode3), traceabilityCode)));
            }
            return cb.exists(subquery.select(line.get(BaseEntity_.id)).where(conditions.toArray(Predicate[]::new)));
        };
    }
}
