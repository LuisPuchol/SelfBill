package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeliveryNoteLineRequest(
        @NotNull(message = "Article ID cannot be null")
        @Positive(message = "Article ID must be positive")
        Integer articleId,

        @NotNull(message = "Trazability code 1 cannot be null")
        Integer trazabilityCode1,
        Integer trazabilityCode2,
        Integer trazabilityCode3,

        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @NotNull(message = "Price cannot be null")
        BigDecimal price) {
}
