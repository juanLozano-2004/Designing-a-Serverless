package edu.eci.orders.lambda.custom;

/** Input built by the API Gateway mapping template for GET /orders?customerId=...: {"customerId": "CUST-42"} */
public class FindOrdersInput {

    private String customerIdentifier;

    public FindOrdersInput() {
    }

    public FindOrdersInput(String customerIdentifier) {
        this.customerIdentifier = customerIdentifier;
    }

    public String getCustomerId() {
        return customerIdentifier;
    }

    public void setCustomerId(String customerIdentifier) {
        this.customerIdentifier = customerIdentifier;
    }
}
