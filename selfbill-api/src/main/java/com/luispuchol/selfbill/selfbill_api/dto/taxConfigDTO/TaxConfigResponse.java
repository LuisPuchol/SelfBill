package com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaxConfigResponse {

    private Integer id;
    private BigDecimal vatPercentage;
    private BigDecimal surchargePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
