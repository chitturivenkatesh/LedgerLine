package com.ledgerline;

import com.ledgerline.entity.*;
import com.ledgerline.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired TestSupport t;
    @Autowired LedgerRepository ledger;

    private static String bearer(String token) { return "Bearer " + token; }

    @Test void noTokenMeans401() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test void tamperedTokenMeans401() throws Exception {
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer abc.def.ghi")).andExpect(status().isUnauthorized());
    }

    // 11 (HTTP level)
    @Test void holderCannotReadAnotherHoldersAccount() throws Exception {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); User other = t.newUser(Role.ACCOUNT_HOLDER);
        Account a = t.newAccount(owner, "100");
        mvc.perform(get("/api/accounts/" + a.getId() + "/balance").header("Authorization", bearer(t.token(other))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts/" + a.getId() + "/statement").header("Authorization", bearer(t.token(other))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts/" + a.getId() + "/balance").header("Authorization", bearer(t.token(owner))))
                .andExpect(status().isOk());
    }

    @Test void holderCannotUseAdminEndpoints() throws Exception {
        User h = t.newUser(Role.ACCOUNT_HOLDER);
        mvc.perform(get("/api/users").header("Authorization", bearer(t.token(h)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/ledger").header("Authorization", bearer(t.token(h)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit/logs").header("Authorization", bearer(t.token(h)))).andExpect(status().isForbidden());
    }

    // 12
    @Test void auditorCannotModifyFinancialData() throws Exception {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(owner, "100");
        User auditor = t.newUser(Role.AUDITOR);
        long entriesBefore = ledger.findByAccount_IdOrderByIdAsc(a.getId()).size();
        String body = "{\"accountId\":" + a.getId() + ",\"amount\":50}";

        mvc.perform(post("/api/transactions/deposit").header("Authorization", bearer(t.token(auditor)))
                .header("Idempotency-Key", TestSupport.key()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/transactions/withdraw").header("Authorization", bearer(t.token(auditor)))
                .header("Idempotency-Key", TestSupport.key()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/transactions/1").header("Authorization", bearer(t.token(auditor)))).andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/ledger/1").header("Authorization", bearer(t.token(auditor)))).andExpect(status().is4xxClientError());
        mvc.perform(post("/api/users").header("Authorization", bearer(t.token(auditor))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"x\",\"email\":\"x@x.com\",\"password\":\"password123\",\"role\":\"ADMIN\"}")).andExpect(status().isForbidden());

        assertThat(ledger.findByAccount_IdOrderByIdAsc(a.getId())).hasSize((int) entriesBefore);
        assertThat(t.balance(a.getId())).isEqualByComparingTo("100");
        mvc.perform(get("/api/ledger").header("Authorization", bearer(t.token(auditor)))).andExpect(status().isOk());
    }

    @Test void registrationCannotChooseRole() throws Exception {
        String email = "r" + System.nanoTime() + "@test.local";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Eve\",\"email\":\"" + email + "\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("ACCOUNT_HOLDER"));
    }

    @Test void missingIdempotencyHeaderIs400() throws Exception {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(owner, "0");
        mvc.perform(post("/api/transactions/deposit").header("Authorization", bearer(t.token(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":" + a.getId() + ",\"amount\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test void replayedRequestReturns200WithReplayHeader() throws Exception {
        User owner = t.newUser(Role.ACCOUNT_HOLDER); Account a = t.newAccount(owner, "0");
        String key = TestSupport.key(), body = "{\"accountId\":" + a.getId() + ",\"amount\":5}";
        mvc.perform(post("/api/transactions/deposit").header("Authorization", bearer(t.token(owner)))
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/transactions/deposit").header("Authorization", bearer(t.token(owner)))
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(header().string("Idempotent-Replayed", "true"));
    }
}
