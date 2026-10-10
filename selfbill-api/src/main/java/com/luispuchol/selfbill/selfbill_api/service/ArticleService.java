package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.NextCodeResponse;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.ArticleMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.specification.ArticleSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    public Page<ArticleResponse> getAllArticles(ArticleFilter filter, Pageable pageable) {
        return articleRepository.findAll(ArticleSpecification.withFilter(filter), pageable)
                .map(articleMapper::toResponse);
    }

    public ArticleResponse getArticleById(Integer id) {
        return articleMapper.toResponse(findArticle(id));
    }

    public ArticleResponse getArticleByCode(Integer code) {
        return articleRepository.findByCodeAndDeletedAtIsNull(code)
                .map(articleMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, code));
    }

    public ArticleResponse getArticleByName(String name) {
        return articleRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                .map(articleMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, name));
    }

    @Transactional
    public ArticleResponse createArticle(ArticleRequest request) {
        requireUnusedCode(request.getCode());
        return articleMapper.toResponse(articleRepository.save(articleMapper.toEntity(request)));
    }

    @Transactional
    public ArticleResponse updateArticle(Integer id, ArticleRequest request) {
        Article article = findArticle(id);
        if (!article.getCode().equals(request.getCode())) {
            requireUnusedCode(request.getCode());
        }
        articleMapper.updateEntity(article, request);
        return articleMapper.toResponse(articleRepository.save(article));
    }

    public NextCodeResponse getNextCode() {
        return new NextCodeResponse(articleRepository.findNextCode());
    }

    @Transactional
    public void deleteArticle(Integer id) {
        articleRepository.delete(findArticle(id));
    }

    private Article findArticle(Integer id) {
        return articleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, id));
    }

    private void requireUnusedCode(Integer code) {
        if (articleRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException(ErrorCode.ARTICLE_DUPLICATE_CODE, code);
        }
    }
}
