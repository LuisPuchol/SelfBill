package com.luispuchol.selfbill.selfbill_api.shared;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
