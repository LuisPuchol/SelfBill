package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLine;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceabilityService {

    private final TraceabilityRepository repository;

    public List<TraceabilityArticleResponse> getArticles(LocalDate dateFrom, LocalDate dateTo) {
        return repository.findArticles(start(dateFrom), end(dateTo)).stream()
                .map(row -> TraceabilityArticleResponse.builder()
                        .articleId((Integer) row[0])
                        .articleCode((Integer) row[1])
                        .articleName((String) row[2])
                        .build())
                .toList();
    }

    public List<TraceabilityLotResponse> getLots(Integer articleId, LocalDate dateFrom, LocalDate dateTo) {
        Map<String, BigDecimal> quantities = new TreeMap<>();
        for (Object[] row : repository.findLotRows(articleId, start(dateFrom), end(dateTo))) {
            // A lot repeated in the slots of one line counts once for that line
            codes((String) row[0], (String) row[1], (String) row[2])
                    .forEach(code -> quantities.merge(code, (BigDecimal) row[3], BigDecimal::add));
        }
        return quantities.entrySet().stream()
                .map(e -> TraceabilityLotResponse.builder().code(e.getKey()).quantity(e.getValue()).build())
                .toList();
    }

    public List<TraceabilityDeliveryResponse> getDeliveries(Integer articleId, String code, LocalDate dateFrom,
            LocalDate dateTo) {
        // The lines come ordered by date and code: one row per note, in that order
        Map<DeliveryNote, BigDecimal> quantities = new LinkedHashMap<>();
        for (DeliveryNoteLine line : repository.findLinesOfLot(articleId, code, start(dateFrom), end(dateTo))) {
            quantities.merge(line.getDeliveryNote(), line.getQuantity(), BigDecimal::add);
        }
        return quantities.entrySet().stream()
                .map(e -> toDelivery(e.getKey(), e.getValue()))
                .toList();
    }

    private static TraceabilityDeliveryResponse toDelivery(DeliveryNote note, BigDecimal quantity) {
        Client client = note.getClient();
        return TraceabilityDeliveryResponse.builder()
                .deliveryNoteId(note.getId())
                .deliveryNoteCode(note.getCode())
                .date(note.getDate())
                .clientId(client.getId())
                .clientCode(client.getCode())
                .clientName(client.getName())
                .quantity(quantity)
                .build();
    }

    private static List<String> codes(String code1, String code2, String code3) {
        return Stream.of(code1, code2, code3).filter(Objects::nonNull).distinct().toList();
    }

    private static LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private static LocalDateTime end(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
