package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteRepository;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteSpecification;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteTaxRegime;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentMessages;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentSender;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentType;
import com.luispuchol.selfbill.selfbill_api.billing.document.OutgoingDocument;
import com.luispuchol.selfbill.selfbill_api.billing.document.PdfFile;
import com.luispuchol.selfbill.selfbill_api.billing.document.PdfService;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.settings.profile.UserProfileService;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoiceRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final ClientRepository clientRepository;
    private final UserProfileService userProfileService;
    private final InvoiceMapper invoiceMapper;
    private final DeliveryNoteTaxRegime deliveryNoteTaxRegime;
    private final PdfService pdfService;
    private final DocumentSender documentSender;
    private final DocumentMessages documentMessages;
    private final Clock clock;

    public Page<InvoiceResponse> getAllInvoices(InvoiceFilter filter, Pageable pageable) {
        return invoiceRepository.findAll(InvoiceSpecification.withFilter(filter), pageable)
                .map(invoiceMapper::toSummary);
    }

    public InvoiceResponse getInvoiceById(Integer id) {
        return invoiceMapper.toResponse(findInvoice(id));
    }

    public List<PendingInvoiceResponse> getPendingInvoices(LocalDate dateTo) {
        List<DeliveryNote> pending = deliveryNoteRepository.findAll(
                DeliveryNoteSpecification.withFilter(DeliveryNoteFilter.builder().invoiced(false).dateTo(dateTo).build()),
                Sort.by("date", "code"));

        // Same grouping an invoice demands: one client, one calendar month, one pair of rates
        record Key(Client client, YearMonth month, BigDecimal vat, BigDecimal surcharge) {
        }
        return pending.stream()
                .collect(Collectors.groupingBy(
                        note -> new Key(note.getClient(), YearMonth.from(note.getDate()),
                                note.getVatPercentage().stripTrailingZeros(),
                                note.getSurchargePercentage().stripTrailingZeros()),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted(Comparator.<Map.Entry<Key, List<DeliveryNote>>, Integer>comparing(e -> e.getKey().client().getCode())
                        .thenComparing(e -> e.getKey().month())
                        .thenComparing(e -> e.getKey().vat())
                        .thenComparing(e -> e.getKey().surcharge()))
                .map(e -> toPendingInvoice(e.getKey().client(), e.getKey().month(), e.getValue()))
                .toList();
    }

    private static PendingInvoiceResponse toPendingInvoice(Client client, YearMonth month,
            List<DeliveryNote> notes) {
        BigDecimal vatPercentage = notes.get(0).getVatPercentage();
        BigDecimal surchargePercentage = notes.get(0).getSurchargePercentage();
        BigDecimal subtotal = notes.stream().map(DeliveryNote::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vatAmount = percentageOf(subtotal, vatPercentage);
        BigDecimal surchargeAmount = percentageOf(subtotal, surchargePercentage);
        return PendingInvoiceResponse.builder()
                .clientId(client.getId())
                .clientCode(client.getCode())
                .clientName(client.getName())
                .month(month.toString())
                .vatPercentage(vatPercentage)
                .surchargePercentage(surchargePercentage)
                .subtotal(subtotal)
                .vatAmount(vatAmount)
                .surchargeAmount(surchargeAmount)
                .total(subtotal.add(vatAmount).add(surchargeAmount))
                .clientDeleted(client.getDeletedAt() != null)
                .missingFiscalData(missingFiscalData(client))
                .deliveryNotes(notes.stream()
                        .map(note -> PendingDeliveryNoteResponse.builder()
                                .id(note.getId())
                                .code(note.getCode())
                                .date(note.getDate())
                                .total(note.getTotal())
                                .build())
                        .toList())
                .build();
    }

    public InvoiceResponse getInvoiceByCode(InvoiceSeries series, Integer code) {
        return invoiceRepository.findBySeriesAndCode(series, code)
                .map(invoiceMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, series.format(code)));
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Client client = findClient(request.clientId());
        List<DeliveryNote> deliveryNotes = findInvoiceableDeliveryNotes(request, null);

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .client(client)
                .series(InvoiceSeries.forVatType(client.getVatType()))
                .build());
        include(invoice, deliveryNotes);
        return invoiceMapper.toResponse(invoice);
    }

    // One outer transaction: if issuing fails, the draft is rolled back with it
    @Transactional
    public InvoiceResponse createAndIssue(InvoiceRequest request) {
        InvoiceResponse draft = createInvoice(request);
        // Writes the draft first, as a draft saved earlier is: otherwise the draft's lines, inserted by an
        // auto-flush along the way, are left behind when issuing rebuilds them
        invoiceRepository.flush();
        return issueInvoice(draft.id());
    }

    @Transactional
    public InvoiceResponse updateInvoice(Integer id, InvoiceRequest request) {
        Invoice invoice = findInvoice(id);
        requireDraft(invoice);
        Client client = findClient(request.clientId());
        List<DeliveryNote> deliveryNotes = findInvoiceableDeliveryNotes(request, invoice.getId());

        releaseDeliveryNotesNotIn(invoice, deliveryNotes);
        invoice.setClient(client);
        include(invoice, deliveryNotes);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse issueInvoice(Integer id) {
        Invoice invoice = findInvoice(id);
        requireDraft(invoice);
        requireIssuable(invoice.getClient());
        recalculate(invoice, invoice.getDeliveryNotes());

        invoice.setCode(invoiceRepository.findMaxCode(invoice.getSeries().name()).orElse(0) + 1);
        invoice.setDate(LocalDateTime.now(clock));
        invoice.setStatus(InvoiceStatus.ISSUED);
        freezeClientData(invoice);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void recalculateDraft(Invoice invoice) {
        requireDraft(invoice);
        recalculate(invoice, invoice.getDeliveryNotes());
    }

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendInvoiceEmail(Integer id) {
        documentSender.send(DocumentType.INVOICE, () -> {
            Invoice invoice = findIssuedInvoice(id);
            return new OutgoingDocument(invoice.getClient(), invoice.getNumber(),
                    pdfService.generateInvoicePdf(invoice));
        });
    }

    private List<DeliveryNote> findInvoiceableDeliveryNotes(InvoiceRequest request, Integer editedInvoiceId) {
        List<Integer> ids = request.deliveryNoteIds().stream().distinct().toList();
        List<DeliveryNote> deliveryNotes = deliveryNoteRepository.findAllById(ids);
        if (deliveryNotes.size() != ids.size()) {
            Set<Integer> foundIds = deliveryNotes.stream().map(DeliveryNote::getId).collect(Collectors.toSet());
            String missingIds = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, missingIds);
        }

        for (DeliveryNote deliveryNote : deliveryNotes) {
            if (!deliveryNote.getClient().getId().equals(request.clientId())) {
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

    // Both sides of the association: a later step of the same transaction reads the invoice's notes
    private void include(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        deliveryNotes.forEach(deliveryNote -> deliveryNote.setInvoice(invoice));
        invoice.getDeliveryNotes().clear();
        invoice.getDeliveryNotes().addAll(deliveryNotes);
        recalculate(invoice, deliveryNotes);
    }

    private void recalculate(Invoice invoice, List<DeliveryNote> deliveryNotes) {
        requireSingleMonth(deliveryNotes);
        invoice.setSeries(InvoiceSeries.forVatType(invoice.getClient().getVatType()));
        deliveryNotes.forEach(deliveryNote -> deliveryNoteTaxRegime.apply(deliveryNote, invoice.getClient()));
        applyFinancials(invoice, deliveryNotes);
        invoice.getLines().clear();
        invoice.getLines().addAll(buildLines(invoice, deliveryNotes));
    }

    // A recapitulative invoice groups operations of one calendar month (CONCEPTO.md §2)
    private static void requireSingleMonth(List<DeliveryNote> deliveryNotes) {
        long months = deliveryNotes.stream()
                .map(deliveryNote -> YearMonth.from(deliveryNote.getDate()))
                .distinct()
                .count();
        if (months > 1) {
            throw new BusinessException(ErrorCode.INVOICE_MIXED_MONTHS);
        }
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
                .sorted(Comparator.comparing(DeliveryNote::getDate).thenComparing(DeliveryNote::getCode))
                .flatMap(deliveryNote -> deliveryNote.getLines().stream()
                        .map(line -> InvoiceLine.builder()
                                .invoice(invoice)
                                .deliveryNote(deliveryNote)
                                .deliveryNoteCode(deliveryNote.getCode())
                                .deliveryNoteDate(deliveryNote.getDate())
                                .article(line.getArticle())
                                .articleName(line.getArticle().getName())
                                .articleCode(line.getArticle().getCode())
                                .traceabilityCode1(line.getTraceabilityCode1())
                                .traceabilityCode2(line.getTraceabilityCode2())
                                .traceabilityCode3(line.getTraceabilityCode3())
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

    // A complete invoice shows the issuer's and the client's NIF and address (RD 1619/2012, art. 6)
    private void requireIssuable(Client client) {
        if (client.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.CLIENT_NOT_FOUND, client.getId());
        }
        List<String> missing = missingFiscalData(client);
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.CLIENT_FISCAL_DATA_MISSING, client.getId(),
                    String.join(", ", missing));
        }
        if (!userProfileService.hasProfile()) {
            throw new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
    }

    static List<String> missingFiscalData(Client client) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(client.getNif())) {
            missing.add("nif");
        }
        if (!StringUtils.hasText(client.getAddress())) {
            missing.add("address");
        }
        if (!StringUtils.hasText(client.getPostalCode())) {
            missing.add("postalCode");
        }
        if (!StringUtils.hasText(client.getCity())) {
            missing.add("city");
        }
        return missing;
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
}
