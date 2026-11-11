package com.ledgerline.controller;

import com.ledgerline.dto.*;
import com.ledgerline.security.AppUserPrincipal;
import com.ledgerline.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }

    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> list() { return users.list(); }

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> create(@AuthenticationPrincipal AppUserPrincipal p, @Valid @RequestBody AdminCreateUserRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(users.create(p.getId(), r));
    }

    @GetMapping("/{id}") @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public UserDTO get(@PathVariable Long id) { return users.get(id); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public UserDTO update(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id, @Valid @RequestBody UpdateUserRequest r) {
        return users.update(p.getId(), id, r);
    }

    /** Disables the user (history must be preserved). */
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal p, @PathVariable Long id) {
        users.disable(p.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
