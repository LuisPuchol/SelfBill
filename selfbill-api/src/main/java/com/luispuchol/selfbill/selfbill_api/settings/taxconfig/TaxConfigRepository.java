package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Integer> {

    Optional<TaxConfig> findFirstBy();
}
