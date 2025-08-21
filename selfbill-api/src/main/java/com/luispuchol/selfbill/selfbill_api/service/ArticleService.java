package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    public ResponseEntity<Article> getArticleById(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        return article != null ? ResponseEntity.ok(article) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<Article> getArticleByName(String name) {
        Article article = articleRepository.findByNameIgnoreCase(name).orElse(null);
        return article != null ? ResponseEntity.ok(article) : ResponseEntity.notFound().build();
    }

    public ResponseEntity<Article> createOrUpdateArticle(Article article) {
        Optional<Article> existing = articleRepository.findByNameIgnoreCase(article.getName());

        if (existing.isPresent() && !existing.get().getId().equals(article.getId())) {
            Article existingArticle = existing.get();
            existingArticle.setId(article.getId());
            existingArticle.setName(article.getName());
            return ResponseEntity.ok(articleRepository.save(existingArticle));
        } else {
            return ResponseEntity.ok(articleRepository.save(article));
        }
    }

    public ResponseEntity<Void> deleteArticle(Long id) {
        if (articleRepository.existsById(id)) {
            articleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
