package com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxConfigRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal vatPercentage;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal surchargePercentage;
}
