package com.luispuchol.selfbill.selfbill_api.catalog.article;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record ArticleRequest(
        @Schema(description = "Unique among non-deleted articles", example = "101")
        @NotNull(message = "Code cannot be null")
        @Positive(message = "Code must be positive")
        Integer code,

        @Schema(example = "Tomate pera")
        @NotBlank(message = "Name cannot be blank")
        String name) {
}
