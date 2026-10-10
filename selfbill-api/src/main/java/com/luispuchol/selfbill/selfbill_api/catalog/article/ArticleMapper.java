package com.luispuchol.selfbill.selfbill_api.catalog.article;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ArticleMapper {

    ArticleResponse toResponse(Article article);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Article toEntity(ArticleRequest request);

    @InheritConfiguration
    void updateEntity(@MappingTarget Article article, ArticleRequest request);
}
