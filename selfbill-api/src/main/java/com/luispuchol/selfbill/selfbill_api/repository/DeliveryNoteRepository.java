package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNoteArticles;

@Repository
public interface DeliveryNoteRepository
        extends JpaRepository<DeliveryNote, Integer>, JpaSpecificationExecutor<DeliveryNote> {
    Optional<DeliveryNote> findByCode(Integer code);

    @Query("SELECT dna FROM DeliveryNoteArticles dna " +
            "WHERE dna.trazabilityCode1 = :code " +
            "OR dna.trazabilityCode2 = :code " +
            "OR dna.trazabilityCode3 = :code")
    List<DeliveryNoteArticles> findByTrazabilityCode(@Param("code") Integer code);
}
