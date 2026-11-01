package edu.eci.orders.application;

import edu.eci.orders.domain.CreateOrderRequest;
import edu.eci.orders.domain.Order;
import edu.eci.orders.domain.OrderNotFoundException;
import edu.eci.orders.domain.ValidationException;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void initializeOrderService() {
        orderService = new OrderService(new InMemoryOrderRepository());
    }

    @Test
    void shouldCreateValidOrderWithGeneratedId() {
        Order createdOrder = orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertNotNull(createdOrder.getId());
        assertTrue(createdOrder.getId().startsWith("ORD-"));
        assertEquals("CUST-42", createdOrder.getCustomerId());
        assertEquals("MacBook Air", createdOrder.getProduct());
        assertEquals(1, createdOrder.getQuantity());
    }

    @Test
    void shouldAssignDifferentIdsToCreatedOrders() {
        Order firstOrder = orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        Order secondOrder = orderService.registerOrder(new CreateOrderRequest("CUST-42", "Mouse", 1));

        assertNotEquals(firstOrder.getId(), secondOrder.getId());
    }

    @Test
    void shouldRetrieveExistingOrder() {
        Order createdOrder = orderService.registerOrder(new CreateOrderRequest("CUST-7", "Keyboard", 2));

        Order foundOrder = orderService.retrieveOrder(createdOrder.getId());

        assertEquals(createdOrder, foundOrder);
    }

    @Test
    void shouldReportMissingOrder() {
        OrderNotFoundException missingOrderFailure = assertThrows(OrderNotFoundException.class,
                () -> orderService.retrieveOrder("ORD-999"));

        assertEquals("ORD-999", missingOrderFailure.getRequestedOrderId());
        assertEquals("Order ORD-999 does not exist", missingOrderFailure.getMessage());
    }

    @Test
    void shouldListOnlyOrdersForRequestedCustomer() {
        orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        orderService.registerOrder(new CreateOrderRequest("CUST-7", "Keyboard", 2));
        orderService.registerOrder(new CreateOrderRequest("CUST-42", "Mouse", 3));

        List<Order> matchingOrders = orderService.listOrdersForCustomer("CUST-42");

        assertEquals(2, matchingOrders.size());
        assertTrue(matchingOrders.stream().allMatch(candidateOrder ->
                candidateOrder.getCustomerId().equals("CUST-42")));
    }

    @Test
    void shouldReturnEmptyListForCustomerWithoutOrders() {
        assertTrue(orderService.listOrdersForCustomer("CUST-100").isEmpty());
    }

    @Test
    void shouldRejectNullCreationRequest() {
        assertThrows(ValidationException.class, () -> orderService.registerOrder(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingCustomerIdentifier(String customerIdentifier) {
        assertThrows(ValidationException.class,
                () -> orderService.registerOrder(new CreateOrderRequest(customerIdentifier, "MacBook Air", 1)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectMissingProductName(String productName) {
        assertThrows(ValidationException.class,
                () -> orderService.registerOrder(new CreateOrderRequest("CUST-42", productName, 1)));
    }

    @Test
    void shouldRejectMissingQuantity() {
        assertThrows(ValidationException.class,
                () -> orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", null)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveQuantity(int itemQuantity) {
        assertThrows(ValidationException.class,
                () -> orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", itemQuantity)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankIdentifierOnRetrieval(String orderIdentifier) {
        assertThrows(ValidationException.class, () -> orderService.retrieveOrder(orderIdentifier));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankCustomerIdentifierOnSearch(String customerIdentifier) {
        assertThrows(ValidationException.class, () -> orderService.listOrdersForCustomer(customerIdentifier));
    }

    @Test
    void shouldNoLongerRetrieveRemovedOrder() {
        Order createdOrder = orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        orderService.removeOrder(createdOrder.getId());

        assertThrows(OrderNotFoundException.class, () -> orderService.retrieveOrder(createdOrder.getId()));
    }

    @Test
    void shouldReportAttemptToRemoveMissingOrder() {
        OrderNotFoundException missingOrderFailure = assertThrows(OrderNotFoundException.class,
                () -> orderService.removeOrder("ORD-999"));

        assertEquals("ORD-999", missingOrderFailure.getRequestedOrderId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankIdentifierOnRemoval(String orderIdentifier) {
        assertThrows(ValidationException.class, () -> orderService.removeOrder(orderIdentifier));
    }

    @Test
    void shouldKeepOtherCustomerOrdersWhenRemovingOne() {
        Order orderToRemove = orderService.registerOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));
        Order orderToKeep = orderService.registerOrder(new CreateOrderRequest("CUST-42", "Mouse", 1));

        orderService.removeOrder(orderToRemove.getId());

        assertEquals(List.of(orderToKeep), orderService.listOrdersForCustomer("CUST-42"));
    }

    @Test
    void shouldExposeSampleOrdersFromFreshRepository() {
        OrderService seededOrderService = new OrderService(InMemoryOrderRepository.createPreloadedRepository());

        assertEquals("MacBook Air", seededOrderService.retrieveOrder("ORD-001").getProduct());
        assertEquals(2, seededOrderService.listOrdersForCustomer("CUST-42").size());
    }
}
