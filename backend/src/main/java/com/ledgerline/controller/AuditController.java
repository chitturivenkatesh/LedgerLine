package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.repository.*;
import com.ledgerline.service.AuditService;
import com.ledgerline.util.DtoMapper;
import com.ledgerline.util.Paging;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class AuditController {
    private final AuditService audit;
    private final TransactionAttemptRepository attempts;
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository txs;
    private final LedgerRepository ledger;

    public AuditController(AuditService audit, TransactionAttemptRepository attempts, UserRepository users,
                           AccountRepository accounts, TransactionRepository txs, LedgerRepository ledger) {
        this.audit = audit; this.attempts = attempts; this.users = users; this.accounts = accounts; this.txs = txs; this.ledger = ledger;
    }

    @GetMapping("/logs")
    public List<AuditLogDTO> logs(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return audit.list(page, size);
    }

    @GetMapping("/logs/{id}")
    public AuditLogDTO log(@PathVariable Long id) { return audit.get(id); }

    @GetMapping("/attempts")
    public List<AttemptDTO> attempts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return attempts.findAllByOrderByIdDesc(Paging.of(page, size)).stream().map(DtoMapper::attempt).toList();
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public StatsDTO stats() {
        return new StatsDTO(users.count(), accounts.count(), txs.count(), attempts.count(), ledger.count(), accounts.totalBalance());
    }
}
