package com.ledgerline.service;

import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import org.springframework.stereotype.Service;

/** Single place that answers "may this user do X to this account?". Always evaluated server-side. */
@Service
public class AccountAccessService {
    private final AccountPermissionRepository perms;
    private final UserRepository users;

    public AccountAccessService(AccountPermissionRepository perms, UserRepository users) {
        this.perms = perms; this.users = users;
    }

    public User requireUser(Long id) {
        User u = users.findById(id).orElseThrow(() -> BusinessException.forbidden("USER_NOT_FOUND", "Unknown user"));
        if (!u.isEnabled()) throw BusinessException.forbidden("USER_DISABLED", "User is disabled");
        return u;
    }

    public boolean isOwner(User u, Account a) { return a.getHolder().getId().equals(u.getId()); }

    public boolean canView(User u, Account a) {
        if (u.getRole() == Role.ADMIN || u.getRole() == Role.AUDITOR) return true;
        return isOwner(u, a) || perms.existsByUser_IdAndAccount_Id(u.getId(), a.getId());
    }

    /** Owners implicitly hold every permission on their own account; others need an explicit row. */
    public boolean hasPermission(User u, Account a, PermissionType type) {
        if (isOwner(u, a)) return true;
        return perms.existsByUser_IdAndAccount_IdAndPermissionType(u.getId(), a.getId(), type);
    }

    public void requireView(User u, Account a) {
        if (!canView(u, a)) throw BusinessException.forbidden("FORBIDDEN", "You do not have access to this account");
    }
}
