package com.luispuchol.selfbill.selfbill_api.dto.deliverynote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;

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

    private DeliveryNoteFormat format;
    private BigDecimal vatPercentage;
    private BigDecimal surchargePercentage;

    private Integer clientId;
    private String clientName;

    private List<DeliveryNoteLineResponse> lines;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
