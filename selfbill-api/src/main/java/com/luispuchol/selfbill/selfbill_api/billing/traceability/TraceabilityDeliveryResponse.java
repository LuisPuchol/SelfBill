package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "A delivery note that carries a lot of an article")
@Builder
public record TraceabilityDeliveryResponse(
        Integer deliveryNoteId,
        Integer deliveryNoteCode,
        LocalDateTime date,
        Integer clientId,
        Integer clientCode,

        @Schema(description = "Trade name of the client")
        String clientName,

        @Schema(description = "Quantity of the article with that lot in this delivery note, all its lines added")
        BigDecimal quantity) {
}
