package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentMessages;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentSender;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentType;
import com.luispuchol.selfbill.selfbill_api.billing.document.OutgoingDocument;
import com.luispuchol.selfbill.selfbill_api.billing.document.PdfFile;
import com.luispuchol.selfbill.selfbill_api.billing.document.PdfService;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceService;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.catalog.article.Article;
import com.luispuchol.selfbill.selfbill_api.catalog.article.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientTaxRegimeChanged;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;
    private final DeliveryNoteTaxRegime deliveryNoteTaxRegime;
    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private final DocumentSender documentSender;
    private final DocumentMessages documentMessages;

    public Page<DeliveryNoteResponse> getAllDeliveryNotes(DeliveryNoteFilter filter, Pageable pageable) {
        return deliveryNoteRepository.findAll(DeliveryNoteSpecification.withFilter(filter), pageable)
                .map(deliveryNoteMapper::toResponse);
    }

    public DeliveryNoteResponse getDeliveryNoteById(Integer id) {
        return deliveryNoteMapper.toResponse(findDeliveryNote(id));
    }

    @Transactional
    public DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest request) {
        Integer code = request.code() != null ? request.code() : deliveryNoteRepository.findNextCode();
        requireUnusedCode(code);
        Client client = findClient(request.clientId());

        DeliveryNote deliveryNote = deliveryNoteMapper.toEntity(request, client);
        deliveryNote.setCode(code);
        deliveryNote.setFormat(client.getDefaultDeliveryNoteFormat());
        deliveryNoteTaxRegime.apply(deliveryNote, client);
        replaceLines(deliveryNote, request.lines());
        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(deliveryNote));
    }

    @Transactional
    public DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest request) {
        DeliveryNote deliveryNote = findDeliveryNote(id);
        requireEditable(deliveryNote, request.clientId());
        Integer code = request.code() != null ? request.code() : deliveryNote.getCode();
        if (!deliveryNote.getCode().equals(code)) {
            requireUnusedCode(code);
        }

        boolean clientChanged = !deliveryNote.getClient().getId().equals(request.clientId());
        Client client = findClient(request.clientId());
        deliveryNoteMapper.updateEntity(deliveryNote, request, client);
        deliveryNote.setCode(code);
        if (clientChanged) {
            deliveryNote.setFormat(client.getDefaultDeliveryNoteFormat());
            deliveryNoteTaxRegime.apply(deliveryNote, client);
        }
        replaceLines(deliveryNote, request.lines());

        Invoice invoice = deliveryNote.getInvoice();
        if (invoice != null) {
            invoiceService.recalculateDraft(invoice);
        }
        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(deliveryNote));
    }

    @EventListener
    @Transactional
    public void refreshTaxRegime(ClientTaxRegimeChanged event) {
        Client client = event.client();
        List<DeliveryNote> editable = deliveryNoteRepository.findEditableByClientId(client.getId());
        editable.forEach(deliveryNote -> deliveryNoteTaxRegime.apply(deliveryNote, client));
        editable.stream()
                .map(DeliveryNote::getInvoice)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(invoiceService::recalculateDraft);
    }

    @Transactional
    public void deleteDeliveryNote(Integer id) {
        DeliveryNote deliveryNote = findDeliveryNote(id);
        if (deliveryNote.getInvoice() != null) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_ALREADY_INVOICED, deliveryNote.getCode());
        }
        deliveryNoteRepository.delete(deliveryNote);
    }

    public PdfFile exportDeliveryNotePdf(Integer id) {
        DeliveryNote deliveryNote = findDeliveryNote(id);
        return new PdfFile(documentMessages.filename(DocumentType.DELIVERY_NOTE, String.valueOf(deliveryNote.getCode())),
                pdfService.generateDeliveryNotePdf(deliveryNote));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendDeliveryNoteEmail(Integer id) {
        documentSender.send(DocumentType.DELIVERY_NOTE, () -> {
            DeliveryNote deliveryNote = findDeliveryNote(id);
            return new OutgoingDocument(deliveryNote.getClient(), String.valueOf(deliveryNote.getCode()),
                    pdfService.generateDeliveryNotePdf(deliveryNote));
        });
    }

    private static void requireEditable(DeliveryNote deliveryNote, Integer requestedClientId) {
        Invoice invoice = deliveryNote.getInvoice();
        if (invoice == null) {
            return;
        }
        if (invoice.getStatus() == InvoiceStatus.ISSUED) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_ALREADY_INVOICED, deliveryNote.getCode());
        }
        if (!deliveryNote.getClient().getId().equals(requestedClientId)) {
            throw new BusinessException(ErrorCode.INVOICE_MIXED_CLIENTS);
        }
    }

    private void replaceLines(DeliveryNote deliveryNote, List<DeliveryNoteLineRequest> lineRequests) {
        deliveryNote.getLines().clear();
        if (lineRequests != null) {
            for (DeliveryNoteLineRequest lineRequest : lineRequests) {
                DeliveryNoteLine line = deliveryNoteMapper.toLineEntity(lineRequest,
                        findArticle(lineRequest.articleId()), deliveryNote);
                line.setTotal(line.getQuantity().multiply(line.getPrice()).setScale(2, RoundingMode.HALF_UP));
                deliveryNote.getLines().add(line);
            }
        }
        deliveryNote.setTotal(deliveryNote.getLines().stream()
                .map(DeliveryNoteLine::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void requireUnusedCode(Integer code) {
        if (deliveryNoteRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE, code);
        }
    }

    private DeliveryNote findDeliveryNote(Integer id) {
        return deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));
    }

    private Client findClient(Integer id) {
        return clientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));
    }

    private Article findArticle(Integer id) {
        return articleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, id));
    }
}
