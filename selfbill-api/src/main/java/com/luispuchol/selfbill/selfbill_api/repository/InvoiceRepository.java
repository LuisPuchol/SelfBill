package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByCode(Integer code);
}
