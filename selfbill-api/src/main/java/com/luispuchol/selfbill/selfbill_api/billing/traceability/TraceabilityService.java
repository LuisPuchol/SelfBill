package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        // By day, then number: the time of day does not reorder the numbers of a day. Sorted here because
        // a day cast in the query is not reliable on SQLite. One row per note, in that order
        List<DeliveryNoteLine> lines = new ArrayList<>(
                repository.findLinesOfLot(articleId, code, start(dateFrom), end(dateTo)));
        lines.sort(Comparator.<DeliveryNoteLine, LocalDate>comparing(l -> l.getDeliveryNote().getDate().toLocalDate())
                .thenComparing(l -> l.getDeliveryNote().getCode()));
        Map<DeliveryNote, BigDecimal> quantities = new LinkedHashMap<>();
        for (DeliveryNoteLine line : lines) {
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

    // A line migrated from the old program may have no lot: its first code, which cannot be null, is then empty
    private static List<String> codes(String code1, String code2, String code3) {
        return Stream.of(code1, code2, code3).filter(StringUtils::hasText).distinct().toList();
    }

    private static LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private static LocalDateTime end(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
