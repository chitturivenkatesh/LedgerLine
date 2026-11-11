package com.ledgerline.security;

import com.ledgerline.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public CustomUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(email.toLowerCase())
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));
    }
}
