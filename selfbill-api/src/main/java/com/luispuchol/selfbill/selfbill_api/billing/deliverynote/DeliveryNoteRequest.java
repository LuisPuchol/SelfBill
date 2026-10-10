package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeliveryNoteRequest(
        @NotNull(message = "Date cannot be null")
        LocalDateTime date,

        @Positive(message = "Code must be positive")
        Integer code,

        @NotNull(message = "Client ID cannot be null")
        @Positive(message = "Client ID must be positive")
        Integer clientId,

        @Valid
        List<DeliveryNoteLineRequest> lines) {
}
