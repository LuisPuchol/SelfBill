package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryNoteResponse {

    private Integer id;
    private Integer code;

    private Integer invoiceId;

    private LocalDateTime date;
    private BigDecimal total;

    private Integer clientId;
    private String clientName;

    private List<DeliveryNoteArticlesResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
