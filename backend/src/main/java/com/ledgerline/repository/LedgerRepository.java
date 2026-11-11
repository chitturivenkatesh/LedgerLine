package com.ledgerline.repository;

import com.ledgerline.entity.LedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import java.util.*;

/**
 * Deliberately extends Repository, NOT JpaRepository: no update or delete method exists to call.
 * The database triggers are the second line of defence.
 */
public interface LedgerRepository extends Repository<LedgerEntry, Long> {
    <S extends LedgerEntry> S save(S entry);
    Optional<LedgerEntry> findById(Long id);
    long count();
    List<LedgerEntry> findAllByOrderByIdDesc(Pageable pageable);
    List<LedgerEntry> findByAccount_IdOrderByIdAsc(Long accountId);
    List<LedgerEntry> findByTransaction_IdOrderByIdAsc(Long transactionId);
}
