package com.luispuchol.selfbill.selfbill_api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping()
    public List<Article> getAllArticles() {
        return articleService.getAllArticles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<Article> getArticleByName(@PathVariable String name) {
        return articleService.getArticleByName(name);
    }

    @PostMapping
    public ResponseEntity<Article> createArticle(@RequestBody @Valid Article article) {
        return articleService.createOrUpdateArticle(article);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        return articleService.deleteArticle(id);
    }
}
