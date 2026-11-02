package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLineRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/traceability")
@Tag(name = "Traceability", description = """
        Where each lot went: the articles sold in a period, the lots of an article with their quantities, and \
        the delivery notes that carry a lot. A line carries up to three lot codes and all three count the same. \
        Periods are inclusive whole days, over the date of the delivery notes.""")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @Operation(summary = "Get the articles sold in a period", description = """
            Articles with at least one line in a delivery note dated within the period, both days inclusive. \
            Ordered by article code.""")
    @GetMapping("/articles")
    public List<TraceabilityArticleResponse> getArticles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-01") LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-31") LocalDate dateTo) {
        return traceabilityService.getArticles(dateFrom, dateTo);
    }

    @Operation(summary = "Get the lots of an article in a period", description = """
            Lots of the article with the quantity delivered of each in the period, both days inclusive. All three \
            code slots of a line count, and a lot repeated in one line counts once for that line. Ordered by code.""")
    @GetMapping("/lots")
    public List<TraceabilityLotResponse> getLots(
            @RequestParam @Positive Integer articleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-01") LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-31") LocalDate dateTo) {
        return traceabilityService.getLots(articleId, dateFrom, dateTo);
    }

    @Operation(summary = "Get the delivery notes that carry a lot of an article", description = """
            Delivery notes dated within the period, both days inclusive, with a line of the article that carries \
            the code in any of its three slots. The quantity is the sum of those lines. Ordered by date and code.""")
    @GetMapping("/deliveries")
    public List<TraceabilityDeliveryResponse> getDeliveries(
            @RequestParam @Positive Integer articleId,
            @RequestParam @Pattern(regexp = DeliveryNoteLineRequest.TRACEABILITY_SEARCH_PATTERN,
                    message = DeliveryNoteLineRequest.TRACEABILITY_SEARCH_MESSAGE) @Schema(example = "01210426") String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-01") LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-31") LocalDate dateTo) {
        return traceabilityService.getDeliveries(articleId, code, dateFrom, dateTo);
    }
}
