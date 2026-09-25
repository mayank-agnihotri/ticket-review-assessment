package com.atl.ticketmgmt.common.exception;

import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends ApiException {

    public AiServiceUnavailableException(String message) {
        super("AI_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super("AI_SERVICE_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
