package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryNoteArticlesResponse {

    private Integer id;
    private Integer deliveryNoteId;

    private Integer articleId;
    private String articleName;

    private Integer trazabilityCode1;
    private Integer trazabilityCode2;
    private Integer trazabilityCode3;

    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal total;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
