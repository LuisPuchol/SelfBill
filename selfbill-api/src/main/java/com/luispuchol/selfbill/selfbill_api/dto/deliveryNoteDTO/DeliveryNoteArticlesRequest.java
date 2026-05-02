package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.math.BigDecimal;

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
public class DeliveryNoteArticlesRequest {

    private Integer id;

    @NotNull(message = "Article ID cannot be null")
    @Positive(message = "Article ID must be positive")
    private Integer articleId;

    @NotNull(message = "Trazability code 1 cannot be null")
    private Integer trazabilityCode1;
    private Integer trazabilityCode2;
    private Integer trazabilityCode3;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private BigDecimal price;
    private BigDecimal total;
}
