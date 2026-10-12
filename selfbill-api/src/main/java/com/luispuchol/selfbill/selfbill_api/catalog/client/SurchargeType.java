package com.luispuchol.selfbill.selfbill_api.catalog.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether the client pays the equivalence surcharge (recargo de equivalencia) at the global rate")
public enum SurchargeType {
    WITH_SURCHARGE,
    WITHOUT_SURCHARGE
}
