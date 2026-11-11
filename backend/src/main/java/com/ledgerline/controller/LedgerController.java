package com.ledgerline.controller;

import com.ledgerline.dto.LedgerEntryDTO;
import com.ledgerline.security.AppUserPrincipal;
import com.ledgerline.service.LedgerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** GET only. There is intentionally no endpoint that can change a ledger entry. */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {
    private final LedgerService ledger;
    public LedgerController(LedgerService ledger) { this.ledger = ledger; }

    @GetMapping
    public List<LedgerEntryDTO> all(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ledger.all(p.getId(), page, size);
    }

    @GetMapping("/account/{accountId}")
    public List<LedgerEntryDTO> byAccount(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long accountId) {
        return ledger.forAccount(p.getId(), accountId);
    }

    @GetMapping("/transaction/{transactionId}")
    public List<LedgerEntryDTO> byTransaction(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long transactionId) {
        return ledger.forTransaction(p.getId(), transactionId);
    }
}
