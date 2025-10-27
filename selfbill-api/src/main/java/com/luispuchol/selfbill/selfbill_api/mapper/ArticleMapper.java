package com.luispuchol.selfbill.selfbill_api.mapper;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;

@Component
public class ArticleMapper {

    public ArticleResponse toResponse(Article article) {
        if (article == null) {
            return null;
        }
        return new ArticleResponse(
                article.getId(),
                article.getCode(),
                article.getName(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    public Article toEntity(ArticleRequest request) {
        if (request == null) {
            return null;
        }
        return new Article(
                request.getCode(),
                request.getName());
    }

    public void updateEntity(Article article, ArticleRequest request) {
        if (article != null && request != null) {
            article.setCode(request.getCode());
            article.setName(request.getName());
        }
    }
}