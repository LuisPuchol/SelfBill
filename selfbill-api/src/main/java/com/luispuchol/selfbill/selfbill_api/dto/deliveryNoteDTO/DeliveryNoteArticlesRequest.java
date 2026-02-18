package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DeliveryNoteArticlesRequest {

    @NotNull(message = "Article ID cannot be null")
    @Positive(message = "Article ID must be positive")
    private Integer id;

    private Integer deliveryNoteId;

    @NotNull(message = "Article ID cannot be null")
    @Positive(message = "Article ID must be positive")
    private Integer articleId;

    private Integer trazabilityCode1;
    private Integer trazabilityCode2;
    private Integer trazabilityCode3;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal total;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
