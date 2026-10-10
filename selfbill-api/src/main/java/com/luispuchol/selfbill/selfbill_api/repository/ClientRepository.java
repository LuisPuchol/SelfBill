package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.luispuchol.selfbill.selfbill_api.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Integer>, JpaSpecificationExecutor<Client> {

    Optional<Client> findByIdAndDeletedAtIsNull(Integer id);

    Optional<Client> findByCodeAndDeletedAtIsNull(Integer code);

    Optional<Client> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    Optional<Client> findByNifAndDeletedAtIsNull(String nif);

    boolean existsByCodeAndDeletedAtIsNull(Integer code);
}
