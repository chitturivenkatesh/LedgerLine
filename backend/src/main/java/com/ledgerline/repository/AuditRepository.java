package com.ledgerline.repository;

import com.ledgerline.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import java.util.*;

public interface AuditRepository extends Repository<AuditLog, Long> {
    <S extends AuditLog> S save(S log);
    Optional<AuditLog> findById(Long id);
    List<AuditLog> findAllByOrderByIdDesc(Pageable pageable);
}
