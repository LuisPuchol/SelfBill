package com.luispuchol.selfbill.selfbill_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.articleDTO.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "Article management endpoints")
@Validated
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "Get all articles", description = "Returns list of all non-deleted articles")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping()
    public ResponseEntity<List<ArticleResponse>> getAllArticles() {
        return ResponseEntity.ok(articleService.getAllArticles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticleById(
            @PathVariable @NotNull @Positive Integer id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<ArticleResponse> getArticleByCode(
            @PathVariable @NotNull @Positive Integer code) {
        return ResponseEntity.ok(articleService.getArticleByCode(code));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ArticleResponse> getArticleByName(
            @PathVariable @NotBlank String name) {
        return ResponseEntity.ok(articleService.getArticleByName(name));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @RequestBody @Valid ArticleRequest articleRequest) {
        ArticleResponse created = articleService.createArticle(articleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable @NotNull @Positive Integer id,
            @RequestBody @Valid ArticleRequest articleRequest) {
        ArticleResponse updated = articleService.updateArticle(id, articleRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable @NotNull @Positive Integer id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}