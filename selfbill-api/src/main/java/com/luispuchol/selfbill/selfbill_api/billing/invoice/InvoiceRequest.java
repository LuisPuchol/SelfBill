package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record InvoiceRequest(
        @Schema(example = "7")
        @NotNull(message = "Client ID cannot be null")
        @Positive(message = "Client ID must be positive")
        Integer clientId,

        @Schema(description = "Delivery notes of the client that make up the invoice. On update, replaces the current set",
                example = "[12, 15]")
        @NotEmpty(message = "At least one delivery note ID is required")
        List<@NotNull @Positive Integer> deliveryNoteIds) {
}
