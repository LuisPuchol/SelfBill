package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;

@Repository
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Integer>, JpaSpecificationExecutor<DeliveryNote> {
    Optional<DeliveryNote> findByCode(Integer code);
}
