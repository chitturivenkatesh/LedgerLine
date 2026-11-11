package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.security.AppUserPrincipal;
import com.ledgerline.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;
    private final LedgerService ledger;
    public AccountController(AccountService accounts, LedgerService ledger) { this.accounts = accounts; this.ledger = ledger; }

    @PostMapping
    public ResponseEntity<AccountDTO> create(@AuthenticationPrincipal AppUserPrincipal p, @Valid @RequestBody CreateAccountRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.create(p.getId(), r));
    }

    @GetMapping
    public List<AccountDTO> list(@AuthenticationPrincipal AppUserPrincipal p) { return accounts.list(p.getId()); }

    @GetMapping("/my-accounts")
    public List<AccountDTO> mine(@AuthenticationPrincipal AppUserPrincipal p) { return accounts.myAccounts(p.getId()); }

    @GetMapping("/{id}")
    public AccountDTO get(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id) { return accounts.get(p.getId(), id); }

    @GetMapping("/{id}/balance")
    public Map<String, Object> balance(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id) {
        BigDecimal b = accounts.balance(p.getId(), id);
        return Map.of("accountId", id, "balance", b);
    }

    @GetMapping("/{id}/statement")
    public List<LedgerEntryDTO> statement(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id) {
        return ledger.forAccount(p.getId(), id);
    }

    @PutMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public AccountDTO status(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id, @Valid @RequestBody UpdateAccountStatusRequest r) {
        return accounts.updateStatus(p.getId(), id, r.status());
    }

    @GetMapping("/{id}/permissions") @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public List<PermissionDTO> permissions(@PathVariable Long id) { return accounts.permissions(id); }

    @PostMapping("/{id}/permissions") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionDTO> grant(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id, @Valid @RequestBody PermissionRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.grant(p.getId(), id, r));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id, @PathVariable Long permissionId) {
        accounts.revoke(p.getId(), id, permissionId);
        return ResponseEntity.noContent().build();
    }
}
