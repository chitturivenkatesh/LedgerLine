package com.ledgerline.service;

import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * THE money-moving transaction boundary. One @Transactional method = one database transaction containing:
 * the transactions row, the account balance updates, the ledger entries and the audit row.
 * Any RuntimeException rolls back all of them together. No partial transfer can exist.
 */
@Service
public class TransactionProcessor {
    @PersistenceContext private EntityManager em;
    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final LedgerRepository ledger;
    private final AccountAccessService access;
    private final AuditService audit;

    public TransactionProcessor(AccountRepository accounts, TransactionRepository txs, LedgerRepository ledger,
                                AccountAccessService access, AuditService audit) {
        this.accounts = accounts; this.txs = txs; this.ledger = ledger; this.access = access; this.audit = audit;
    }

    @Transactional
    public Transaction process(TransactionCommand c, BigDecimal amount, String fingerprint) {
        User actor = access.requireUser(c.actorId());
        if (c.accountId() == null) throw BusinessException.badRequest("ACCOUNT_REQUIRED", "accountId is required");
        Account primary = accounts.findById(c.accountId())
                .orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));

        // ---- 1. Authorization (identity from the JWT, never from the body) ----
        Account source = null, dest = null;
        switch (c.type()) {
            case DEPOSIT -> {
                requireHolderRole(actor);
                dest = primary;
                requirePermission(actor, dest, PermissionType.DEPOSIT);
            }
            case WITHDRAWAL -> {
                requireHolderRole(actor);
                source = primary;
                requirePermission(actor, source, PermissionType.WITHDRAW);
            }
            case TRANSFER -> {
                requireHolderRole(actor);
                source = primary;
                requirePermission(actor, source, PermissionType.TRANSFER);
                String num = c.counterpartyNumber() == null ? "" : c.counterpartyNumber().trim();
                dest = accounts.findByAccountNumber(num)
                        .orElseThrow(() -> BusinessException.notFound("DESTINATION_NOT_FOUND", "Destination account not found"));
                if (dest.getId().equals(source.getId()))
                    throw BusinessException.badRequest("SAME_ACCOUNT", "Source and destination must differ");
            }
            case ADJUSTMENT -> {
                if (actor.getRole() != Role.ADMIN) throw BusinessException.forbidden("FORBIDDEN", "Only administrators may post adjustments");
                if (c.reason() == null || c.reason().isBlank()) throw BusinessException.badRequest("REASON_REQUIRED", "An adjustment needs a reason");
                if (c.direction() == AdjustmentDirection.CREDIT) dest = primary; else source = primary;
            }
        }

        // ---- 2. Insert the transaction row FIRST. The unique (initiated_by, idempotency_key) index makes a concurrent
        //         duplicate request fail here (or wait for us to commit, then fail) before it can touch any balance. ----
        Transaction t = new Transaction();
        t.setTransactionReference(newReference());
        t.setTransactionType(c.type());
        t.setSourceAccount(source);
        t.setDestinationAccount(dest);
        t.setAmount(amount);
        t.setStatus(TransactionStatus.SUCCESS);
        t.setIdempotencyKey(c.idempotencyKey());
        t.setRequestFingerprint(fingerprint);
        t.setInitiatedBy(actor);
        t = txs.saveAndFlush(t);

        // ---- 3. Pessimistic row locks in ASCENDING id order (consistent order => no lock-order deadlocks).
        //         refresh(...) forces SELECT ... FOR UPDATE and overwrites any stale copy loaded for authorization. ----
        List<Long> ids = new ArrayList<>();
        if (source != null) ids.add(source.getId());
        if (dest != null) ids.add(dest.getId());
        Collections.sort(ids);
        for (Long id : ids) {
            Account a = em.find(Account.class, id);
            em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
        }

        // ---- 4. Validate against the LOCKED, current balances, then apply. Same transaction => no check/act gap. ----
        if (source != null) {
            requireActive(source);
            if (source.getBalance().compareTo(amount) < 0)
                throw BusinessException.unprocessable("INSUFFICIENT_FUNDS", "Insufficient funds");
        }
        if (dest != null) requireActive(dest);

        if (source != null) {
            source.setBalance(source.getBalance().subtract(amount));
            ledger.save(entry(t, source, EntryType.DEBIT, amount));
        }
        if (dest != null) {
            dest.setBalance(dest.getBalance().add(amount));
            ledger.save(entry(t, dest, EntryType.CREDIT, amount));
        }

        audit.log(actor.getId(), "TRANSACTION_" + c.type(), "TRANSACTION", t.getTransactionReference(),
                "amount=" + amount.toPlainString() + " source=" + (source == null ? "-" : source.getId())
                        + " dest=" + (dest == null ? "-" : dest.getId())
                        + (c.type() == TransactionType.ADJUSTMENT ? " reason=" + c.reason() : ""));
        return t;
    }

    private LedgerEntry entry(Transaction t, Account a, EntryType type, BigDecimal amount) {
        LedgerEntry e = new LedgerEntry();
        e.setTransaction(t); e.setAccount(a); e.setEntryType(type);
        e.setAmount(amount); e.setBalanceAfter(a.getBalance());
        return e;
    }

    private void requireHolderRole(User u) {
        if (u.getRole() != Role.ACCOUNT_HOLDER)
            throw BusinessException.forbidden("FORBIDDEN", "Your role may not move money");
    }

    private void requirePermission(User u, Account a, PermissionType p) {
        if (!access.hasPermission(u, a, p))
            throw BusinessException.forbidden("FORBIDDEN", "You are not permitted to " + p + " on this account");
    }

    private void requireActive(Account a) {
        if (a.getStatus() != AccountStatus.ACTIVE)
            throw BusinessException.unprocessable("ACCOUNT_NOT_ACTIVE", "Account " + a.getAccountNumber() + " is " + a.getStatus());
    }

    private String newReference() {
        return "TXN-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
