package com.luispuchol.selfbill.selfbill_api.catalog.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ClientRequest(
        @NotNull(message = "Code cannot be null")
        @Positive(message = "Code must be positive")
        Integer code,

        @NotBlank(message = "Business name cannot be blank")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @NotBlank(message = "Name cannot be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 20, message = "NIF must not exceed 20 characters")
        String nif,

        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
        String email,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "Province must not exceed 100 characters")
        String province,

        @Pattern(regexp = "^\\d{5}$", message = "Postal code must be 5 digits")
        String postalCode,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 20, message = "Phone2 must not exceed 20 characters")
        String phone2,

        @NotNull(message = "VAT type cannot be null")
        VatType vatType,

        @NotNull(message = "Surcharge type cannot be null")
        SurchargeType surchargeType,

        @NotNull(message = "Default delivery note format cannot be null")
        DeliveryNoteFormat defaultDeliveryNoteFormat) {
}
