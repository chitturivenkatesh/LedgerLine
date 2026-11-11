CREATE DATABASE IF NOT EXISTS ledgerline_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ledgerline_db;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL,
  password VARCHAR(100) NOT NULL,
  role ENUM('ADMIN','ACCOUNT_HOLDER','AUDITOR') NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_number VARCHAR(20) NOT NULL,
  account_holder_id BIGINT NOT NULL,
  account_type ENUM('SAVINGS','CURRENT') NOT NULL,
  balance DECIMAL(19,4) NOT NULL DEFAULT 0,
  status ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_accounts_number UNIQUE (account_number),
  CONSTRAINT fk_accounts_holder FOREIGN KEY (account_holder_id) REFERENCES users(id),
  CONSTRAINT ck_accounts_balance_nonneg CHECK (balance >= 0),  -- no overdraft unless a later design explicitly adds one
  INDEX idx_accounts_holder (account_holder_id)
) ENGINE=InnoDB;

CREATE TABLE transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  transaction_reference VARCHAR(40) NOT NULL,
  transaction_type ENUM('DEPOSIT','WITHDRAWAL','TRANSFER','ADJUSTMENT') NOT NULL,
  source_account_id BIGINT NULL,
  destination_account_id BIGINT NULL,
  amount DECIMAL(19,4) NOT NULL,
  status ENUM('SUCCESS','FAILED') NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL,
  request_fingerprint CHAR(64) NOT NULL,   -- SHA-256 of request details, detects key reuse with different payload
  initiated_by BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_tx_reference UNIQUE (transaction_reference),
  CONSTRAINT uq_tx_idempotency UNIQUE (initiated_by, idempotency_key),
  CONSTRAINT fk_tx_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
  CONSTRAINT fk_tx_dest FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
  CONSTRAINT fk_tx_user FOREIGN KEY (initiated_by) REFERENCES users(id),
  CONSTRAINT ck_tx_amount_pos CHECK (amount > 0),
  INDEX idx_tx_source (source_account_id, created_at),
  INDEX idx_tx_dest (destination_account_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE ledger_entries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  transaction_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  entry_type ENUM('DEBIT','CREDIT') NOT NULL,
  amount DECIMAL(19,4) NOT NULL,
  balance_after DECIMAL(19,4) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_le_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
  CONSTRAINT fk_le_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT ck_le_amount_pos CHECK (amount > 0),
  INDEX idx_le_account (account_id, id),
  INDEX idx_le_tx (transaction_id)
) ENGINE=InnoDB;

CREATE TABLE transaction_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_reference VARCHAR(40) NOT NULL,
  account_id BIGINT NULL,
  initiated_by BIGINT NULL,
  transaction_type ENUM('DEPOSIT','WITHDRAWAL','TRANSFER','ADJUSTMENT') NOT NULL,
  amount DECIMAL(19,4) NULL,
  failure_reason VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ta_account FOREIGN KEY (account_id) REFERENCES accounts(id),
  CONSTRAINT fk_ta_user FOREIGN KEY (initiated_by) REFERENCES users(id),
  INDEX idx_ta_account (account_id), INDEX idx_ta_user (initiated_by)
) ENGINE=InnoDB;

CREATE TABLE account_permissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  permission_type ENUM('VIEW','DEPOSIT','WITHDRAW','TRANSFER') NOT NULL,
  CONSTRAINT uq_perm UNIQUE (user_id, account_id, permission_type),
  CONSTRAINT fk_perm_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_perm_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(60) NOT NULL,
  entity_id VARCHAR(60) NULL,
  details TEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_audit_user (user_id), INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB;

-- Append-only protection: even a buggy or compromised app cannot rewrite history through UPDATE/DELETE.
DELIMITER $$
CREATE TRIGGER trg_ledger_no_update BEFORE UPDATE ON ledger_entries FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ledger_entries is append-only'; END$$
CREATE TRIGGER trg_ledger_no_delete BEFORE DELETE ON ledger_entries FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ledger_entries is append-only'; END$$
CREATE TRIGGER trg_attempts_no_update BEFORE UPDATE ON transaction_attempts FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'transaction_attempts is append-only'; END$$
CREATE TRIGGER trg_attempts_no_delete BEFORE DELETE ON transaction_attempts FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'transaction_attempts is append-only'; END$$
CREATE TRIGGER trg_audit_no_update BEFORE UPDATE ON audit_logs FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only'; END$$
CREATE TRIGGER trg_audit_no_delete BEFORE DELETE ON audit_logs FOR EACH ROW
BEGIN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_logs is append-only'; END$$
DELIMITER ;
