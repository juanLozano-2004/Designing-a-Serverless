package edu.eci.orders.domain;

import java.util.Objects;

/**
 * An order in the system. Plain Java object: no HTTP or AWS knowledge.
 * Has a no-arg constructor and setters so JSON serializers (including the
 * AWS Lambda Java runtime) can build and read it.
 */
public class Order {

    private String orderIdentifier;
    private String customerIdentifier;
    private String productName;
    private int itemQuantity;

    public Order() {
    }

    public Order(String orderIdentifier, String customerIdentifier, String productName, int itemQuantity) {
        this.orderIdentifier = orderIdentifier;
        this.customerIdentifier = customerIdentifier;
        this.productName = productName;
        this.itemQuantity = itemQuantity;
    }

    public String getId() {
        return orderIdentifier;
    }

    public void setId(String orderIdentifier) {
        this.orderIdentifier = orderIdentifier;
    }

    public String getCustomerId() {
        return customerIdentifier;
    }

    public void setCustomerId(String customerIdentifier) {
        this.customerIdentifier = customerIdentifier;
    }

    public String getProduct() {
        return productName;
    }

    public void setProduct(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return itemQuantity;
    }

    public void setQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof Order otherOrder)) return false;
        return itemQuantity == otherOrder.itemQuantity
                && Objects.equals(orderIdentifier, otherOrder.orderIdentifier)
                && Objects.equals(customerIdentifier, otherOrder.customerIdentifier)
                && Objects.equals(productName, otherOrder.productName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderIdentifier, customerIdentifier, productName, itemQuantity);
    }

    @Override
    public String toString() {
        return "Order{id='" + orderIdentifier + "', customerId='" + customerIdentifier
                + "', product='" + productName + "', quantity=" + itemQuantity + "}";
    }
}
