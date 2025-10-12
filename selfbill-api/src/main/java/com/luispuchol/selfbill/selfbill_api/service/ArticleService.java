package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.luispuchol.selfbill.selfbill_api.dto.ArticleDTO;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.mapper.ArticleMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    public List<ArticleDTO> getAllArticles() {
        return articleRepository.findAll().stream()
                .filter(article -> article.getDeletedAt() == null)
                .map(articleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ArticleDTO getArticleById(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found: " + id));

        // article soft delete check?

        return articleMapper.toDTO(article);
    }

    public ArticleDTO getArticleByName(String name) {
        Article article = articleRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new BusinessException("Article not found" + name));

        // article soft delete check?

        return articleMapper.toDTO(article);
    }

    public ArticleDTO createArticle(ArticleDTO articleDTO) {
        Optional<Article> existingByCode = articleRepository.findByCode(articleDTO.getCode());
        if (existingByCode.isPresent() && existingByCode.get().getDeletedAt() == null) {
            throw new BusinessException("Already exists article with code: " + articleDTO.getCode());
        }

        Optional<Article> existingByName = articleRepository.findByNameIgnoreCase(articleDTO.getName());
        if (existingByName.isPresent() && existingByName.get().getDeletedAt() == null) {
            throw new BusinessException("Already exists article with name: " + articleDTO.getName());
        }

        Article article = articleMapper.toEntity(articleDTO);
        Article saved = articleRepository.save(article);
        return articleMapper.toDTO(saved);
    }

    public ArticleDTO updateArticle(Integer id, ArticleDTO articleDTO) {
        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found with ID: " + id));

        if (existingArticle.getDeletedAt() != null) { // 🆕
            throw new BusinessException("Cannot update deleted article");
        }

        Optional<Article> articleWithSameCode = articleRepository.findByCode(articleDTO.getCode());
        if (articleWithSameCode.isPresent() &&
                articleWithSameCode.get().getId() != existingArticle.getId() &&
                articleWithSameCode.get().getDeletedAt() == null) {
            throw new BusinessException("Already exists another article with code: " + articleDTO.getCode());
        }

        Optional<Article> articleWithSameName = articleRepository.findByNameIgnoreCase(articleDTO.getName());
        if (articleWithSameName.isPresent() &&
                articleWithSameName.get().getId() != existingArticle.getId() &&
                articleWithSameName.get().getDeletedAt() == null) {
            throw new BusinessException("Already exists another article with name: " + articleDTO.getName());
        }

        existingArticle.setCode(articleDTO.getCode());
        existingArticle.setName(articleDTO.getName());

        Article saved = articleRepository.save(existingArticle);
        return articleMapper.toDTO(saved);
    }

    public void deleteArticle(Integer id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Article not found with ID: " + id));

        if (article.getDeletedAt() != null) {
            throw new BusinessException("Article already deleted");
        }

        article.softDelete();
        articleRepository.save(article);
    }
}
