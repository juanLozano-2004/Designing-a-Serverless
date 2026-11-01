package edu.eci.orders.domain;

/** The requested order does not exist. Says nothing about HTTP 404: that mapping lives elsewhere. */
public class OrderNotFoundException extends RuntimeException {

    private final String requestedOrderId;

    public OrderNotFoundException(String requestedOrderId) {
        super("Order " + requestedOrderId + " does not exist");
        this.requestedOrderId = requestedOrderId;
    }

    public String getRequestedOrderId() {
        return requestedOrderId;
    }
}
