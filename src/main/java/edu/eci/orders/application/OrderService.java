package edu.eci.orders.application;

import edu.eci.orders.domain.CreateOrderRequest;
import edu.eci.orders.domain.Order;
import edu.eci.orders.domain.OrderNotFoundException;
import edu.eci.orders.domain.ValidationException;

import java.util.List;

/**
 * Application logic of the Orders API. Knows about orders, customers, products
 * and quantities; knows nothing about HTTP, status codes or API Gateway.
 */
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order registerOrder(CreateOrderRequest creationRequest) {
        if (creationRequest == null) {
            throw new ValidationException("Request body is required");
        }
        requireNonBlank(creationRequest.getCustomerId(), "customerId");
        requireNonBlank(creationRequest.getProduct(), "product");
        if (creationRequest.getQuantity() == null) {
            throw new ValidationException("quantity is required");
        }
        if (creationRequest.getQuantity() < 1) {
            throw new ValidationException("quantity must be greater than 0");
        }

        Order newOrder = new Order(
                orderRepository.generateNextOrderId(),
                creationRequest.getCustomerId().trim(),
                creationRequest.getProduct().trim(),
                creationRequest.getQuantity());
        return orderRepository.storeOrder(newOrder);
    }

    public Order retrieveOrder(String orderId) {
        requireNonBlank(orderId, "id");
        return orderRepository.findOrderById(orderId.trim())
                .orElseThrow(() -> new OrderNotFoundException(orderId.trim()));
    }

    public List<Order> listOrdersForCustomer(String customerIdentifier) {
        requireNonBlank(customerIdentifier, "customerId");
        return orderRepository.findOrdersForCustomer(customerIdentifier.trim());
    }

    public void removeOrder(String orderId) {
        requireNonBlank(orderId, "id");
        if (!orderRepository.removeOrderById(orderId.trim())) {
            throw new OrderNotFoundException(orderId.trim());
        }
    }

    private static void requireNonBlank(String suppliedValue, String fieldName) {
        if (suppliedValue == null || suppliedValue.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
    }
}
