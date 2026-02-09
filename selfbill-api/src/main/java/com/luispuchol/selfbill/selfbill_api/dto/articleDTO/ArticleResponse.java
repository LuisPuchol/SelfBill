package com.luispuchol.selfbill.selfbill_api.dto.articleDTO;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Integer id;

    private Integer code;

    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}