package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "A lot of an article and the quantity delivered of it in the period")
@Builder
public record TraceabilityLotResponse(
        @Schema(example = "01210426")
        String code,

        @Schema(description = "Sum of the lines that carry the lot in any slot, a line counted once")
        BigDecimal quantity) {
}
