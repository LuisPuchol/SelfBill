package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Integer> {

    Optional<TaxConfig> findFirstBy();
}
