package com.luispuchol.selfbill.selfbill_api.dto.clientDTO;

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

    // Business and personal details
    private String businessName;
    private String name;
    private String nif;

    // Contact details
    private String email;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String phone;
    private String phone2;

    // Client options
    private String vatType;
    private String surchargeType;
    private String invoiceMode;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}