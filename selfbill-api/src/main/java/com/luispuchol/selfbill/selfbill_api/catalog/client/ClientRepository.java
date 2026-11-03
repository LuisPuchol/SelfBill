package com.luispuchol.selfbill.selfbill_api.catalog.client;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, Integer>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByIdAndDeletedAtIsNull(Integer id);

    Optional<Client> findByCodeAndDeletedAtIsNull(Integer code);

    List<Client> findByNameIgnoreCaseAndDeletedAtIsNullOrderByCode(String name);

    List<Client> findByNifAndDeletedAtIsNullOrderByCode(String nif);

    @Query("SELECT COALESCE(MAX(c.code), 0) + 1 FROM Client c WHERE c.deletedAt IS NULL")
    int findNextCode();

    boolean existsByCodeAndDeletedAtIsNull(Integer code);
}
