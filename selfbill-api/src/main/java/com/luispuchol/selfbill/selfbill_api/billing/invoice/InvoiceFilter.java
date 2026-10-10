package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InvoiceFilter {
    private InvoiceSeries series;
    private Integer codeFrom;
    private Integer codeTo;
    private Integer clientId;
    private Integer deliveryNoteId;
    private InvoiceStatus status;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
