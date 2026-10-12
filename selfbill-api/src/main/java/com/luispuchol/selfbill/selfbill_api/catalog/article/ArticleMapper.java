package com.luispuchol.selfbill.selfbill_api.catalog.article;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface ArticleMapper {

    ArticleResponse toResponse(Article article);

    Article toEntity(ArticleRequest request);

    void updateEntity(@MappingTarget Article article, ArticleRequest request);
}
