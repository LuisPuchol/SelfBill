package com.luispuchol.selfbill.selfbill_api.catalog.article;

import lombok.Data;

@Data
public class ArticleFilter {
    private String name;
    private Integer codeFrom;
    private Integer codeTo;
}
