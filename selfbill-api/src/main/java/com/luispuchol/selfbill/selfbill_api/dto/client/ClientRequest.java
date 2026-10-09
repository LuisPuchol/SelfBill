package com.luispuchol.selfbill.selfbill_api.dto.client;

import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;

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

    @NotBlank(message = "Business name cannot be blank")
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 20, message = "NIF must not exceed 20 characters")
    private String nif;

    @Size(max = 50, message = "Email must not exceed 50 characters")
    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Pattern(regexp = "^\\d{5}$", message = "Postal code must be 5 digits")
    private String postalCode;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 20, message = "Phone2 must not exceed 20 characters")
    private String phone2;

    @NotNull(message = "VAT type cannot be null")
    private VatType vatType;

    @NotNull(message = "Surcharge type cannot be null")
    private SurchargeType surchargeType;

    @NotNull(message = "Default delivery note format cannot be null")
    private DeliveryNoteFormat defaultDeliveryNoteFormat;
}
