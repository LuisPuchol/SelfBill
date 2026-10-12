package com.luispuchol.selfbill.selfbill_api.catalog.article;

import lombok.Builder;

@Builder
public record ArticleFilter(
        String name,
        Integer codeFrom,
        Integer codeTo) {
}
