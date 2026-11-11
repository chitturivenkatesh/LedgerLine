package com.ledgerline.service;

import com.ledgerline.dto.AuditLogDTO;
import com.ledgerline.entity.AuditLog;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.AuditRepository;
import com.ledgerline.util.DtoMapper;
import com.ledgerline.util.Paging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AuditService {
    private final AuditRepository repo;
    public AuditService(AuditRepository repo) { this.repo = repo; }

    /** Joins the caller's transaction: an audit row exists if and only if the audited change committed. */
    @Transactional
    public void log(Long userId, String action, String entityType, String entityId, String details) {
        AuditLog l = new AuditLog();
        l.setUserId(userId); l.setAction(action); l.setEntityType(entityType); l.setEntityId(entityId);
        l.setDetails(details == null ? null : (details.length() > 2000 ? details.substring(0, 2000) : details));
        repo.save(l);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> list(int page, int size) {
        return repo.findAllByOrderByIdDesc(Paging.of(page, size)).stream().map(DtoMapper::audit).toList();
    }

    @Transactional(readOnly = true)
    public AuditLogDTO get(Long id) {
        return repo.findById(id).map(DtoMapper::audit)
                .orElseThrow(() -> BusinessException.notFound("AUDIT_NOT_FOUND", "Audit log not found"));
    }
}
