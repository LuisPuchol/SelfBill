package com.luispuchol.selfbill.selfbill_api.catalog.client;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ClientResponse(
        Integer id,
        Integer code,

        String businessName,
        String name,
        String nif,

        String email,
        String address,
        String city,
        String province,
        String postalCode,
        String phone,
        String phone2,

        VatType vatType,
        SurchargeType surchargeType,
        DeliveryNoteFormat defaultDeliveryNoteFormat,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
