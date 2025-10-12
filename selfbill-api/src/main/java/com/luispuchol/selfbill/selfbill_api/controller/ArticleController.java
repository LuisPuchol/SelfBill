package com.luispuchol.selfbill.selfbill_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import com.luispuchol.selfbill.selfbill_api.dto.ArticleDTO;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/articles")
@Validated
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping()
    public ResponseEntity<List<ArticleDTO>> getAllArticles() {
        return ResponseEntity.ok(articleService.getAllArticles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDTO> getArticleById(@PathVariable @NotNull @Positive Integer id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ArticleDTO> getArticleByName(@PathVariable @NotBlank String name) {
        return ResponseEntity.ok(articleService.getArticleByName(name));
    }

    @PostMapping
    public ResponseEntity<ArticleDTO> createArticle(@RequestBody @Valid ArticleDTO articleDTO) {
        ArticleDTO created = articleService.createArticle(articleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleDTO> updateArticle(
            @PathVariable @NotNull @Positive Integer id,
            @RequestBody @Valid ArticleDTO articleDTO) {
        return ResponseEntity.ok(articleService.updateArticle(id, articleDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable @NotNull @Positive Integer id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
