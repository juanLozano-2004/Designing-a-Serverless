package edu.eci.orders.lambda.custom;

/**
 * Error returned to the Lambda runtime. Its message is the JSON error body
 * ({"error": ..., "message": ...}) so that API Gateway (custom integration)
 * can match the error code with a regex and pass the body through unchanged.
 */
public class OrderApiException extends RuntimeException {

    private final String errorCode;

    public OrderApiException(String errorCode, String errorMessage) {
        super("{\"error\":\"" + escapeJsonValue(errorCode) + "\",\"message\":\""
                + escapeJsonValue(errorMessage) + "\"}");
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    private static String escapeJsonValue(String valueToEscape) {
        return valueToEscape == null ? "" : valueToEscape.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
