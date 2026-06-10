package com.luispuchol.selfbill.selfbill_api.dto.clientDTO;

import lombok.Data;

@Data
public class ClientFilter {
    private String name;
    private String businessName;
    private Integer codeFrom;
    private Integer codeTo;
}
