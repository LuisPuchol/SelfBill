package com.luispuchol.selfbill.selfbill_api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.dto.NextCodeResponse;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "Article management endpoints")
@Validated
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "Get all articles", description = "Returns paginated and filtered list of non-deleted articles")
    @GetMapping
    public Page<ArticleResponse> getAllArticles(@ParameterObject ArticleFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return articleService.getAllArticles(filter, pageable);
    }

    @Operation(summary = "Suggest the next free code")
    @GetMapping("/next-code")
    public NextCodeResponse getNextCode() {
        return articleService.getNextCode();
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticleById(@PathVariable @Positive Integer id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/by-code/{code}")
    public ArticleResponse getArticleByCode(@PathVariable @Positive Integer code) {
        return articleService.getArticleByCode(code);
    }

    @GetMapping("/by-name/{name}")
    public ArticleResponse getArticleByName(@PathVariable @NotBlank String name) {
        return articleService.getArticleByName(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(@RequestBody @Valid ArticleRequest request) {
        return articleService.createArticle(request);
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(@PathVariable @Positive Integer id,
            @RequestBody @Valid ArticleRequest request) {
        return articleService.updateArticle(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable @Positive Integer id) {
        articleService.deleteArticle(id);
    }
}
