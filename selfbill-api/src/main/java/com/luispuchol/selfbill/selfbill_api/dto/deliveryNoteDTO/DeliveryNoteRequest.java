package com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryNoteRequest {

    @NotNull(message = "Date cannot be null")
    private LocalDateTime date;

    @NotNull(message = "Code cannot be null")
    @Positive(message = "Code must be positive")
    private Integer code;

    private BigDecimal total;

    @NotNull(message = "Client ID cannot be null")
    @Positive(message = "Client ID must be positive")
    private Integer clientId;

    @Valid
    private List<DeliveryNoteArticlesRequest> lines;
}
