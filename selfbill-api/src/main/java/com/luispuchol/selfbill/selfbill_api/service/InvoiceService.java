package com.luispuchol.selfbill.selfbill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceSectionResponse;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.InvoiceLine;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.InvoiceMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;
import com.luispuchol.selfbill.selfbill_api.repository.InvoiceRepository;
import com.luispuchol.selfbill.selfbill_api.specification.InvoiceSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

        private final InvoiceRepository invoiceRepository;
        private final DeliveryNoteRepository deliveryNoteRepository;
        private final ClientRepository clientRepository;
        private final InvoiceMapper invoiceMapper;
        private final ITaxConfigService taxConfigService;

        @Transactional(readOnly = true)
        @Override
        public Page<InvoiceResponse> getAllInvoices(InvoiceFilter filter, Pageable pageable) {
                return invoiceRepository.findAll(InvoiceSpecification.withFilter(filter), pageable)
                                .map(invoiceMapper::toResponse);
        }

        @Transactional(readOnly = true)
        @Override
        public InvoiceResponse getInvoiceById(Integer id) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, id));
                return toResponseWithSections(invoice);
        }

        @Transactional(readOnly = true)
        @Override
        public InvoiceResponse getInvoiceByCode(Integer code) {
                Invoice invoice = invoiceRepository.findByCode(code)
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, code));
                return toResponseWithSections(invoice);
        }

        @Transactional
        @Override
        public InvoiceResponse createInvoice(InvoiceRequest request) {
                List<Integer> deliveryNoteIds = request.getDeliveryNoteIds();
                Integer clientId = request.getClientId();

                List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllById(deliveryNoteIds);
                Client client = clientRepository.findById(clientId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, clientId));

                validateRequest(clientId, deliveryNoteIds, deliveryNotes);

                Invoice invoice = buildInvoice(client, deliveryNotes);
                buildAndLinkLines(invoice, deliveryNotes);
                return toResponseWithSections(invoiceRepository.save(invoice));
        }

        private void validateRequest(Integer clientId, List<Integer> deliveryNoteIds,
                        List<DeliveryNote> deliveryNotes) {
                if (deliveryNotes.size() != deliveryNoteIds.size()) {
                        throw new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, deliveryNoteIds);
                }

                boolean mixedClients = deliveryNotes.stream()
                                .anyMatch(dn -> !dn.getClient().getId().equals(clientId));
                if (mixedClients) {
                        throw new BusinessException(ErrorCode.INVOICE_MIXED_CLIENTS);
                }

                deliveryNotes.forEach(dn -> {
                        if (dn.getInvoice() != null) {
                                throw new BusinessException(ErrorCode.INVOICE_DUPLICATE_DELIVERY_NOTE, dn.getCode());
                        }
                });
        }

        private Invoice buildInvoice(Client client, List<DeliveryNote> deliveryNotes) {
                TaxConfig taxConfig = taxConfigService.getTaxConfigEntity();
                BigDecimal vatPercentage = taxConfig.getVatPercentage();
                BigDecimal surchargePercentage = taxConfig.getSurchargePercentage();

                BigDecimal subtotal = deliveryNotes.stream()
                                .map(DeliveryNote::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal vatAmount = client.getVatType() == VatType.WITH_VAT
                                ? subtotal.multiply(vatPercentage).divide(BigDecimal.valueOf(100), 2,
                                                RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                BigDecimal surchargeAmount = client.getSurchargeType() == SurchargeType.WITH_SURCHARGE
                                ? subtotal.multiply(surchargePercentage).divide(BigDecimal.valueOf(100), 2,
                                                RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                Invoice invoice = invoiceMapper.toEntity(client, deliveryNotes);
                invoice.setSubtotal(subtotal);
                invoice.setVatPercentage(vatPercentage);
                invoice.setVatAmount(vatAmount);
                invoice.setSurchargePercentage(surchargePercentage);
                invoice.setSurchargeAmount(surchargeAmount);
                invoice.setTotal(subtotal.add(vatAmount).add(surchargeAmount));
                invoice.setCode(invoiceRepository.findMaxCode().orElse(0) + 1);
                invoice.setDate(LocalDateTime.now());
                return invoice;
        }

        private void buildAndLinkLines(Invoice invoice, List<DeliveryNote> deliveryNotes) {
                List<InvoiceLine> lines = deliveryNotes.stream()
                                .flatMap(dn -> dn.getDeliveryNoteArticles().stream()
                                                .map(dna -> InvoiceLine.builder()
                                                                .invoice(invoice)
                                                                .deliveryNote(dn)
                                                                .deliveryNoteCode(dn.getCode())
                                                                .deliveryNoteDate(dn.getDate())
                                                                .article(dna.getArticle())
                                                                .articleName(dna.getArticle().getName())
                                                                .articleCode(dna.getArticle().getCode())
                                                                .trazabilityCode1(dna.getTrazabilityCode1())
                                                                .trazabilityCode2(dna.getTrazabilityCode2())
                                                                .trazabilityCode3(dna.getTrazabilityCode3())
                                                                .quantity(dna.getQuantity())
                                                                .price(dna.getPrice())
                                                                .total(dna.getTotal())
                                                                .build()))
                                .toList();
                invoice.setLines(lines);
                deliveryNotes.forEach(dn -> dn.setInvoice(invoice));
        }

        @Transactional
        @Override
        public InvoiceResponse updateInvoice(Integer id, InvoiceRequest request) {
                return null;
        }

        @Transactional
        @Override
        public void deleteInvoice(Integer id) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, id));

                invoiceRepository.delete(invoice);
        }

        private InvoiceResponse toResponseWithSections(Invoice invoice) {
                InvoiceResponse response = invoiceMapper.toResponse(invoice);
                response.setSections(buildSections(invoice));
                return response;
        }

        private List<InvoiceSectionResponse> buildSections(Invoice invoice) {
                Map<Integer, List<InvoiceLine>> byDn = invoice.getLines().stream()
                                .collect(Collectors.groupingBy(InvoiceLine::getDeliveryNoteCode));

                return invoice.getDeliveryNotes().stream()
                                .map(dn -> InvoiceSectionResponse.builder()
                                                .deliveryNoteId(dn.getId())
                                                .deliveryNoteCode(dn.getCode())
                                                .deliveryNoteDate(dn.getDate())
                                                .lines(byDn.getOrDefault(dn.getCode(), List.of()).stream()
                                                                .map(invoiceMapper::toLineResponse)
                                                                .toList())
                                                .build())
                                .toList();
        }
}
