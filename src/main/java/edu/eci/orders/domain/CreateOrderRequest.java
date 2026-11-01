package edu.eci.orders.domain;

/**
 * Data needed to create an order. The id is assigned by the system.
 * quantity is an Integer (not int) so a missing value can be told apart from 0.
 */
public class CreateOrderRequest {

    private String customerIdentifier;
    private String productName;
    private Integer itemQuantity;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerIdentifier, String productName, Integer itemQuantity) {
        this.customerIdentifier = customerIdentifier;
        this.productName = productName;
        this.itemQuantity = itemQuantity;
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

    public Integer getQuantity() {
        return itemQuantity;
    }

    public void setQuantity(Integer itemQuantity) {
        this.itemQuantity = itemQuantity;
    }
}
