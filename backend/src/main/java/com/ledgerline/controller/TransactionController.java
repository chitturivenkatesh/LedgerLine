package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.security.AppUserPrincipal;
import com.ledgerline.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody DepositRequest r) {
        return respond(service.deposit(p.getId(), r, key));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody WithdrawRequest r) {
        return respond(service.withdraw(p.getId(), r, key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest r) {
        return respond(service.transfer(p.getId(), r, key));
    }

    /** Administrative adjustment: ADMIN only (enforced in the service), mandatory reason, audit-logged. */
    @PostMapping("/adjustment")
    public ResponseEntity<TransactionDTO> adjustment(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody AdjustmentRequest r) {
        return respond(service.adjust(p.getId(), r, key));
    }

    @GetMapping
    public List<TransactionDTO> list(@AuthenticationPrincipal AppUserPrincipal p,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return service.list(p.getId(), page, size);
    }

    @GetMapping("/{id}")
    public TransactionDTO get(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id) { return service.get(p.getId(), id); }

    @GetMapping("/account/{accountId}")
    public List<TransactionDTO> forAccount(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return service.forAccount(p.getId(), accountId, page, size);
    }

    private ResponseEntity<TransactionDTO> respond(TransactionResult r) {
        ResponseEntity.BodyBuilder b = ResponseEntity.status(r.replayed() ? HttpStatus.OK : HttpStatus.CREATED);
        if (r.replayed()) b.header("Idempotent-Replayed", "true");
        return b.body(r.transaction());
    }
}
