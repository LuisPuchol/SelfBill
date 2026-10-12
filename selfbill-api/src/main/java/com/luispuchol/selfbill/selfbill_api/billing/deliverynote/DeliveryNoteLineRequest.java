package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeliveryNoteLineRequest(
        @Schema(example = "3")
        @NotNull(message = "Article ID cannot be null")
        @Positive(message = "Article ID must be positive")
        Integer articleId,

        @Schema(description = "Lot code: 2 digits of supplier and 6 of date", example = "01210426")
        @NotNull(message = "Traceability code 1 cannot be null")
        @Pattern(regexp = TRACEABILITY_CODE_PATTERN, message = TRACEABILITY_CODE_MESSAGE)
        String traceabilityCode1,

        @Schema(description = "Optional second lot code")
        @Pattern(regexp = TRACEABILITY_CODE_PATTERN, message = TRACEABILITY_CODE_MESSAGE)
        String traceabilityCode2,

        @Schema(description = "Optional third lot code")
        @Pattern(regexp = TRACEABILITY_CODE_PATTERN, message = TRACEABILITY_CODE_MESSAGE)
        String traceabilityCode3,

        @Schema(example = "12.5")
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @Schema(description = "Unit price of this delivery: articles have no price", example = "1.35")
        @NotNull(message = "Price cannot be null")
        BigDecimal price) {

    static final String TRACEABILITY_CODE_PATTERN = "\\d{8}";
    static final String TRACEABILITY_CODE_MESSAGE = "Traceability code must be 8 digits";
}
