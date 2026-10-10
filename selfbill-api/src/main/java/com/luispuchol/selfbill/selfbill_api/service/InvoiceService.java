package com.luispuchol.selfbill.selfbill_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.PdfFile;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceSectionResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.InvoiceLine;
import com.luispuchol.selfbill.selfbill_api.enums.DocumentType;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceSeries;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;
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
@Transactional(readOnly = true)
public class InvoiceService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoiceRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final ClientRepository clientRepository;
    private final InvoiceMapper invoiceMapper;
    private final DeliveryNoteTaxRegime deliveryNoteTaxRegime;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final DocumentMessages documentMessages;
    private final Clock clock;

    public Page<InvoiceResponse> getAllInvoices(InvoiceFilter filter, Pageable pageable) {
        return invoiceRepository.findAll(InvoiceSpecification.withFilter(filter), pageable)
                .map(invoiceMapper::toResponse);
    }

    public InvoiceResponse getInvoiceById(Integer id) {
        return toResponseWithSections(findInvoice(id));
    }

    public InvoiceResponse getInvoiceByCode(InvoiceSeries series, Integer code) {
        return invoiceRepository.findBySeriesAndCode(series, code)
                .map(this::toResponseWithSections)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, series.format(code)));
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Client client = findClient(request.getClientId());
        List<DeliveryNote> deliveryNotes = findInvoiceableDeliveryNotes(request, null);

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .client(client)
                .series(InvoiceSeries.forVatType(client.getVatType()))
                .build());
        include(invoice, deliveryNotes);
        return toResponseWithSections(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Integer id, InvoiceRequest request) {
        Invoice invoice = findInvoice(id);
        requireDraft(invoice);
        Client client = findClient(request.getClientId());
        List<DeliveryNote> deliveryNotes = findInvoiceableDeliveryNotes(request, invoice.getId());

        releaseDeliveryNotesNotIn(invoice, deliveryNotes);
        invoice.setClient(client);
        include(invoice, deliveryNotes);
        return toResponseWithSections(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse issueInvoice(Integer id) {
        Invoice invoice = findInvoice(id);
        requireDraft(invoice);
        // Safety net: amounts and series must match the delivery notes and client being issued
        recalculate(invoice, invoice.getDeliveryNotes());

        // Numbered only on issue so a discarded draft never consumes a number (CONCEPTO.md §5)
        invoice.setCode(invoiceRepository.findMaxCode(invoice.getSeries().name()).orElse(0) + 1);
        invoice.setDate(LocalDateTime.now(clock));
        invoice.setStatus(InvoiceStatus.ISSUED);
        freezeClientData(invoice);
        return toResponseWithSections(invoiceRepository.save(invoice));
    }

    @Transactional
    public void recalculateDraft(Invoice invoice) {
        requireDraft(invoice);
        recalculate(invoice, invoice.getDeliveryNotes());
    }

    // Until Verifactu, deleting an issued invoice is the available correction path (CONCEPTO.md §5)
    @Transactional
    public void deleteInvoice(Integer id) {
        Invoice invoice = findInvoice(id);
        invoice.getDeliveryNotes().forEach(deliveryNote -> {
            deliveryNote.setInvoice(null);
            deliveryNoteTaxRegime.apply(deliveryNote, deliveryNote.getClient());
        });
        invoiceRepository.delete(invoice);
    }

    public PdfFile exportInvoicePdf(Integer id) {
        Invoice invoice = findIssuedInvoice(id);
        return new PdfFile(documentMessages.filename(DocumentType.INVOICE, invoice.getNumber()),
                pdfService.generateInvoicePdf(invoice));
    }

    public void sendInvoiceEmail(Integer id) {
        Invoice invoice = findIssuedInvoice(id);
        emailService.sendDocument(DocumentType.INVOICE, invoice.getClient(), invoice.getNumber(),
                pdfService.generateInvoicePdf(invoice));
    }

    private List<DeliveryNote> findInvoiceableDeliveryNotes(InvoiceRequest request, Integer editedInvoiceId) {
        List<Integer> ids = request.getDeliveryNoteIds();
        List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllById(ids);
        if (deliveryNotes.size() != ids.size()) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, ids);
        }

        for (DeliveryNote deliveryNote : deliveryNotes) {
            if (!deliveryNote.getClient().getId().equals(request.getClientId())) {
                throw new BusinessException(ErrorCode.INVOICE_MIXED_CLIENTS);
            }
            Invoice current = deliveryNote.getInvoice();
            if (current != null && !current.getId().equals(editedInvoiceId)) {
                throw new BusinessException(ErrorCode.INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED,
                        deliveryNote.getCode());
            }
        }
        return deliveryNotes;
    }

    private static void releaseDeliveryNotesNotIn(Invoice invoice, List<DeliveryNote> kept) {
        Set<Integer> keptIds = kept.stream().map(DeliveryNote::getId).collect(Collectors.toSet());
        invoice.getDeliveryNotes().stream()
                .filter(deliveryNote -> !keptIds.contains(deliveryNote.getId()))
                .forEach(deliveryNote -> deliveryNote.setInvoice(null));
    }

    private void include(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        deliveryNotes.forEach(deliveryNote -> deliveryNote.setInvoice(invoice));
        recalculate(invoice, deliveryNotes);
    }

    private void recalculate(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        invoice.setSeries(InvoiceSeries.forVatType(invoice.getClient().getVatType()));
        // Delivery notes not locked by an issued invoice follow their client's regime (CONCEPTO.md §3)
        deliveryNotes.forEach(deliveryNote -> deliveryNoteTaxRegime.apply(deliveryNote, invoice.getClient()));
        applyFinancials(invoice, deliveryNotes);
        invoice.getLines().clear();
        invoice.getLines().addAll(buildLines(invoice, deliveryNotes));
    }

    private void applyFinancials(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        BigDecimal vatPercentage = commonRate(deliveryNotes, DeliveryNote::getVatPercentage);
        BigDecimal surchargePercentage = commonRate(deliveryNotes, DeliveryNote::getSurchargePercentage);

        BigDecimal subtotal = deliveryNotes.stream()
                .map(DeliveryNote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vatAmount = percentageOf(subtotal, vatPercentage);
        BigDecimal surchargeAmount = percentageOf(subtotal, surchargePercentage);

        invoice.setSubtotal(subtotal);
        invoice.setVatPercentage(vatPercentage);
        invoice.setVatAmount(vatAmount);
        invoice.setSurchargePercentage(surchargePercentage);
        invoice.setSurchargeAmount(surchargeAmount);
        invoice.setTotal(subtotal.add(vatAmount).add(surchargeAmount));
    }

    // One rate per invoice: the business never mixes them, so a mix is refused, not broken down (CONCEPTO.md §8)
    private static BigDecimal commonRate(List<DeliveryNote> deliveryNotes, Function<DeliveryNote, BigDecimal> rate) {
        if (deliveryNotes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal first = rate.apply(deliveryNotes.get(0));
        for (DeliveryNote deliveryNote : deliveryNotes) {
            if (rate.apply(deliveryNote).compareTo(first) != 0) {
                throw new BusinessException(ErrorCode.INVOICE_MIXED_TAX_RATES);
            }
        }
        return first;
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static List<InvoiceLine> buildLines(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        return deliveryNotes.stream()
                .flatMap(deliveryNote -> deliveryNote.getLines().stream()
                        .map(line -> InvoiceLine.builder()
                                .invoice(invoice)
                                .deliveryNote(deliveryNote)
                                .deliveryNoteCode(deliveryNote.getCode())
                                .deliveryNoteDate(deliveryNote.getDate())
                                .article(line.getArticle())
                                .articleName(line.getArticle().getName())
                                .articleCode(line.getArticle().getCode())
                                .trazabilityCode1(line.getTrazabilityCode1())
                                .trazabilityCode2(line.getTrazabilityCode2())
                                .trazabilityCode3(line.getTrazabilityCode3())
                                .quantity(line.getQuantity())
                                .price(line.getPrice())
                                .total(line.getTotal())
                                .build()))
                .toList();
    }

    private static void freezeClientData(Invoice invoice) {
        Client client = invoice.getClient();
        invoice.setClientBusinessName(client.getBusinessName());
        invoice.setClientNif(client.getNif());
        invoice.setClientAddress(client.getAddress());
        invoice.setClientCity(client.getCity());
        invoice.setClientProvince(client.getProvince());
        invoice.setClientPostalCode(client.getPostalCode());
    }

    private static void requireDraft(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_ISSUED, invoice.getCode());
        }
    }

    private Invoice findIssuedInvoice(Integer id) {
        Invoice invoice = findInvoice(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessException(ErrorCode.INVOICE_NOT_ISSUED, id);
        }
        return invoice;
    }

    private Invoice findInvoice(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, id));
    }

    private Client findClient(Integer id) {
        return clientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));
    }

    private InvoiceResponse toResponseWithSections(Invoice invoice) {
        InvoiceResponse response = invoiceMapper.toResponse(invoice);
        response.setSections(buildSections(invoice));
        return response;
    }

    // From the frozen lines, not the live relation: during a rectification the delivery notes
    // point to the new draft and the original must keep its sections (CONCEPTO.md §9.1)
    private List<InvoiceSectionResponse> buildSections(Invoice invoice) {
        Map<Integer, List<InvoiceLine>> linesByDeliveryNote = invoice.getLines().stream()
                .collect(Collectors.groupingBy(InvoiceLine::getDeliveryNoteCode, LinkedHashMap::new,
                        Collectors.toList()));

        return linesByDeliveryNote.values().stream()
                .map(lines -> {
                    InvoiceLine first = lines.get(0);
                    return InvoiceSectionResponse.builder()
                            .deliveryNoteId(first.getDeliveryNote() != null ? first.getDeliveryNote().getId() : null)
                            .deliveryNoteCode(first.getDeliveryNoteCode())
                            .deliveryNoteDate(first.getDeliveryNoteDate())
                            .lines(lines.stream().map(invoiceMapper::toLineResponse).toList())
                            .build();
                })
                .toList();
    }
}
