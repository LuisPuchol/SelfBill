package com.luispuchol.selfbill.selfbill_api.catalog.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ClientFilter(
        @Schema(description = "Part of the trade name, the legal name or the NIF, ignoring case and accents")
        String q,

        @Schema(description = "Part of the trade name, ignoring case")
        String name,

        @Schema(description = "Part of the legal name, ignoring case")
        String businessName,

        @Schema(description = "Lowest code, inclusive")
        Integer codeFrom,

        @Schema(description = "Highest code, inclusive")
        Integer codeTo,

        @Schema(description = "true lists only the deleted ones; absent or false, only the live ones")
        Boolean deleted) {
}
