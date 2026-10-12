package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record DeliveryNoteRequest(
        @Schema(example = "2026-10-12T08:00:00")
        @NotNull(message = "Date cannot be null")
        LocalDateTime date,

        @Schema(description = "On create, null takes the next free code. On update, null keeps the current one",
                example = "1050")
        @Positive(message = "Code must be positive")
        Integer code,

        @Schema(example = "7")
        @NotNull(message = "Client ID cannot be null")
        @Positive(message = "Client ID must be positive")
        Integer clientId,

        @Schema(description = "Replaces all the lines of the delivery note")
        @Valid
        List<DeliveryNoteLineRequest> lines) {
}
