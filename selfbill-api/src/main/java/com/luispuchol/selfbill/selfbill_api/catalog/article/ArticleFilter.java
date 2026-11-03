package com.luispuchol.selfbill.selfbill_api.catalog.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ArticleFilter(
        @Schema(description = "Part of the name, ignoring case and accents")
        String name,

        @Schema(description = "Lowest code, inclusive")
        Integer codeFrom,

        @Schema(description = "Highest code, inclusive")
        Integer codeTo,

        @Schema(description = "true lists only the deleted ones; absent or false, only the live ones")
        Boolean deleted) {
}
