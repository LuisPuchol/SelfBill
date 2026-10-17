package com.luispuchol.selfbill.selfbill_api.catalog.article;

import org.hibernate.annotations.SQLDelete;

import com.luispuchol.selfbill.selfbill_api.shared.SearchText;
import com.luispuchol.selfbill.selfbill_api.shared.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// No @SQLRestriction: documents keep loading deleted rows (CONCEPTO.md §7)
@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE articles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Article extends SoftDeletableEntity {

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchText = SearchText.normalize(name);
    }
}
