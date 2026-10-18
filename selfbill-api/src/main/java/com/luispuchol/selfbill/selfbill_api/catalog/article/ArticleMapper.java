package com.luispuchol.selfbill.selfbill_api.catalog.article;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ArticleMapper {

    ArticleResponse toResponse(Article article);

    @Mapping(target = "searchText", ignore = true)
    Article toEntity(ArticleRequest request);

    @Mapping(target = "searchText", ignore = true)
    void updateEntity(@MappingTarget Article article, ArticleRequest request);
}
