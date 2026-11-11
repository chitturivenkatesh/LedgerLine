package com.ledgerline.service;

import com.ledgerline.entity.*;
import com.ledgerline.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * Failure-recording strategy: a refused request is written in its OWN transaction (REQUIRES_NEW), after the money
 * transaction has already rolled back. So a failure record can never commit part of a money movement, and a rolled-back
 * money movement can never swallow the failure record.
 */
@Service
public class AttemptRecorder {
    private static final BigDecimal MAX_STORABLE = new BigDecimal("999999999999999");
    private final TransactionAttemptRepository attempts;
    private final AccountRepository accounts;
    private final UserRepository users;

    public AttemptRecorder(TransactionAttemptRepository attempts, AccountRepository accounts, UserRepository users) {
        this.attempts = attempts; this.accounts = accounts; this.users = users;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String requestReference, TransactionCommand c, String failureReason) {
        TransactionAttempt a = new TransactionAttempt();
        a.setRequestReference(requestReference);
        a.setAccountId(c.accountId() != null && accounts.existsById(c.accountId()) ? c.accountId() : null);
        a.setInitiatedBy(c.actorId() != null && users.existsById(c.actorId()) ? c.actorId() : null);
        a.setTransactionType(c.type());
        BigDecimal amt = c.rawAmount();
        a.setAmount(amt != null && amt.abs().compareTo(MAX_STORABLE) < 0 ? amt.setScale(4, java.math.RoundingMode.DOWN) : null);
        a.setFailureReason(failureReason.length() > 255 ? failureReason.substring(0, 255) : failureReason);
        attempts.save(a);
    }
}
