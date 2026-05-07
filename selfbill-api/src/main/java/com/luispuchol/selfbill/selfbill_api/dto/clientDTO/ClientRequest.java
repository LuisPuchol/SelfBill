package com.luispuchol.selfbill.selfbill_api.dto.clientDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {

    @NotNull(message = "Code cannot be null")
    @Positive(message = "Code must be positive")
    private Integer code;

    // Business and personal details
    @NotBlank(message = "Business name cannot be blank")
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 20, message = "NIF must not exceed 20 characters")
    private String nif;

    // Contact details
    @Size(max = 50, message = "Email must not exceed 50 characters")
    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    @Pattern(regexp = "^[0-9]{5}$", message = "Postal code must be 5 digits")
    private String postalCode;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 20, message = "Phone2 must not exceed 20 characters")
    private String phone2;

    // Client options
    @NotNull(message = "VAT type cannot be null")
    @Pattern(regexp = "WITH_VAT|WITHOUT_VAT", message = "VAT type must be WITH_VAT or WITHOUT_VAT")
    private String vatType;

    @NotNull(message = "Surcharge type cannot be null")
    @Pattern(regexp = "WITH_SURCHARGE|WITHOUT_SURCHARGE", message = "Surcharge type must be WITH_SURCHARGE or WITHOUT_SURCHARGE")
    private String surchargeType;

    @NotNull(message = "Invoice mode cannot be null")
    @Pattern(regexp = "PER_DELIVERY_NOTE|GROUPED", message = "Invoice mode must be PER_DELIVERY_NOTE or GROUPED")
    private String invoiceMode;

}