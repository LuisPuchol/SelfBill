package com.luispuchol.selfbill.selfbill_api.catalog.client;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private Integer id;
    private Integer code;

    private String businessName;
    private String name;
    private String nif;

    private String email;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String phone;
    private String phone2;

    private VatType vatType;
    private SurchargeType surchargeType;
    private DeliveryNoteFormat defaultDeliveryNoteFormat;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
