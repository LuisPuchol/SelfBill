package com.luispuchol.selfbill.selfbill_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleFilter;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleRequest;
import com.luispuchol.selfbill.selfbill_api.dto.article.ArticleResponse;
import com.luispuchol.selfbill.selfbill_api.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
@DisplayName("Article Controller Tests")
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticleService articleService;

    private ArticleResponse articleResponse1;
    private ArticleResponse articleResponse2;
    private ArticleRequest articleRequest;

    @BeforeEach
    void setUp() {
        articleResponse1 = ArticleResponse.builder()
                .id(1)
                .code(1001)
                .name("Laptop Dell XPS")
                .build();

        articleResponse2 = ArticleResponse.builder()
                .id(2)
                .code(1002)
                .name("Mouse Logitech")
                .build();

        articleRequest = ArticleRequest.builder()
                .code(1003)
                .name("Keyboard Mechanical")
                .build();
    }

    @Test
    @DisplayName("GET /api/articles - Debe retornar todos los artículos paginados")
    void getAllArticles_ShouldReturnPageOfArticles() throws Exception {
        List<ArticleResponse> articles = Arrays.asList(articleResponse1, articleResponse2);
        Page<ArticleResponse> page = new PageImpl<>(articles);
        when(articleService.getAllArticles(any(ArticleFilter.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Laptop Dell XPS")))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].name", is("Mouse Logitech")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        verify(articleService, times(1)).getAllArticles(any(ArticleFilter.class), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Debe retornar un artículo por ID")
    void getArticleById_ShouldReturnArticle() throws Exception {
        when(articleService.getArticleById(1)).thenReturn(articleResponse1);

        mockMvc.perform(get("/api/articles/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.code", is(1001)))
                .andExpect(jsonPath("$.name", is("Laptop Dell XPS")));

        verify(articleService, times(1)).getArticleById(1);
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Debe fallar con ID inválido (0)")
    void getArticleById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/articles/0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(articleService, never()).getArticleById(any());
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Debe fallar con ID negativo")
    void getArticleById_WithNegativeId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/articles/-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(articleService, never()).getArticleById(any());
    }

    @Test
    @DisplayName("GET /api/articles/by-code/{code} - Debe retornar un artículo por código")
    void getArticleByCode_ShouldReturnArticle() throws Exception {
        when(articleService.getArticleByCode(1001)).thenReturn(articleResponse1);

        mockMvc.perform(get("/api/articles/by-code/1001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1001)))
                .andExpect(jsonPath("$.name", is("Laptop Dell XPS")));

        verify(articleService, times(1)).getArticleByCode(1001);
    }

    @Test
    @DisplayName("GET /api/articles/by-name/{name} - Debe retornar un artículo por nombre")
    void getArticleByName_ShouldReturnArticle() throws Exception {
        when(articleService.getArticleByName("Laptop Dell XPS")).thenReturn(articleResponse1);

        mockMvc.perform(get("/api/articles/by-name/Laptop Dell XPS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Laptop Dell XPS")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(articleService, times(1)).getArticleByName("Laptop Dell XPS");
    }

    @Test
    @DisplayName("POST /api/articles - Debe crear un nuevo artículo")
    void createArticle_ShouldReturnCreatedArticle() throws Exception {
        ArticleResponse createdResponse = ArticleResponse.builder()
                .id(3)
                .code(1003)
                .name("Keyboard Mechanical")
                .build();

        when(articleService.createArticle(any(ArticleRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.code", is(1003)))
                .andExpect(jsonPath("$.name", is("Keyboard Mechanical")));

        verify(articleService, times(1)).createArticle(any(ArticleRequest.class));
    }

    @Test
    @DisplayName("POST /api/articles - Debe fallar con body inválido (sin campos requeridos)")
    void createArticle_WithInvalidBody_ShouldReturnBadRequest() throws Exception {
        ArticleRequest invalidRequest = ArticleRequest.builder().build();

        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(articleService, never()).createArticle(any());
    }

    @Test
    @DisplayName("PUT /api/articles/{id} - Debe actualizar un artículo existente")
    void updateArticle_ShouldReturnUpdatedArticle() throws Exception {
        ArticleResponse updatedResponse = ArticleResponse.builder()
                .id(1)
                .code(1003)
                .name("Keyboard Mechanical")
                .build();

        when(articleService.updateArticle(eq(1), any(ArticleRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/articles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Keyboard Mechanical")));

        verify(articleService, times(1)).updateArticle(eq(1), any(ArticleRequest.class));
    }

    @Test
    @DisplayName("PUT /api/articles/{id} - Debe fallar con ID inválido")
    void updateArticle_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/articles/0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(articleRequest)))
                .andExpect(status().isBadRequest());

        verify(articleService, never()).updateArticle(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/articles/{id} - Debe eliminar un artículo")
    void deleteArticle_ShouldReturnNoContent() throws Exception {
        doNothing().when(articleService).deleteArticle(1);

        mockMvc.perform(delete("/api/articles/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(articleService, times(1)).deleteArticle(1);
    }

    @Test
    @DisplayName("DELETE /api/articles/{id} - Debe fallar con ID inválido")
    void deleteArticle_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/articles/-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(articleService, never()).deleteArticle(any());
    }

    @Test
    @DisplayName("POST /api/articles - Debe responder 400 ante un JSON mal formado")
    void createArticle_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verify(articleService, never()).createArticle(any());
    }

    @Test
    @DisplayName("POST /api/articles - Debe devolver los errores de validación como ProblemDetail")
    void createArticle_WithInvalidBody_ShouldReturnProblemDetail() throws Exception {
        mockMvc.perform(post("/api/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors.code", is("Code cannot be null")));
    }
}