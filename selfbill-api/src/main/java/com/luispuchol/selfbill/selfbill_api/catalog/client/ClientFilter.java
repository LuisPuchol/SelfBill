package com.luispuchol.selfbill.selfbill_api.catalog.client;

import lombok.Data;

@Data
public class ClientFilter {
    private String name;
    private String businessName;
    private Integer codeFrom;
    private Integer codeTo;
}
