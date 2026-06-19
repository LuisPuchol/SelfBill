package com.luispuchol.selfbill.selfbill_api.repository;

import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Integer> {
}
