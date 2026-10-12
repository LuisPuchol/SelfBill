package com.luispuchol.selfbill.selfbill_api.catalog.article;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.ARTICLE_DUPLICATE_CODE;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.ARTICLE_NOT_FOUND;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.catalog.NextCodeResponse;
import com.luispuchol.selfbill.selfbill_api.shared.ApiErrors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "Article catalog. Articles have no price: it is set on each delivery note line")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "Get all articles", description = "Returns paginated and filtered list of non-deleted articles")
    @GetMapping
    public Page<ArticleResponse> getAllArticles(@ParameterObject ArticleFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return articleService.getAllArticles(filter, pageable);
    }

    @Operation(summary = "Suggest the next free code", description = "Highest code among non-deleted articles plus one")
    @GetMapping("/next-code")
    public NextCodeResponse getNextCode() {
        return articleService.getNextCode();
    }

    @Operation(summary = "Get an article by id")
    @ApiErrors(ARTICLE_NOT_FOUND)
    @GetMapping("/{id}")
    public ArticleResponse getArticleById(@PathVariable @Positive Integer id) {
        return articleService.getArticleById(id);
    }

    @Operation(summary = "Get an article by code")
    @ApiErrors(ARTICLE_NOT_FOUND)
    @GetMapping("/by-code/{code}")
    public ArticleResponse getArticleByCode(@PathVariable @Positive Integer code) {
        return articleService.getArticleByCode(code);
    }

    @Operation(summary = "Get an article by name", description = "Exact match, ignoring case")
    @ApiErrors(ARTICLE_NOT_FOUND)
    @GetMapping("/by-name/{name}")
    public ArticleResponse getArticleByName(@PathVariable @NotBlank String name) {
        return articleService.getArticleByName(name);
    }

    @Operation(summary = "Create an article")
    @ApiErrors(ARTICLE_DUPLICATE_CODE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(@RequestBody @Valid ArticleRequest request) {
        return articleService.createArticle(request);
    }

    @Operation(summary = "Update an article", description = "Issued invoices keep the code and name they were issued with")
    @ApiErrors({ ARTICLE_NOT_FOUND, ARTICLE_DUPLICATE_CODE })
    @PutMapping("/{id}")
    public ArticleResponse updateArticle(@PathVariable @Positive Integer id,
            @RequestBody @Valid ArticleRequest request) {
        return articleService.updateArticle(id, request);
    }

    @Operation(summary = "Delete an article", description = """
            Logical deletion: documents that use the article keep showing it, and its code becomes free.
            Pending delivery notes that use it cannot be saved again until it is restored.""")
    @ApiErrors(ARTICLE_NOT_FOUND)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable @Positive Integer id) {
        articleService.deleteArticle(id);
    }
}
