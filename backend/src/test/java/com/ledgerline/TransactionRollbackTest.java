package com.ledgerline;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.repository.*;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackTest {
    @Autowired TransactionService tx;
    @Autowired TestSupport t;
    @Autowired TransactionRepository txRepo;
    @Autowired TransactionAttemptRepository attempts;
    @MockitoSpyBean LedgerRepository ledger;

    // 14: inject a failure on the SECOND ledger write (after the debit entry and both balance changes were staged).
    @Test void failureAfterDebitRollsBackEverythingAndIsRecorded() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); User v = t.newUser(Role.ACCOUNT_HOLDER);
        Account a = t.newAccount(u, "300"); Account b = t.newAccount(v, "0");

        AtomicInteger calls = new AtomicInteger();
        doAnswer(inv -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("injected failure");
            return inv.callRealMethod();
        }).when(ledger).save(any(LedgerEntry.class));

        String key = TestSupport.key();
        assertThatThrownBy(() -> tx.transfer(u.getId(), new TransferRequest(a.getId(), b.getAccountNumber(), new BigDecimal("100")), key))
                .isInstanceOf(IllegalStateException.class);

        assertThat(t.balance(a.getId())).isEqualByComparingTo("300");   // debit rolled back
        assertThat(t.balance(b.getId())).isEqualByComparingTo("0");     // credit never applied
        assertThat(txRepo.findByInitiatedBy_IdAndIdempotencyKey(u.getId(), key)).isEmpty(); // transactions row rolled back
        assertThat(attempts.findByInitiatedByOrderByIdDesc(u.getId()))
                .anyMatch(x -> x.getFailureReason().startsWith("INTERNAL_ERROR")); // failure recorded in its own tx
    }
}
