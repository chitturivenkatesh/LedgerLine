package com.ledgerline;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.LedgerRepository;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static com.ledgerline.TestSupport.key;
import static org.assertj.core.api.Assertions.assertThat;

/** Real threads, real MySQL connections (pool of 30), released together by a latch. No @Transactional on the test. */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {
    @Autowired TransactionService tx;
    @Autowired TestSupport t;
    @Autowired LedgerRepository ledger;

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private <T> List<Object> runConcurrently(List<Callable<T>> jobs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(jobs.size());
        CountDownLatch ready = new CountDownLatch(jobs.size()), go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<T> job : jobs) {
            futures.add(pool.submit(() -> {
                ready.countDown(); go.await();
                try { return job.call(); } catch (Exception e) { return e; }
            }));
        }
        ready.await(); go.countDown();
        List<Object> results = new ArrayList<>();
        for (Future<Object> f : futures) results.add(f.get(60, TimeUnit.SECONDS));
        pool.shutdown();
        return results;
    }

    // 9
    @Test void twoConcurrentWithdrawalsCannotOverdraw() throws Exception {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        List<Callable<TransactionResult>> jobs = List.of(
                () -> tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("80")), key()),
                () -> tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("80")), key()));
        List<Object> results = runConcurrently(jobs);
        long ok = results.stream().filter(r -> r instanceof TransactionResult).count();
        long refused = results.stream().filter(r -> r instanceof BusinessException be && be.getCode().equals("INSUFFICIENT_FUNDS")).count();
        assertThat(ok).isEqualTo(1);
        assertThat(refused).isEqualTo(1);
        assertThat(t.balance(a.getId())).isEqualByComparingTo("20");
    }

    @Test void manyConcurrentWithdrawalsNeverGoNegative() throws Exception {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        List<Callable<TransactionResult>> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) jobs.add(() -> tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("30")), key()));
        List<Object> results = runConcurrently(jobs);
        long ok = results.stream().filter(r -> r instanceof TransactionResult).count();
        assertThat(ok).isEqualTo(3); // 100 / 30 -> exactly three succeed
        assertThat(t.balance(a.getId())).isEqualByComparingTo("10");
    }

    // 10: opposite-direction transfers are the classic deadlock recipe; ordered locking must prevent it.
    @Test void concurrentOppositeTransfersConserveMoneyAndDoNotDeadlock() throws Exception {
        User u = t.newUser(Role.ACCOUNT_HOLDER); User v = t.newUser(Role.ACCOUNT_HOLDER);
        Account a = t.newAccount(u, "1000"); Account b = t.newAccount(v, "1000");
        List<Callable<TransactionResult>> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jobs.add(() -> tx.transfer(u.getId(), new TransferRequest(a.getId(), b.getAccountNumber(), bd("10")), key()));
            jobs.add(() -> tx.transfer(v.getId(), new TransferRequest(b.getId(), a.getAccountNumber(), bd("5")), key()));
        }
        List<Object> results = runConcurrently(jobs);
        assertThat(results).allMatch(r -> r instanceof TransactionResult);
        assertThat(t.balance(a.getId())).isEqualByComparingTo("950");  // 1000 - 100 + 50
        assertThat(t.balance(b.getId())).isEqualByComparingTo("1050"); // 1000 + 100 - 50
        assertThat(t.balance(a.getId()).add(t.balance(b.getId()))).isEqualByComparingTo("2000");
    }

    // Duplicate-request race: 8 simultaneous identical requests -> exactly one transaction.
    @Test void simultaneousDuplicateRequestsProcessOnce() throws Exception {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        String k = key();
        List<Callable<TransactionResult>> jobs = new ArrayList<>();
        for (int i = 0; i < 8; i++) jobs.add(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("25")), k));
        List<Object> results = runConcurrently(jobs);
        assertThat(results).allMatch(r -> r instanceof TransactionResult);
        Set<Long> ids = new HashSet<>();
        results.forEach(r -> ids.add(((TransactionResult) r).transaction().id()));
        assertThat(ids).hasSize(1);
        assertThat(t.balance(a.getId())).isEqualByComparingTo("25");
        assertThat(ledger.findByAccount_IdOrderByIdAsc(a.getId())).hasSize(1);
    }
}
