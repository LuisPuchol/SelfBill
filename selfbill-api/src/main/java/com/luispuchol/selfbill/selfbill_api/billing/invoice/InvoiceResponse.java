package com.luispuchol.selfbill.selfbill_api.billing.invoice;

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
public class InvoiceResponse {

    private Integer id;
    private InvoiceSeries series;
    private Integer code;
    private String number;
    private LocalDateTime date;
    private InvoiceStatus status;

    private Integer clientId;
    private String clientName;

    private List<InvoiceSectionResponse> sections;

    private BigDecimal subtotal;
    private BigDecimal vatPercentage;
    private BigDecimal vatAmount;
    private BigDecimal surchargePercentage;
    private BigDecimal surchargeAmount;
    private BigDecimal total;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
