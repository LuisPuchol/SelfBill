package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryNoteRepository
        extends JpaRepository<DeliveryNote, Integer>, JpaSpecificationExecutor<DeliveryNote> {

    // The list shows the client and the invoice state of every row: load them with the page, not one by one
    @Override
    @EntityGraph(attributePaths = { "client", "invoice" })
    Page<DeliveryNote> findAll(Specification<DeliveryNote> spec, Pageable pageable);

    boolean existsByCode(Integer code);

    @Query("SELECT COALESCE(MAX(dn.code), 0) + 1 FROM DeliveryNote dn")
    int findNextCode();

    @Query("SELECT dn FROM DeliveryNote dn LEFT JOIN dn.invoice i WHERE dn.client.id = :clientId "
            + "AND (i IS NULL OR i.status = com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus.DRAFT)")
    List<DeliveryNote> findEditableByClientId(@Param("clientId") Integer clientId);
}
