package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.mapper.ArticleMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    public List<ArticleResponse> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(articleMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ArticleResponse getArticleById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found: " + id));
        return articleMapper.toResponse(article);
    }

    public ArticleResponse getArticleByCode(Integer code) {
        Article article = articleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Article not found with code: " + code));
        return articleMapper.toResponse(article);
    }

    public ArticleResponse getArticleByName(String name) {
        Article article = articleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new BusinessException("Article not found: " + name));
        return articleMapper.toResponse(article);
    }

    public ArticleResponse createArticle(ArticleRequest articleRequest) {
        Optional<Article> existingByCode = articleRepository.findByCode(articleRequest.getCode());
        if (existingByCode.isPresent()) {
            throw new BusinessException("Already exists article with code: " + articleRequest.getCode());
        }

        Article article = articleMapper.toEntity(articleRequest);
        Article saved = articleRepository.save(article);
        return articleMapper.toResponse(saved);
    }

    public ArticleResponse updateArticle(Integer id, ArticleRequest articleRequest) {
        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found with ID: " + id));

        Optional<Article> articleWithSameCode = articleRepository.findByCode(articleRequest.getCode());
        if (articleWithSameCode.isPresent() &&
                !articleWithSameCode.get().getId().equals(existingArticle.getId())) {
            throw new BusinessException("Already exists another article with code: " + articleRequest.getCode());
        }

        articleMapper.updateEntity(existingArticle, articleRequest);
        Article saved = articleRepository.save(existingArticle);
        return articleMapper.toResponse(saved);
    }

    public void deleteArticle(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found with ID: " + id));

        articleRepository.delete(article);
    }
}
