package com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {

    // PER_DELIVERY_NOTE: exactly one ID; GROUPED: one or more IDs from the same client
    @NotEmpty(message = "At least one delivery note ID is required")
    private List<@NotNull @Positive Integer> deliveryNoteIds;
}
