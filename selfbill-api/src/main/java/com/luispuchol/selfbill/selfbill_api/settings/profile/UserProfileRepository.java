package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

    Optional<UserProfile> findFirstBy();
}
