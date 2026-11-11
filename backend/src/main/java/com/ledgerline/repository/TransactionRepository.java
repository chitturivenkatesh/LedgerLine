package com.ledgerline.repository;

import com.ledgerline.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByInitiatedBy_IdAndIdempotencyKey(Long userId, String key);
    List<Transaction> findAllByOrderByIdDesc(Pageable pageable);

    @Query("select t from Transaction t where t.sourceAccount.id in :ids or t.destinationAccount.id in :ids order by t.id desc")
    List<Transaction> findInvolving(@Param("ids") Collection<Long> accountIds, Pageable pageable);
}
