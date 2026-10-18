package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Integer> {

    Optional<TaxConfig> findFirstByValidFromLessThanEqualOrderByValidFromDesc(LocalDate date);

    Optional<TaxConfig> findByValidFrom(LocalDate validFrom);

    List<TaxConfig> findAllByOrderByValidFromDesc();

    boolean existsByValidFrom(LocalDate validFrom);
}
