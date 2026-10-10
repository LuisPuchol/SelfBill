package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceSeries;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findBySeriesAndCode(InvoiceSeries series, Integer code);

    // Native so it also counts soft-deleted invoices, whose numbers stay taken
    @Query(value = "SELECT MAX(code) FROM invoices WHERE series = :series", nativeQuery = true)
    Optional<Integer> findMaxCode(@Param("series") String series);
}
