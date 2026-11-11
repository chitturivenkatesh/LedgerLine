package com.ledgerline.repository;

import com.ledgerline.entity.TransactionAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface TransactionAttemptRepository extends Repository<TransactionAttempt, Long> {
    <S extends TransactionAttempt> S save(S attempt);
    long count();
    List<TransactionAttempt> findAllByOrderByIdDesc(Pageable pageable);
    List<TransactionAttempt> findByInitiatedByOrderByIdDesc(Long userId);
}
