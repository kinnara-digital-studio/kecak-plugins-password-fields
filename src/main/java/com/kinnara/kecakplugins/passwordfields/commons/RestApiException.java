package com.kinnara.kecakplugins.passwordfields.commons;

public class RestApiException extends Exception {
    private String message;
    private int errorCode;

    public RestApiException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
