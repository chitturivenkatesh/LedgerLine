package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.encoder = encoder; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> list() { return users.findAll().stream().map(UserDTO::from).toList(); }

    @Transactional(readOnly = true)
    public UserDTO get(Long id) { return UserDTO.from(find(id)); }

    @Transactional
    public UserDTO create(Long actorId, AdminCreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw BusinessException.conflict("EMAIL_EXISTS", "An account with this email already exists");
        User u = new User();
        u.setFullName(req.fullName().trim()); u.setEmail(email);
        u.setPassword(encoder.encode(req.password())); u.setRole(req.role());
        u = users.save(u);
        audit.log(actorId, "USER_CREATED", "USER", String.valueOf(u.getId()), "role=" + u.getRole());
        return UserDTO.from(u);
    }

    @Transactional
    public UserDTO update(Long actorId, Long id, UpdateUserRequest req) {
        User u = find(id);
        boolean self = u.getId().equals(actorId);
        if (self && ((req.role() != null && req.role() != u.getRole()) || Boolean.FALSE.equals(req.enabled())))
            throw BusinessException.badRequest("SELF_LOCKOUT", "You cannot change your own role or disable yourself");
        if (req.fullName() != null) u.setFullName(req.fullName().trim());
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        audit.log(actorId, "USER_UPDATED", "USER", String.valueOf(id), "role=" + u.getRole() + " enabled=" + u.isEnabled());
        return UserDTO.from(u);
    }

    /** Users own ledger history, so they are never physically deleted: DELETE disables the user. */
    @Transactional
    public void disable(Long actorId, Long id) {
        if (id.equals(actorId)) throw BusinessException.badRequest("SELF_LOCKOUT", "You cannot disable yourself");
        User u = find(id);
        u.setEnabled(false);
        audit.log(actorId, "USER_DISABLED", "USER", String.valueOf(id), null);
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
    }
}
