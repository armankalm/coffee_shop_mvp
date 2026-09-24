package com.coffeeshop.app.service.oauth;

/** OAuth failure carrying a short error code that is passed to the frontend callback page. */
public class OAuthException extends RuntimeException {

    private final String errorCode;

    public OAuthException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public OAuthException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
