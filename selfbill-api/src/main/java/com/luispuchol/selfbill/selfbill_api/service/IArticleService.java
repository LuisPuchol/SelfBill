package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;

public interface IArticleService {
    List<ArticleResponse> getAllArticles();

    ArticleResponse getArticleById(Integer id);

    ArticleResponse getArticleByCode(Integer code);

    ArticleResponse getArticleByName(String name);

    ArticleResponse createArticle(ArticleRequest articleRequest);

    ArticleResponse updateArticle(Integer id, ArticleRequest articleRequest);

    void deleteArticle(Integer id);
}
