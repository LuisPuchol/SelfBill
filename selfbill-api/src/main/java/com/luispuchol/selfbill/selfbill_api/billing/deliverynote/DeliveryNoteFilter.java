package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDate;

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

        @Schema(description = "Earliest date, inclusive", example = "2026-10-01")
        LocalDate dateFrom,

        @Schema(description = "Latest date, inclusive", example = "2026-10-31")
        LocalDate dateTo,

        @Schema(description = "Text searched in the client's trade name, business name and NIF, ignoring case and accents",
                example = "garcia")
        String q,

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
