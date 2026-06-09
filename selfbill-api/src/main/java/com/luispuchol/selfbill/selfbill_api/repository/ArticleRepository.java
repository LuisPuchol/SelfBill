package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.luispuchol.selfbill.selfbill_api.entity.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer>, JpaSpecificationExecutor<Article> {
    Optional<Article> findByNameIgnoreCase(String name);

    Optional<Article> findByCode(Integer code);
}