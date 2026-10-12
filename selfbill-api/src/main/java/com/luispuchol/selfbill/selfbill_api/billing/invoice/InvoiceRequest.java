package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record InvoiceRequest(
        @NotNull(message = "Client ID cannot be null")
        @Positive(message = "Client ID must be positive")
        Integer clientId,

        @NotEmpty(message = "At least one delivery note ID is required")
        List<@NotNull @Positive Integer> deliveryNoteIds) {
}
