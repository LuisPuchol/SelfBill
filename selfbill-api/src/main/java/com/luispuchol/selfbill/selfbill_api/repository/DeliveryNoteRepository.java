package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;

public interface DeliveryNoteRepository
        extends JpaRepository<DeliveryNote, Integer>, JpaSpecificationExecutor<DeliveryNote> {

    boolean existsByCode(Integer code);

    @Query("SELECT COALESCE(MAX(dn.code), 0) + 1 FROM DeliveryNote dn")
    int findNextCode();

    @Query("SELECT dn FROM DeliveryNote dn LEFT JOIN dn.invoice i WHERE dn.client.id = :clientId "
            + "AND (i IS NULL OR i.status = com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus.DRAFT)")
    List<DeliveryNote> findEditableByClientId(@Param("clientId") Integer clientId);
}
