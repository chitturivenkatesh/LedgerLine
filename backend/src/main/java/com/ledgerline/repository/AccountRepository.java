package com.ledgerline.repository;

import com.ledgerline.entity.Account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);

    @Query("select distinct a from Account a where a.holder.id = :uid " +
           "or a.id in (select p.account.id from AccountPermission p where p.user.id = :uid) order by a.id")
    List<Account> findAccessibleTo(@Param("uid") Long userId);

    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal totalBalance();
}
