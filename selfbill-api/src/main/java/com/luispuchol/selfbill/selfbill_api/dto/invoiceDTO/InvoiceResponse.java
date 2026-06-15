package com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO;

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
    private Integer code;
    private LocalDateTime date;
    private String status;

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
