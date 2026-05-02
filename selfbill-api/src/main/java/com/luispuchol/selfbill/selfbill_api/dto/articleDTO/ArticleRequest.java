package com.luispuchol.selfbill.selfbill_api.dto.articleDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequest {
    @NotNull(message = "Code cannot be null")
    @Positive(message = "Code must be positive")
    private Integer code;

    @NotBlank(message = "Name cannot be blank")
    private String name;
}