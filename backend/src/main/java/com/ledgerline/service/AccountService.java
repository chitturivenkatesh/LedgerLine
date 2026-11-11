package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import com.ledgerline.util.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accounts;
    private final UserRepository users;
    private final AccountPermissionRepository perms;
    private final AccountAccessService access;
    private final AuditService audit;

    public AccountService(AccountRepository accounts, UserRepository users, AccountPermissionRepository perms,
                          AccountAccessService access, AuditService audit) {
        this.accounts = accounts; this.users = users; this.perms = perms; this.access = access; this.audit = audit;
    }

    @Transactional
    public AccountDTO create(Long actorId, CreateAccountRequest req) {
        User actor = access.requireUser(actorId);
        User holder;
        if (actor.getRole() == Role.ADMIN) {
            if (req.holderId() == null) throw BusinessException.badRequest("HOLDER_REQUIRED", "holderId is required");
            holder = users.findById(req.holderId()).orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "Holder not found"));
            if (holder.getRole() != Role.ACCOUNT_HOLDER) throw BusinessException.badRequest("INVALID_HOLDER", "Holder must have role ACCOUNT_HOLDER");
        } else if (actor.getRole() == Role.ACCOUNT_HOLDER) {
            holder = actor; // never trust a holder id from the client
        } else {
            throw BusinessException.forbidden("FORBIDDEN", "Auditors cannot create accounts");
        }
        Account a = new Account();
        a.setAccountNumber(newAccountNumber());
        a.setHolder(holder);
        a.setAccountType(req.accountType());
        a.setBalance(BigDecimal.ZERO);
        a.setStatus(AccountStatus.ACTIVE);
        a = accounts.save(a);
        audit.log(actorId, "ACCOUNT_CREATED", "ACCOUNT", String.valueOf(a.getId()), "number=" + a.getAccountNumber() + " holder=" + holder.getId());
        return DtoMapper.account(a);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> list(Long actorId) {
        User u = access.requireUser(actorId);
        List<Account> list = (u.getRole() == Role.ACCOUNT_HOLDER) ? accounts.findAccessibleTo(u.getId()) : accounts.findAll();
        return list.stream().map(DtoMapper::account).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> myAccounts(Long actorId) {
        User u = access.requireUser(actorId);
        return accounts.findAccessibleTo(u.getId()).stream().map(DtoMapper::account).toList();
    }

    @Transactional(readOnly = true)
    public AccountDTO get(Long actorId, Long accountId) {
        return DtoMapper.account(loadViewable(actorId, accountId));
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Long actorId, Long accountId) {
        return loadViewable(actorId, accountId).getBalance();
    }

    @Transactional
    public AccountDTO updateStatus(Long actorId, Long accountId, AccountStatus status) {
        Account a = accounts.findById(accountId).orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        a.setStatus(status);
        audit.log(actorId, "ACCOUNT_STATUS_CHANGED", "ACCOUNT", String.valueOf(accountId), "status=" + status);
        return DtoMapper.account(a);
    }

    @Transactional
    public PermissionDTO grant(Long actorId, Long accountId, PermissionRequest req) {
        Account a = accounts.findById(accountId).orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        User u = users.findById(req.userId()).orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
        if (u.getRole() != Role.ACCOUNT_HOLDER) throw BusinessException.badRequest("INVALID_USER", "Only ACCOUNT_HOLDER users can receive permissions");
        if (perms.existsByUser_IdAndAccount_IdAndPermissionType(u.getId(), accountId, req.permissionType()))
            throw BusinessException.conflict("PERMISSION_EXISTS", "Permission already granted");
        AccountPermission p = new AccountPermission();
        p.setUser(u); p.setAccount(a); p.setPermissionType(req.permissionType());
        p = perms.save(p);
        audit.log(actorId, "PERMISSION_GRANTED", "ACCOUNT", String.valueOf(accountId), "user=" + u.getId() + " type=" + req.permissionType());
        return new PermissionDTO(p.getId(), u.getId(), u.getFullName(), accountId, p.getPermissionType());
    }

    @Transactional
    public void revoke(Long actorId, Long accountId, Long permissionId) {
        AccountPermission p = perms.findById(permissionId).orElseThrow(() -> BusinessException.notFound("PERMISSION_NOT_FOUND", "Permission not found"));
        if (!p.getAccount().getId().equals(accountId)) throw BusinessException.notFound("PERMISSION_NOT_FOUND", "Permission not found");
        perms.delete(p);
        audit.log(actorId, "PERMISSION_REVOKED", "ACCOUNT", String.valueOf(accountId), "permission=" + permissionId);
    }

    @Transactional(readOnly = true)
    public List<PermissionDTO> permissions(Long accountId) {
        return perms.findByAccount_Id(accountId).stream()
                .map(p -> new PermissionDTO(p.getId(), p.getUser().getId(), p.getUser().getFullName(), accountId, p.getPermissionType())).toList();
    }

    private Account loadViewable(Long actorId, Long accountId) {
        User u = access.requireUser(actorId);
        Account a = accounts.findById(accountId).orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "Account not found"));
        access.requireView(u, a);
        return a;
    }

    private String newAccountNumber() {
        for (int i = 0; i < 10; i++) {
            String n = "LL" + String.format("%010d", Math.floorMod(RANDOM.nextLong(), 10_000_000_000L));
            if (!accounts.existsByAccountNumber(n)) return n;
        }
        throw new IllegalStateException("Could not allocate account number");
    }
}
