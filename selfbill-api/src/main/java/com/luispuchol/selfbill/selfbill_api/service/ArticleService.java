package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.ArticleMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    @Transactional
    @Override
    public List<ArticleResponse> getAllArticles() {
        return articleRepository.findAll().stream()
                .map(articleMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public ArticleResponse getArticleById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, id));
        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse getArticleByCode(Integer code) {
        Article article = articleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, code));
        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse getArticleByName(String name) {
        Article article = articleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, name));
        return articleMapper.toResponse(article);
    }

    @Transactional
    @Override
    public ArticleResponse createArticle(ArticleRequest articleRequest) {
        Optional<Article> existingByCode = articleRepository.findByCode(articleRequest.getCode());
        if (existingByCode.isPresent()) {
            throw new BusinessException(ErrorCode.ARTICLE_DUPLICATE_CODE, articleRequest.getCode());
        }

        Article article = articleMapper.toEntity(articleRequest);
        Article saved = articleRepository.save(article);
        return articleMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public ArticleResponse updateArticle(Integer id, ArticleRequest articleRequest) {
        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, id));

        if (!existingArticle.getCode().equals(articleRequest.getCode())) {
            Optional<Article> articleWithSameCode = articleRepository.findByCode(articleRequest.getCode());
            if (articleWithSameCode.isPresent()) {
                throw new BusinessException(ErrorCode.ARTICLE_DUPLICATE_CODE, articleRequest.getCode());
            }
        }

        articleMapper.updateEntity(existingArticle, articleRequest);
        Article saved = articleRepository.save(existingArticle);
        return articleMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public void deleteArticle(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, id));

        articleRepository.delete(article);
    }
}
