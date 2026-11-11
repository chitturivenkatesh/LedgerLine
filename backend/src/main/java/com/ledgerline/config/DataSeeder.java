package com.ledgerline.config;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

/** Seeds one user per role (+ a funded demo account). Password comes from SEED_PASSWORD, never from source code. */
@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seed(UserRepository users, PasswordEncoder enc, AccountService accounts, TransactionService txs,
                           @Value("${app.seed.enabled}") boolean enabled, @Value("${app.seed.password}") String password) {
        return args -> {
            if (!enabled) return;
            if (password.length() < 8) throw new IllegalStateException("SEED_PASSWORD must be at least 8 chars");
            create(users, enc, password, "System Admin", "admin@ledgerline.local", Role.ADMIN);
            create(users, enc, password, "Audit Officer", "auditor@ledgerline.local", Role.AUDITOR);
            boolean newHolder = !users.existsByEmail("holder@ledgerline.local");
            User holder = create(users, enc, password, "Demo Holder", "holder@ledgerline.local", Role.ACCOUNT_HOLDER);
            create(users, enc, password, "Second Holder", "holder2@ledgerline.local", Role.ACCOUNT_HOLDER);
            if (newHolder) {
                AccountDTO acc = accounts.create(holder.getId(), new CreateAccountRequest(AccountType.SAVINGS, null));
                txs.deposit(holder.getId(), new DepositRequest(acc.id(), new BigDecimal("10000.00")), "seed-deposit-" + holder.getId());
                log.info("Seeded demo account {} with 10000.00", acc.accountNumber());
            }
        };
    }

    private User create(UserRepository users, PasswordEncoder enc, String pw, String name, String email, Role role) {
        return users.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(name); u.setEmail(email); u.setPassword(enc.encode(pw)); u.setRole(role);
            log.info("Seeded {} user {}", role, email);
            return users.save(u);
        });
    }
}
