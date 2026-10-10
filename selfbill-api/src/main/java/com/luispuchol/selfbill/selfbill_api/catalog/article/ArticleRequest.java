package com.luispuchol.selfbill.selfbill_api.catalog.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record ArticleRequest(
        @NotNull(message = "Code cannot be null")
        @Positive(message = "Code must be positive")
        Integer code,

        @NotBlank(message = "Name cannot be blank")
        String name) {
}
