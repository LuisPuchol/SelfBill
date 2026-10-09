package com.luispuchol.selfbill.selfbill_api.dto.client;

import lombok.Data;

@Data
public class ClientFilter {
    private String name;
    private String businessName;
    private Integer codeFrom;
    private Integer codeTo;
}
