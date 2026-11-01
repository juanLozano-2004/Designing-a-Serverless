package edu.eci.orders.lambda.custom;

import edu.eci.orders.application.OrderService;
import edu.eci.orders.domain.CreateOrderRequest;
import edu.eci.orders.domain.Order;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomHandlersTest {

    private OrderService orderService;

    @BeforeEach
    void initializeOrderService() {
        orderService = new OrderService(InMemoryOrderRepository.createPreloadedRepository());
    }

    @Test
    void shouldReturnCreatedOrderFromCreateHandler() {
        Order createdOrder = new CreateOrderHandler(orderService)
                .handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 1), null);

        assertEquals("ORD-004", createdOrder.getId());
    }

    @Test
    void shouldReportInvalidCreateRequestAsJsonError() {
        OrderApiException invalidRequestFailure = assertThrows(OrderApiException.class,
                () -> new CreateOrderHandler(orderService)
                .handleRequest(new CreateOrderRequest("CUST-42", "MacBook Air", 0), null));

        assertEquals("INVALID_REQUEST", invalidRequestFailure.getErrorCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"quantity must be greater than 0\"}",
                invalidRequestFailure.getMessage());
    }

    @Test
    void shouldReturnExistingOrderFromGetHandler() {
        Order foundOrder = new GetOrderHandler(orderService).handleRequest(new GetOrderInput("ORD-003"), null);

        assertEquals("Keyboard", foundOrder.getProduct());
    }

    @Test
    void shouldReportMissingOrderFromGetHandlerAsJsonError() {
        OrderApiException missingOrderFailure = assertThrows(OrderApiException.class,
                () -> new GetOrderHandler(orderService).handleRequest(new GetOrderInput("ORD-999"), null));

        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                missingOrderFailure.getMessage());
    }

    @Test
    void shouldTreatNullGetInputAsInvalid() {
        OrderApiException invalidInputFailure = assertThrows(OrderApiException.class,
                () -> new GetOrderHandler(orderService).handleRequest(null, null));

        assertEquals("INVALID_REQUEST", invalidInputFailure.getErrorCode());
    }

    @Test
    void shouldReturnCustomerOrdersFromFindHandler() {
        List<Order> matchingOrders = new FindOrdersByCustomerHandler(orderService)
                .handleRequest(new FindOrdersInput("CUST-42"), null);

        assertEquals(2, matchingOrders.size());
    }

    @Test
    void shouldReportMissingCustomerIdentifierAsInvalid() {
        OrderApiException invalidInputFailure = assertThrows(OrderApiException.class,
                () -> new FindOrdersByCustomerHandler(orderService).handleRequest(new FindOrdersInput(""), null));

        assertEquals("INVALID_REQUEST", invalidInputFailure.getErrorCode());
    }
}
