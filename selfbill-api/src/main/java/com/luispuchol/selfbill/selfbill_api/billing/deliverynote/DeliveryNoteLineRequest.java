package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeliveryNoteLineRequest(
        @Schema(example = "3")
        @NotNull(message = "Article ID cannot be null")
        @Positive(message = "Article ID must be positive")
        Integer articleId,

        @Schema(description = "Traceability code, such as a lot number", example = "241012")
        @NotNull(message = "Traceability code 1 cannot be null")
        Integer traceabilityCode1,

        @Schema(description = "Optional second traceability code")
        Integer traceabilityCode2,

        @Schema(description = "Optional third traceability code")
        Integer traceabilityCode3,

        @Schema(example = "12.5")
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @Schema(description = "Unit price of this delivery: articles have no price", example = "1.35")
        @NotNull(message = "Price cannot be null")
        BigDecimal price) {
}
