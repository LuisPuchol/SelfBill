package com.luispuchol.selfbill.selfbill_api.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceLineResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.InvoiceLine;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(target = "sections", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(source = "client", target = "client")
    @Mapping(source = "deliveryNotes", target = "deliveryNotes")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "vatPercentage", ignore = true)
    @Mapping(target = "vatAmount", ignore = true)
    @Mapping(target = "surchargePercentage", ignore = true)
    @Mapping(target = "surchargeAmount", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Invoice toEntity(Client client, List<DeliveryNote> deliveryNotes);

    @Mapping(source = "article.id", target = "articleId")
    InvoiceLineResponse toLineResponse(InvoiceLine invoiceLine);
}
