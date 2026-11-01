package edu.eci.orders.lambda.custom;

/** Input built by the API Gateway mapping template for GET /orders/{id}: {"id": "ORD-001"} */
public class GetOrderInput {

    private String orderIdentifier;

    public GetOrderInput() {
    }

    public GetOrderInput(String orderIdentifier) {
        this.orderIdentifier = orderIdentifier;
    }

    public String getId() {
        return orderIdentifier;
    }

    public void setId(String orderIdentifier) {
        this.orderIdentifier = orderIdentifier;
    }
}
