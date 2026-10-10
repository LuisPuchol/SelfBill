package com.luispuchol.selfbill.selfbill_api.catalog.article;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ArticleResponse(
        Integer id,

        Integer code,

        String name,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
