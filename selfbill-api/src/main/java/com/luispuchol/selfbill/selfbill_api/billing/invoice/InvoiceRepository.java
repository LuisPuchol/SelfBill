package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {

    // The ledger shows the client of every row: load them with the page, not one by one
    @Override
    @EntityGraph(attributePaths = "client")
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    Optional<Invoice> findBySeriesAndCode(InvoiceSeries series, Integer code);

    // Native so it also counts soft-deleted invoices, whose numbers stay taken
    @Query(value = "SELECT MAX(code) FROM invoices WHERE series = :series", nativeQuery = true)
    Optional<Integer> findMaxCode(@Param("series") String series);

    // Invoice filters the deleted ones out; Client does not, so a deleted client still shows
    @Query("""
            SELECT new com.luispuchol.selfbill.selfbill_api.billing.invoice.ClientSummaryResponse(
                c.id, c.code, c.name, CAST(COUNT(i) AS integer),
                SUM(i.subtotal), SUM(i.vatAmount), SUM(i.surchargeAmount), SUM(i.total))
            FROM Invoice i JOIN i.client c
            WHERE i.status = com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus.ISSUED
              AND i.date >= :from AND i.date < :to
            GROUP BY c.id, c.code, c.name
            ORDER BY c.code
            """)
    List<ClientSummaryResponse> summarizeByClient(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
