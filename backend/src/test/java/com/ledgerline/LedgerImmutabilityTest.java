package com.ledgerline;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.entity.*;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LedgerImmutabilityTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TestSupport t;
    @Autowired TransactionService tx;

    // 13: even raw SQL (a buggy query, a compromised app) cannot rewrite history.
    @Test void ledgerEntriesCannotBeUpdatedOrDeleted() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        var r = tx.deposit(u.getId(), new DepositRequest(a.getId(), new BigDecimal("10")), TestSupport.key());
        Long entryId = jdbc.queryForObject("select id from ledger_entries where transaction_id = ?", Long.class, r.transaction().id());

        assertThatThrownBy(() -> jdbc.update("update ledger_entries set amount = 9999 where id = ?", entryId))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from ledger_entries where id = ?", entryId))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThat(jdbc.queryForObject("select amount from ledger_entries where id = ?", BigDecimal.class, entryId))
                .isEqualByComparingTo("10");
    }

    @Test void failedAttemptsAndAuditLogsAreAlsoAppendOnly() {
        assertThatThrownBy(() -> jdbc.update("delete from transaction_attempts")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("update audit_logs set action = 'x'")).isInstanceOf(DataAccessException.class);
    }
}
