package edu.eci.orders.lambda.custom;

import edu.eci.orders.domain.OrderNotFoundException;
import edu.eci.orders.domain.ValidationException;

import java.util.function.Supplier;

/** Translates domain exceptions into error codes. Choosing the HTTP status is left to API Gateway. */
final class Errors {

    private Errors() {
    }

    static <T> T mapDomainFailure(Supplier<T> operationSupplier) {
        try {
            return operationSupplier.get();
        } catch (ValidationException validationFailure) {
            throw new OrderApiException("INVALID_REQUEST", validationFailure.getMessage());
        } catch (OrderNotFoundException missingOrderFailure) {
            throw new OrderApiException("ORDER_NOT_FOUND", missingOrderFailure.getMessage());
        } catch (RuntimeException unexpectedFailure) {
            // Logged to CloudWatch; the client only gets a generic message.
            unexpectedFailure.printStackTrace();
            throw new OrderApiException("INTERNAL_ERROR", "Unexpected error");
        }
    }
}
