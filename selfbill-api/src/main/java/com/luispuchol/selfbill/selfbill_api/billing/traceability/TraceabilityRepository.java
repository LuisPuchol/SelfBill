package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLine;

// Reads live lines of live delivery notes: both entities carry @SQLRestriction. `to` is exclusive
interface TraceabilityRepository extends JpaRepository<DeliveryNoteLine, Integer> {

    @Query("""
            SELECT DISTINCT a.id, a.code, a.name FROM DeliveryNoteLine l JOIN l.article a JOIN l.deliveryNote n
            WHERE n.date >= :from AND n.date < :to ORDER BY a.code""")
    List<Object[]> findArticles(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Rows of code1, code2, code3 and quantity
    @Query("""
            SELECT l.traceabilityCode1, l.traceabilityCode2, l.traceabilityCode3, l.quantity
            FROM DeliveryNoteLine l JOIN l.deliveryNote n
            WHERE l.article.id = :articleId AND n.date >= :from AND n.date < :to""")
    List<Object[]> findLotRows(@Param("articleId") Integer articleId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT l FROM DeliveryNoteLine l JOIN FETCH l.deliveryNote n JOIN FETCH n.client
            WHERE l.article.id = :articleId AND n.date >= :from AND n.date < :to
            AND (l.traceabilityCode1 = :code OR l.traceabilityCode2 = :code OR l.traceabilityCode3 = :code)
            ORDER BY n.date, n.code""")
    List<DeliveryNoteLine> findLinesOfLot(@Param("articleId") Integer articleId, @Param("code") String code,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
