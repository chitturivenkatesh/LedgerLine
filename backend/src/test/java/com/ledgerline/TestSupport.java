package com.ledgerline;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.repository.*;
import com.ledgerline.security.JwtService;
import com.ledgerline.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/** Creates unique users/accounts per test, so tests never depend on (or need to delete) each other's rows. */
@Component
public class TestSupport {
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired AccountService accountService;
    @Autowired TransactionService txService;
    @Autowired JwtService jwt;

    public User newUser(Role role) {
        User u = new User();
        u.setFullName("Test " + role);
        u.setEmail(UUID.randomUUID().toString().substring(0, 12) + "@test.local");
        u.setPassword(encoder.encode("password123"));
        u.setRole(role);
        return users.save(u);
    }

    public Account newAccount(User holder, String openingBalance) {
        AccountDTO dto = accountService.create(holder.getId(), new CreateAccountRequest(AccountType.SAVINGS, null));
        if (new BigDecimal(openingBalance).signum() > 0)
            txService.deposit(holder.getId(), new DepositRequest(dto.id(), new BigDecimal(openingBalance)), key());
        return accounts.findById(dto.id()).orElseThrow();
    }

    public BigDecimal balance(Long accountId) { return accounts.findById(accountId).orElseThrow().getBalance(); }
    public String token(User u) { return jwt.generateToken(u.getEmail(), u.getRole().name()); }
    public static String key() { return UUID.randomUUID().toString(); }
}
