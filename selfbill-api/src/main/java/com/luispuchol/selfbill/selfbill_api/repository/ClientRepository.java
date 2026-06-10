package com.luispuchol.selfbill.selfbill_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.luispuchol.selfbill.selfbill_api.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByCode(Integer code);
    Optional<Client> findByNameIgnoreCase(String name);
    Optional<Client> findByNif(String nif);
}
