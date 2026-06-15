package com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO;

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
public class InvoiceSectionResponse {

    private Integer deliveryNoteId;
    private Integer deliveryNoteCode;
    private LocalDateTime deliveryNoteDate;

    private List<InvoiceLineResponse> lines;
}
