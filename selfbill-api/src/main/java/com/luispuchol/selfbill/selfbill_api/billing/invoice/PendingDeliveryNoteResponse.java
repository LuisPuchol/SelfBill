package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record PendingDeliveryNoteResponse(
        Integer id,
        Integer code,
        LocalDateTime date,
        BigDecimal total) {
}
