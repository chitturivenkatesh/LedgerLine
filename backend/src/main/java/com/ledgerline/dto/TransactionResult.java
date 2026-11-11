package com.ledgerline.dto;

public record TransactionResult(TransactionDTO transaction, boolean replayed) {}
