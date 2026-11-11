package com.ledgerline.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status; this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static BusinessException badRequest(String code, String msg) { return new BusinessException(HttpStatus.BAD_REQUEST, code, msg); }
    public static BusinessException forbidden(String code, String msg) { return new BusinessException(HttpStatus.FORBIDDEN, code, msg); }
    public static BusinessException notFound(String code, String msg) { return new BusinessException(HttpStatus.NOT_FOUND, code, msg); }
    public static BusinessException conflict(String code, String msg) { return new BusinessException(HttpStatus.CONFLICT, code, msg); }
    public static BusinessException unprocessable(String code, String msg) { return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code, msg); }
}
