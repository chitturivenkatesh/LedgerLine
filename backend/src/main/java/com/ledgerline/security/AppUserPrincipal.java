package com.ledgerline.security;

import com.ledgerline.entity.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/** The identity of the caller comes from here (verified JWT + DB), never from request bodies. */
public class AppUserPrincipal implements UserDetails {
    private final Long id; private final String email; private final String password;
    private final Role role; private final boolean enabled;

    public AppUserPrincipal(User u) {
        this.id = u.getId(); this.email = u.getEmail(); this.password = u.getPassword();
        this.role = u.getRole(); this.enabled = u.isEnabled();
    }
    public Long getId() { return id; }
    public Role getRole() { return role; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
}
