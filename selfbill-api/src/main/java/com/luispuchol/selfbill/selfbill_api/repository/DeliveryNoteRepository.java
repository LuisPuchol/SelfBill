package com.luispuchol.selfbill.selfbill_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;

public interface DeliveryNoteRepository
        extends JpaRepository<DeliveryNote, Integer>, JpaSpecificationExecutor<DeliveryNote> {

    boolean existsByCode(Integer code);
}
