package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;

public interface IArticleService {
    Page<ArticleResponse> getAllArticles(ArticleFilter filter, Pageable pageable);

    ArticleResponse getArticleById(Integer id);

    ArticleResponse getArticleByCode(Integer code);

    ArticleResponse getArticleByName(String name);

    ArticleResponse createArticle(ArticleRequest articleRequest);

    ArticleResponse updateArticle(Integer id, ArticleRequest articleRequest);

    void deleteArticle(Integer id);
}
