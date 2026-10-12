package com.luispuchol.selfbill.selfbill_api.catalog.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WITH_VAT applies the global VAT rate. WITHOUT_VAT applies 0% and invoices the client in the WITHOUT_VAT series")
public enum VatType {
    WITH_VAT,
    WITHOUT_VAT
}
