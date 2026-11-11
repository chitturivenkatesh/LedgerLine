package com.ledgerline.util;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import java.math.BigDecimal;
import java.util.List;

/** Entities never leave the service layer; everything the API returns is built here. */
public final class DtoMapper {
    private DtoMapper() {}

    public static AccountDTO account(Account a) {
        return new AccountDTO(a.getId(), a.getAccountNumber(), a.getAccountType(), a.getBalance(), a.getStatus(),
                a.getHolder().getId(), a.getHolder().getFullName(), a.getCreatedAt());
    }

    public static TransactionDTO transaction(Transaction t, List<LedgerEntry> entries) {
        BigDecimal srcAfter = null, dstAfter = null;
        if (entries != null) {
            for (LedgerEntry e : entries) {
                if (e.getEntryType() == EntryType.DEBIT) srcAfter = e.getBalanceAfter();
                else dstAfter = e.getBalanceAfter();
            }
        }
        Account s = t.getSourceAccount(), d = t.getDestinationAccount();
        return new TransactionDTO(t.getId(), t.getTransactionReference(), t.getTransactionType(), t.getAmount(),
                t.getStatus(), s == null ? null : s.getId(), s == null ? null : s.getAccountNumber(),
                d == null ? null : d.getId(), d == null ? null : d.getAccountNumber(),
                t.getInitiatedBy().getId(), t.getInitiatedBy().getFullName(), t.getCreatedAt(), srcAfter, dstAfter);
    }

    public static LedgerEntryDTO ledger(LedgerEntry e) {
        Transaction t = e.getTransaction();
        return new LedgerEntryDTO(e.getId(), t.getId(), t.getTransactionReference(), t.getTransactionType(),
                e.getAccount().getId(), e.getAccount().getAccountNumber(), e.getEntryType(), e.getAmount(),
                e.getBalanceAfter(), t.getInitiatedBy().getFullName(), e.getCreatedAt());
    }

    public static AuditLogDTO audit(AuditLog l) {
        return new AuditLogDTO(l.getId(), l.getUserId(), l.getAction(), l.getEntityType(), l.getEntityId(), l.getDetails(), l.getCreatedAt());
    }

    public static AttemptDTO attempt(TransactionAttempt a) {
        return new AttemptDTO(a.getId(), a.getRequestReference(), a.getAccountId(), a.getInitiatedBy(),
                a.getTransactionType(), a.getAmount(), a.getFailureReason(), a.getCreatedAt());
    }
}
