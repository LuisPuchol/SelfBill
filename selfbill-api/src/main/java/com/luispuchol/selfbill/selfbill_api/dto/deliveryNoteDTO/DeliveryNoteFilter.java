package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DeliveryNoteFilter {
    private Integer codeFrom;
    private Integer codeTo;
    private Integer clientId;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
