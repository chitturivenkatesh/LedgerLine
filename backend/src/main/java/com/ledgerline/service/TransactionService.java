package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import com.ledgerline.util.DtoMapper;
import com.ledgerline.util.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Orchestrator. Deliberately NOT @Transactional: it must be able to (a) read for idempotency, (b) run the money
 * transaction in TransactionProcessor, and (c) write a failure record AFTER that transaction rolled back.
 */
@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    private static final int MAX_ATTEMPTS = 3;

    private final TransactionProcessor processor;
    private final AttemptRecorder recorder;
    private final TransactionRepository txs;
    private final LedgerRepository ledger;
    private final AccountRepository accounts;
    private final AccountAccessService access;

    public TransactionService(TransactionProcessor processor, AttemptRecorder recorder, TransactionRepository txs,
                              LedgerRepository ledger, AccountRepository accounts, AccountAccessService access) {
        this.processor = processor; this.recorder = recorder; this.txs = txs; this.ledger = ledger;
        this.accounts = accounts; this.access = access;
    }

    // ---------------- write side ----------------
    public TransactionResult deposit(Long actorId, DepositRequest r, String key) {
        return run(new TransactionCommand(TransactionType.DEPOSIT, actorId, r.accountId(), null, null, null, r.amount(), key));
    }

    public TransactionResult withdraw(Long actorId, WithdrawRequest r, String key) {
        return run(new TransactionCommand(TransactionType.WITHDRAWAL, actorId, r.accountId(), null, null, null, r.amount(), key));
    }

    public TransactionResult transfer(Long actorId, TransferRequest r, String key) {
        return run(new TransactionCommand(TransactionType.TRANSFER, actorId, r.sourceAccountId(), r.destinationAccountNumber(), null, null, r.amount(), key));
    }

    public TransactionResult adjust(Long actorId, AdjustmentRequest r, String key) {
        return run(new TransactionCommand(TransactionType.ADJUSTMENT, actorId, r.accountId(), null, r.direction(), r.reason(), r.amount(), key));
    }

    private TransactionResult run(TransactionCommand c) {
        String requestRef = "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        try {
            validateKey(c.idempotencyKey());
            BigDecimal amount = validateAmount(c.rawAmount());
            String fingerprint = fingerprint(c, amount);

            Optional<Transaction> existing = txs.findByInitiatedBy_IdAndIdempotencyKey(c.actorId(), c.idempotencyKey());
            if (existing.isPresent()) return replay(existing.get(), fingerprint);

            try {
                Transaction t = executeWithRetry(c, amount, fingerprint);
                return new TransactionResult(withBalances(t), false);
            } catch (DataIntegrityViolationException dup) {
                // A simultaneous request with the same key won the unique index. Return ITS result, never process twice.
                Optional<Transaction> winner = txs.findByInitiatedBy_IdAndIdempotencyKey(c.actorId(), c.idempotencyKey());
                if (winner.isPresent()) return replay(winner.get(), fingerprint);
                throw dup;
            }
        } catch (BusinessException e) {
            safeRecord(requestRef, c, e.getCode() + ": " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Transaction {} failed unexpectedly", requestRef, e);
            safeRecord(requestRef, c, "INTERNAL_ERROR: " + e.getClass().getSimpleName());
            throw e;
        }
    }

    /** Retries only database lock failures (deadlock victim, lock wait timeout). The failed attempt rolled back fully. */
    private Transaction executeWithRetry(TransactionCommand c, BigDecimal amount, String fingerprint) {
        for (int attempt = 1; ; attempt++) {
            try {
                return processor.process(c, amount, fingerprint);
            } catch (PessimisticLockingFailureException e) {
                if (attempt >= MAX_ATTEMPTS) throw e;
                log.warn("Lock failure, retry {}/{}", attempt, MAX_ATTEMPTS);
            }
        }
    }

    private TransactionResult replay(Transaction t, String fingerprint) {
        if (!t.getRequestFingerprint().equals(fingerprint))
            throw BusinessException.conflict("IDEMPOTENCY_KEY_REUSED", "This Idempotency-Key was already used with different request details");
        return new TransactionResult(withBalances(t), true);
    }

    private void safeRecord(String ref, TransactionCommand c, String reason) {
        try { recorder.record(ref, c, reason); }
        catch (RuntimeException ex) { log.error("Could not record failed attempt {}", ref, ex); }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) throw BusinessException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        if (key.length() > 100) throw BusinessException.badRequest("IDEMPOTENCY_KEY_INVALID", "Idempotency-Key must be at most 100 characters");
    }

    static BigDecimal validateAmount(BigDecimal a) {
        if (a == null) throw BusinessException.badRequest("AMOUNT_REQUIRED", "Amount is required");
        if (a.signum() <= 0) throw BusinessException.badRequest("AMOUNT_NOT_POSITIVE", "Amount must be greater than zero");
        if (a.stripTrailingZeros().scale() > 2) throw BusinessException.badRequest("AMOUNT_PRECISION", "Amount can have at most 2 decimal places");
        if (a.compareTo(MAX_AMOUNT) > 0) throw BusinessException.badRequest("AMOUNT_TOO_LARGE", "Amount exceeds the maximum allowed");
        return a.setScale(2);
    }

    /** SHA-256 over the request's meaningful fields. Same key + different fingerprint = client bug or attack. */
    static String fingerprint(TransactionCommand c, BigDecimal amount) {
        String s = String.join("|", String.valueOf(c.type()), String.valueOf(c.accountId()),
                c.counterpartyNumber() == null ? "" : c.counterpartyNumber().trim(),
                amount.toPlainString(), String.valueOf(c.direction()), c.reason() == null ? "" : c.reason().trim());
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private TransactionDTO withBalances(Transaction t) {
        return DtoMapper.transaction(t, ledger.findByTransaction_IdOrderByIdAsc(t.getId()));
    }

    // ---------------- read side ----------------
    public List<TransactionDTO> list(Long actorId, int page, int size) {
        User u = access.requireUser(actorId);
        var pageable = Paging.of(page, size);
        List<Transaction> list;
        if (u.getRole() == Role.ACCOUNT_HOLDER) {
            List<Long> ids = accounts.findAccessibleTo(u.getId()).stream().map(Account::getId).toList();
            list = ids.isEmpty() ? List.of() : txs.findInvolving(ids, pageable);
        } else {
            list = txs.findAllByOrderByIdDesc(pageable);
        }
        return list.stream().map(t -> DtoMapper.transaction(t, null)).toList();
    }

    public TransactionDTO get(Long actorId, Long id) {
        User u = access.requireUser(actorId);
        Transaction t = txs.findById(id).orElseThrow(() -> BusinessException.notFound("TRANSACTION_NOT_FOUND", "Transaction not found"));
        requireCanSee(u, t);
        return withBalances(t);
    }

    public List<TransactionDTO> forAccount(Long actorId, Long accountId, int page, int size) {
        User u = access.requireUser(actorId);
        Account a = accounts.findById(accountId).orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        access.requireView(u, a);
        return txs.findInvolving(List.of(accountId), Paging.of(page, size)).stream().map(t -> DtoMapper.transaction(t, null)).toList();
    }

    void requireCanSee(User u, Transaction t) {
        if (u.getRole() != Role.ACCOUNT_HOLDER) return;
        boolean ok = (t.getSourceAccount() != null && access.canView(u, t.getSourceAccount()))
                || (t.getDestinationAccount() != null && access.canView(u, t.getDestinationAccount()));
        if (!ok) throw BusinessException.forbidden("FORBIDDEN", "You do not have access to this transaction");
    }
}
