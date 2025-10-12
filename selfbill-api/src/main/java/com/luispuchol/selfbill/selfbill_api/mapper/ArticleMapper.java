package com.luispuchol.selfbill.selfbill_api.mapper;

import org.springframework.stereotype.Component;
import com.luispuchol.selfbill.selfbill_api.dto.ArticleDTO;
import com.luispuchol.selfbill.selfbill_api.entity.Article;

@Component
public class ArticleMapper {

    public ArticleDTO toDTO(Article article) {
        if (article == null) {
            return null;
        }
        return new ArticleDTO(
                article.getId(),
                article.getCode(),
                article.getName(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                article.getDeletedAt());
    }

    public Article toEntity(ArticleDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Article(
                dto.getCode(),
                dto.getName());
    }
}