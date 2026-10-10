package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.luispuchol.selfbill.selfbill_api.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Integer>, JpaSpecificationExecutor<Article> {

    Optional<Article> findByIdAndDeletedAtIsNull(Integer id);

    Optional<Article> findByCodeAndDeletedAtIsNull(Integer code);

    Optional<Article> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    @Query("SELECT COALESCE(MAX(a.code), 0) + 1 FROM Article a WHERE a.deletedAt IS NULL")
    int findNextCode();

    boolean existsByCodeAndDeletedAtIsNull(Integer code);
}
