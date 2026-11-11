package com.ledgerline;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.*;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import static com.ledgerline.TestSupport.key;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {
    @Autowired TransactionService tx;
    @Autowired TestSupport t;
    @Autowired LedgerRepository ledger;
    @Autowired TransactionAttemptRepository attempts;

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static void assertCode(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }

    // 1
    @Test void successfulDeposit() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        TransactionResult r = tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("250.50")), key());
        assertThat(r.transaction().status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(t.balance(a.getId())).isEqualByComparingTo("250.50");
        assertThat(ledger.findByTransaction_IdOrderByIdAsc(r.transaction().id())).hasSize(1);
    }

    // 2
    @Test void successfulWithdrawal() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "500");
        tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("120.25")), key());
        assertThat(t.balance(a.getId())).isEqualByComparingTo("379.75");
    }

    // 3 (also checks double-entry: one debit + one credit, same transaction, equal amounts)
    @Test void successfulTransfer() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); User v = t.newUser(Role.ACCOUNT_HOLDER);
        Account a = t.newAccount(u, "300"); Account b = t.newAccount(v, "0");
        TransactionResult r = tx.transfer(u.getId(), new TransferRequest(a.getId(), b.getAccountNumber(), bd("100")), key());
        assertThat(t.balance(a.getId())).isEqualByComparingTo("200");
        assertThat(t.balance(b.getId())).isEqualByComparingTo("100");
        List<LedgerEntry> entries = ledger.findByTransaction_IdOrderByIdAsc(r.transaction().id());
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(entries.get(1).getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(entries.get(0).getAmount()).isEqualByComparingTo(entries.get(1).getAmount());
        assertThat(r.transaction().sourceBalanceAfter()).isEqualByComparingTo("200");
        assertThat(r.transaction().destinationBalanceAfter()).isEqualByComparingTo("100");
    }

    // 4 (+ failure is recorded, balance untouched)
    @Test void insufficientBalanceIsRefusedAndRecorded() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "50");
        assertCode(() -> tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("50.01")), key()), "INSUFFICIENT_FUNDS");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("50");
        assertThat(attempts.findByInitiatedByOrderByIdDesc(u.getId()))
                .anyMatch(x -> x.getFailureReason().startsWith("INSUFFICIENT_FUNDS") && x.getAccountId().equals(a.getId()));
    }

    // 5
    @Test void negativeAmountRejected() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        assertCode(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("-5")), key()), "AMOUNT_NOT_POSITIVE");
        assertCode(() -> tx.withdraw(u.getId(), new WithdrawRequest(a.getId(), bd("-5")), key()), "AMOUNT_NOT_POSITIVE");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("100");
    }

    // 6
    @Test void zeroAmountRejected() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        assertCode(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("0.00")), key()), "AMOUNT_NOT_POSITIVE");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("100");
    }

    @Test void tooManyDecimalsRejected() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        assertCode(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("1.005")), key()), "AMOUNT_PRECISION");
    }

    // 7
    @Test void sameIdempotencyKeyTwiceProcessesOnce() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        String k = key();
        TransactionResult first = tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("40")), k);
        TransactionResult second = tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("40")), k);
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transaction().id()).isEqualTo(first.transaction().id());
        assertThat(second.transaction().transactionReference()).isEqualTo(first.transaction().transactionReference());
        assertThat(t.balance(a.getId())).isEqualByComparingTo("40");
        assertThat(ledger.findByAccount_IdOrderByIdAsc(a.getId())).hasSize(1);
    }

    // 8
    @Test void sameKeyDifferentPayloadIsRejected() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        String k = key();
        tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("40")), k);
        assertCode(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("41")), k), "IDEMPOTENCY_KEY_REUSED");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("40");
    }

    @Test void missingIdempotencyKeyRejected() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "0");
        assertCode(() -> tx.deposit(u.getId(), new DepositRequest(a.getId(), bd("1")), null), "IDEMPOTENCY_KEY_REQUIRED");
    }

    // 11 (service level)
    @Test void strangerCannotWithdrawFromSomeoneElsesAccount() {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); User thief = t.newUser(Role.ACCOUNT_HOLDER);
        Account a = t.newAccount(owner, "500");
        assertCode(() -> tx.withdraw(thief.getId(), new WithdrawRequest(a.getId(), bd("100")), key()), "FORBIDDEN");
        assertCode(() -> tx.transfer(thief.getId(), new TransferRequest(a.getId(), "NOPE", bd("1")), key()), "FORBIDDEN");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("500");
        assertThat(attempts.findByInitiatedByOrderByIdDesc(thief.getId())).hasSize(2);
    }

    @Test void auditorAndAdminCannotMoveMoneyThroughHolderEndpoints() {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(owner, "100");
        User auditor = t.newUser(Role.AUDITOR); User admin = t.newUser(Role.ADMIN);
        assertCode(() -> tx.deposit(auditor.getId(), new DepositRequest(a.getId(), bd("5")), key()), "FORBIDDEN");
        assertCode(() -> tx.withdraw(admin.getId(), new WithdrawRequest(a.getId(), bd("5")), key()), "FORBIDDEN");
        assertThat(t.balance(a.getId())).isEqualByComparingTo("100");
    }

    @Test void cannotTransferToSameAccount() {
        User u = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(u, "100");
        assertCode(() -> tx.transfer(u.getId(), new TransferRequest(a.getId(), a.getAccountNumber(), bd("1")), key()), "SAME_ACCOUNT");
    }

    @Test void adminAdjustmentRequiresAdminAndReason() {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(owner, "100");
        User admin = t.newUser(Role.ADMIN);
        assertCode(() -> tx.adjust(owner.getId(), new AdjustmentRequest(a.getId(), bd("5"), AdjustmentDirection.CREDIT, "x"), key()), "FORBIDDEN");
        assertCode(() -> tx.adjust(admin.getId(), new AdjustmentRequest(a.getId(), bd("5"), AdjustmentDirection.CREDIT, " "), key()), "REASON_REQUIRED");
        tx.adjust(admin.getId(), new AdjustmentRequest(a.getId(), bd("5"), AdjustmentDirection.CREDIT, "goodwill"), key());
        assertThat(t.balance(a.getId())).isEqualByComparingTo("105");
    }

    // 15
    @Test void moneyIsConservedAcrossManyTransfers() {
        User u = t.newUser(Role.ACCOUNT_HOLDER);
        Account[] acc = { t.newAccount(u, "1000"), t.newAccount(u, "1000"), t.newAccount(u, "1000"), t.newAccount(u, "1000") };
        BigDecimal before = BigDecimal.valueOf(4000);
        Random rnd = new Random(42);
        int ok = 0;
        for (int i = 0; i < 60; i++) {
            int from = rnd.nextInt(4), to = (from + 1 + rnd.nextInt(3)) % 4;
            BigDecimal amount = BigDecimal.valueOf(1 + rnd.nextInt(400), 0);
            try {
                tx.transfer(u.getId(), new TransferRequest(acc[from].getId(), acc[to].getAccountNumber(), amount), key());
                ok++;
            } catch (BusinessException expected) { assertThat(expected.getCode()).isEqualTo("INSUFFICIENT_FUNDS"); }
        }
        BigDecimal after = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO, credits = BigDecimal.ZERO;
        for (Account a : acc) {
            after = after.add(t.balance(a.getId()));
            for (LedgerEntry e : ledger.findByAccount_IdOrderByIdAsc(a.getId())) {
                if (e.getEntryType() == EntryType.DEBIT) debits = debits.add(e.getAmount()); else credits = credits.add(e.getAmount());
            }
        }
        assertThat(ok).isGreaterThan(10);
        assertThat(after).isEqualByComparingTo(before);
        // credits - debits over these accounts = the four opening deposits only (transfers net to zero)
        assertThat(credits.subtract(debits)).isEqualByComparingTo(before);
        // every balance equals the last ledger balance_after
        for (Account a : acc) {
            List<LedgerEntry> es = ledger.findByAccount_IdOrderByIdAsc(a.getId());
            assertThat(es.get(es.size() - 1).getBalanceAfter()).isEqualByComparingTo(t.balance(a.getId()));
        }
    }
}
