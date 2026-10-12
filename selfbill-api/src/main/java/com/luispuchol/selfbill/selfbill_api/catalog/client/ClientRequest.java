package com.luispuchol.selfbill.selfbill_api.catalog.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ClientRequest(
        @Schema(description = "Unique among non-deleted clients", example = "7")
        @NotNull(message = "Code cannot be null")
        @Positive(message = "Code must be positive")
        Integer code,

        @Schema(description = "Legal name, printed on invoices", example = "Frutas García S.L.")
        @NotBlank(message = "Business name cannot be blank")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @Schema(description = "Trade name, shown in lists and used to address the client in emails",
                example = "Frutería García")
        @NotBlank(message = "Name cannot be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Schema(example = "B12345678")
        @Size(max = 20, message = "NIF must not exceed 20 characters")
        String nif,

        @Schema(description = "Address delivery notes and invoices are sent to", example = "pedidos@frutasgarcia.es")
        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
        String email,

        @Schema(example = "Calle Mayor 1")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Schema(example = "Valencia")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Schema(example = "Valencia")
        @Size(max = 100, message = "Province must not exceed 100 characters")
        String province,

        @Schema(example = "46001")
        @Pattern(regexp = "^\\d{5}$", message = "Postal code must be 5 digits")
        String postalCode,

        @Schema(example = "961234567")
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
