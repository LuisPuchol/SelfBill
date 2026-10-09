package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;

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
