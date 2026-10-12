package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record DeliveryNoteFilter(
        Integer codeFrom,
        Integer codeTo,
        Integer clientId,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        Boolean invoiced) {
}
