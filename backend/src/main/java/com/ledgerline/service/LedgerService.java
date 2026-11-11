package com.ledgerline.service;

import com.ledgerline.dto.LedgerEntryDTO;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import com.ledgerline.util.DtoMapper;
import com.ledgerline.util.Paging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Read-only by construction: LedgerRepository exposes no update or delete. */
@Service
public class LedgerService {
    private final LedgerRepository ledger;
    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final AccountAccessService access;
    private final TransactionService txService;

    public LedgerService(LedgerRepository ledger, AccountRepository accounts, TransactionRepository txs,
                         AccountAccessService access, TransactionService txService) {
        this.ledger = ledger; this.accounts = accounts; this.txs = txs; this.access = access; this.txService = txService;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> all(Long actorId, int page, int size) {
        User u = access.requireUser(actorId);
        if (u.getRole() == Role.ACCOUNT_HOLDER) throw BusinessException.forbidden("FORBIDDEN", "Only administrators and auditors can view the full ledger");
        return ledger.findAllByOrderByIdDesc(Paging.of(page, size)).stream().map(DtoMapper::ledger).toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> forAccount(Long actorId, Long accountId) {
        User u = access.requireUser(actorId);
        Account a = accounts.findById(accountId).orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        access.requireView(u, a);
        return ledger.findByAccount_IdOrderByIdAsc(accountId).stream().map(DtoMapper::ledger).toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDTO> forTransaction(Long actorId, Long transactionId) {
        User u = access.requireUser(actorId);
        Transaction t = txs.findById(transactionId).orElseThrow(() -> BusinessException.notFound("TRANSACTION_NOT_FOUND", "Transaction not found"));
        txService.requireCanSee(u, t);
        return ledger.findByTransaction_IdOrderByIdAsc(transactionId).stream().map(DtoMapper::ledger).toList();
    }
}
