package com.luispuchol.selfbill.selfbill_api.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.PdfFile;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteLineRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNoteLine;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import com.luispuchol.selfbill.selfbill_api.enums.DocumentType;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.DeliveryNoteMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;
import com.luispuchol.selfbill.selfbill_api.specification.DeliveryNoteSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;
    private final TaxConfigService taxConfigService;
    private final InvoiceService invoiceService;
    private final PdfService pdfService;
    private final EmailService emailService;
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
        requireUnusedCode(request.getCode());
        Client client = findClient(request.getClientId());

        DeliveryNote deliveryNote = deliveryNoteMapper.toEntity(request, client);
        deliveryNote.setFormat(client.getDefaultDeliveryNoteFormat());
        applyTaxRegime(deliveryNote, client);
        replaceLines(deliveryNote, request.getLines());
        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(deliveryNote));
    }

    @Transactional
    public DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest request) {
        DeliveryNote deliveryNote = findDeliveryNote(id);
        requireEditable(deliveryNote, request.getClientId());
        if (!deliveryNote.getCode().equals(request.getCode())) {
            requireUnusedCode(request.getCode());
        }

        boolean clientChanged = !deliveryNote.getClient().getId().equals(request.getClientId());
        Client client = findClient(request.getClientId());
        deliveryNoteMapper.updateEntity(deliveryNote, request, client);
        if (clientChanged) {
            deliveryNote.setFormat(client.getDefaultDeliveryNoteFormat());
            applyTaxRegime(deliveryNote, client);
        }
        replaceLines(deliveryNote, request.getLines());

        Invoice invoice = deliveryNote.getInvoice();
        if (invoice != null) {
            invoiceService.recalculateDraft(invoice);
        }
        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(deliveryNote));
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

    public void sendDeliveryNoteEmail(Integer id) {
        DeliveryNote deliveryNote = findDeliveryNote(id);
        emailService.sendDocument(DocumentType.DELIVERY_NOTE, deliveryNote.getClient(),
                String.valueOf(deliveryNote.getCode()),
                pdfService.generateDeliveryNotePdf(deliveryNote));
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

    // Frozen with the client's regime applied, so a reprint never consults the client (CONCEPTO.md §3, §4)
    private void applyTaxRegime(DeliveryNote deliveryNote, Client client) {
        TaxConfig taxConfig = taxConfigService.getCurrent();
        deliveryNote.setVatPercentage(client.getVatType() == VatType.WITH_VAT
                ? taxConfig.getVatPercentage() : BigDecimal.ZERO);
        deliveryNote.setSurchargePercentage(client.getSurchargeType() == SurchargeType.WITH_SURCHARGE
                ? taxConfig.getSurchargePercentage() : BigDecimal.ZERO);
    }

    private void replaceLines(DeliveryNote deliveryNote, List<DeliveryNoteLineRequest> lineRequests) {
        deliveryNote.getLines().clear();
        if (lineRequests != null) {
            for (DeliveryNoteLineRequest lineRequest : lineRequests) {
                DeliveryNoteLine line = deliveryNoteMapper.toLineEntity(lineRequest,
                        findArticle(lineRequest.getArticleId()), deliveryNote);
                line.setTotal(line.getQuantity().multiply(line.getPrice()));
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
