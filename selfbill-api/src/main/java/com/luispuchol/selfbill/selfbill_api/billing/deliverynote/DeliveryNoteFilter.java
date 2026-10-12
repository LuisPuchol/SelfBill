package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record DeliveryNoteFilter(
        @Schema(description = "Lowest code, inclusive")
        Integer codeFrom,

        @Schema(description = "Highest code, inclusive")
        Integer codeTo,

        Integer clientId,

        @Schema(description = "Earliest date, inclusive", example = "2026-10-01T00:00:00")
        LocalDateTime dateFrom,

        @Schema(description = "Latest date, inclusive", example = "2026-10-31T23:59:59")
        LocalDateTime dateTo,

        @Schema(description = "true: only delivery notes in an invoice. false: only pending ones")
        Boolean invoiced,

        @Schema(description = "Only delivery notes with a line of this article")
        Integer articleId,

        @Schema(description = """
                Only delivery notes with a line that carries this traceability code in any of its three slots. \
                With articleId, both must be on the same line""", example = "01210426")
        @Pattern(regexp = DeliveryNoteLineRequest.TRACEABILITY_CODE_PATTERN,
                message = DeliveryNoteLineRequest.TRACEABILITY_CODE_MESSAGE)
        String traceabilityCode) {
}
