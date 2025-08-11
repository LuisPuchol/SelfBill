package com.luispuchol.selfbill.selfbill_api.repository;

import com.luispuchol.selfbill.selfbill_api.entity.Article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // Additional query methods can be defined here if needed

}
