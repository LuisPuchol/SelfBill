package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TaxConfigRequest(
        @Schema(description = "First day these rates apply to", example = "2026-01-01")
        @NotNull
        LocalDate validFrom,

        @Schema(description = "VAT rate, as a percentage", example = "4")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal vatPercentage,

        @Schema(description = "Equivalence surcharge rate, as a percentage", example = "0.5")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal surchargePercentage) {
}
