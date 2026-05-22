package com.ibm.bmcshell.exceptions;

/**
 * Custom exception for HTTP client errors with retry logic support.
 * Categorizes errors into different types to determine retry behavior.
 */
public class HttpClientException extends Exception {

    private final ErrorType errorType;
    private final int statusCode;
    private final boolean retryable;

    /**
     * Error type enumeration for categorizing HTTP client errors
     */
    public enum ErrorType {
        /** Network connectivity issues (DNS, connection refused, timeout) */
        CONNECTIVITY_ERROR(false, "Network connectivity issue"),

        /** Client errors (4xx status codes) */
        CLIENT_ERROR(false, "Client error"),

        /** Server errors (5xx status codes) */
        SERVER_ERROR(true, "Server error"),

        /** SSL/TLS errors */
        SSL_ERROR(false, "SSL/TLS error"),

        /** Authentication/Authorization errors */
        AUTH_ERROR(false, "Authentication error"),

        /** Timeout errors */
        TIMEOUT_ERROR(false, "Request timeout"),

        /** Unknown errors */
        UNKNOWN_ERROR(false, "Unknown error");

        private final boolean defaultRetryable;
        private final String description;

        ErrorType(boolean defaultRetryable, String description) {
            this.defaultRetryable = defaultRetryable;
            this.description = description;
        }

        public boolean isDefaultRetryable() {
            return defaultRetryable;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructor with error type and message
     */
    public HttpClientException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = -1;
        this.retryable = errorType.isDefaultRetryable();
    }

    /**
     * Constructor with error type, message, and cause
     */
    public HttpClientException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = -1;
        this.retryable = errorType.isDefaultRetryable();
    }

    /**
     * Constructor with error type, status code, and message
     */
    public HttpClientException(ErrorType errorType, int statusCode, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
        this.retryable = determineRetryability(errorType, statusCode);
    }

    /**
     * Constructor with error type, status code, message, and cause
     */
    public HttpClientException(ErrorType errorType, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = statusCode;
        this.retryable = determineRetryability(errorType, statusCode);
    }

    /**
     * Determine if the error is retryable based on error type and status code
     */
    private boolean determineRetryability(ErrorType errorType, int statusCode) {
        // Server errors (5xx) are retryable
        if (errorType == ErrorType.SERVER_ERROR) {
            return true;
        }

        // Client errors (4xx) are not retryable
        if (errorType == ErrorType.CLIENT_ERROR) {
            return false;
        }

        // Use default retryability for other error types
        return errorType.isDefaultRetryable();
    }

    /**
     * Create exception from HTTP status code
     */
    public static HttpClientException fromStatusCode(int statusCode, String message) {
        ErrorType errorType;

        if (statusCode >= 400 && statusCode < 500) {
            errorType = ErrorType.CLIENT_ERROR;
        } else if (statusCode >= 500 && statusCode < 600) {
            errorType = ErrorType.SERVER_ERROR;
        } else {
            errorType = ErrorType.UNKNOWN_ERROR;
        }

        return new HttpClientException(errorType, statusCode, message);
    }

    /**
     * Create exception from connectivity error
     */
    public static HttpClientException connectivityError(String message, Throwable cause) {
        return new HttpClientException(ErrorType.CONNECTIVITY_ERROR, message, cause);
    }

    /**
     * Create exception from SSL error
     */
    public static HttpClientException sslError(String message, Throwable cause) {
        return new HttpClientException(ErrorType.SSL_ERROR, message, cause);
    }

    /**
     * Create exception from timeout error
     */
    public static HttpClientException timeoutError(String message) {
        return new HttpClientException(ErrorType.TIMEOUT_ERROR, message);
    }

    /**
     * Create exception from authentication error
     */
    public static HttpClientException authError(String message) {
        return new HttpClientException(ErrorType.AUTH_ERROR, 401, message);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Get maximum retry attempts for this error type
     */
    public int getMaxRetryAttempts() {
        if (errorType == ErrorType.SERVER_ERROR) {
            return 2; // Retry 2 times for server errors
        }
        return 0; // No retry for other errors
    }

    /**
     * Get user-friendly error message
     */
    public String getUserMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(errorType.getDescription());

        if (statusCode > 0) {
            sb.append(" (HTTP ").append(statusCode).append(")");
        }

        sb.append(": ").append(getMessage());

        if (!retryable) {
            sb.append(" - Not retrying.");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("HttpClientException[type=%s, statusCode=%d, retryable=%s, message=%s]",
                errorType, statusCode, retryable, getMessage());
    }
}
