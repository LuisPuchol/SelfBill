package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InvoiceMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "sections", source = "lines")
    InvoiceResponse toResponse(Invoice invoice);

    @InheritConfiguration(name = "toResponse")
    @Mapping(target = "sections", ignore = true)
    InvoiceResponse toSummary(Invoice invoice);

    @Mapping(target = "articleId", source = "article.id")
    InvoiceLineResponse toLineResponse(InvoiceLine line);

    default List<InvoiceSectionResponse> toSections(List<InvoiceLine> lines) {
        Map<Integer, List<InvoiceLine>> linesByDeliveryNote = lines.stream()
                .collect(Collectors.groupingBy(InvoiceLine::getDeliveryNoteCode, LinkedHashMap::new,
                        Collectors.toList()));

        return linesByDeliveryNote.values().stream()
                .map(group -> {
                    InvoiceLine first = group.get(0);
                    return InvoiceSectionResponse.builder()
                            .deliveryNoteId(first.getDeliveryNote() != null ? first.getDeliveryNote().getId() : null)
                            .deliveryNoteCode(first.getDeliveryNoteCode())
                            .deliveryNoteDate(first.getDeliveryNoteDate())
                            .lines(group.stream().map(this::toLineResponse).toList())
                            .build();
                })
                .toList();
    }
}
