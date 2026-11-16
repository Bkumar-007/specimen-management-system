package com.specimen.sms.application;

/** A request that is well-formed but violates a domain rule (maps to HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
