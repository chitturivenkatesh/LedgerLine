package com.ledgerline.repository;

import com.ledgerline.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AccountPermissionRepository extends JpaRepository<AccountPermission, Long> {
    boolean existsByUser_IdAndAccount_Id(Long userId, Long accountId);
    boolean existsByUser_IdAndAccount_IdAndPermissionType(Long userId, Long accountId, PermissionType type);
    List<AccountPermission> findByAccount_Id(Long accountId);
}
