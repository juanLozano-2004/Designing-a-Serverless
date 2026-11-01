package edu.eci.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import edu.eci.orders.application.OrderRepository;
import edu.eci.orders.application.OrderService;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdersApiHandlerTest {

    private OrdersApiHandler apiHandler;

    @BeforeEach
    void initializeApiHandler() {
        apiHandler = new OrdersApiHandler(new OrderService(InMemoryOrderRepository.createPreloadedRepository()));
    }

    @Test
    void shouldReturn200ForExistingOrder() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(200, gatewayResponse.getStatusCode());
        assertEquals("application/json", gatewayResponse.getHeaders().get("Content-Type"));
        assertEquals("{\"id\":\"ORD-001\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn404AndErrorBodyForMissingOrder() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-999")), null);

        assertEquals(404, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn200ArrayForCustomerSearch() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("GET", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-42")), null);

        assertEquals(200, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getBody().startsWith("[{\"id\":\"ORD-001\""));
    }

    @Test
    void shouldReturnEmptyArrayWhenCustomerHasNoOrders() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("GET", "/orders").withQueryStringParameters(Map.of("customerId", "CUST-100")), null);

        assertEquals(200, gatewayResponse.getStatusCode());
        assertEquals("[]", gatewayResponse.getBody());
    }

    @Test
    void shouldReturn400WhenCustomerIdentifierIsMissing() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(buildRequest("GET", "/orders"), null);

        assertEquals(400, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"customerId is required\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn201AndLocationForCreatedOrder() {
        APIGatewayProxyRequestEvent creationEvent = buildRequest("POST", "/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}")
                .withRequestContext(new APIGatewayProxyRequestEvent.ProxyRequestContext().withPath("/dev/orders"));

        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(creationEvent, null);

        assertEquals(201, gatewayResponse.getStatusCode());
        assertEquals("/dev/orders/ORD-004", gatewayResponse.getHeaders().get("Location"));
        assertEquals("{\"id\":\"ORD-004\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn400ForNonPositiveQuantity() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(buildRequest("POST", "/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"Mouse\",\"quantity\":0}"), null);

        assertEquals(400, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"quantity must be greater than 0\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn400ForMalformedJson() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("POST", "/orders").withBody("{\"customerId\":"), null);

        assertEquals(400, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"Request body is not valid JSON\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn400ForMismatchedJsonType() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(buildRequest("POST", "/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"Mouse\",\"quantity\":\"abc\"}"), null);

        assertEquals(400, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturn400WhenCreationBodyIsMissing() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(buildRequest("POST", "/orders"), null);

        assertEquals(400, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"INVALID_REQUEST\",\"message\":\"Request body is required\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn204WithoutBodyForRemovedOrder() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildDeleteRequest("ORD-001"), null);

        assertEquals(204, gatewayResponse.getStatusCode());
        assertNull(gatewayResponse.getBody());
    }

    @Test
    void shouldReturn404WhenRemovingMissingOrder() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildDeleteRequest("ORD-999"), null);

        assertEquals(404, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                gatewayResponse.getBody());
    }

    @Test
    void shouldReturn404WhenRetrievingRemovedOrder() {
        apiHandler.handleRequest(buildDeleteRequest("ORD-001"), null);

        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(404, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturn404WhenRemovingSameOrderTwice() {
        apiHandler.handleRequest(buildDeleteRequest("ORD-001"), null);

        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildDeleteRequest("ORD-001"), null);

        assertEquals(404, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturn405ForUnsupportedMethod() {
        APIGatewayProxyResponseEvent gatewayResponse = apiHandler.handleRequest(
                buildRequest("PATCH", "/orders/{id}").withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(405, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturn500WithoutExposingUnexpectedFailureDetails() {
        OrderRepository failingRepository = new InMemoryOrderRepository() {
            @Override
            public java.util.Optional<edu.eci.orders.domain.Order> findOrderById(String orderIdentifier) {
                throw new IllegalStateException("database exploded");
            }
        };
        OrdersApiHandler failingHandler = new OrdersApiHandler(new OrderService(failingRepository));

        APIGatewayProxyResponseEvent gatewayResponse = failingHandler.handleRequest(
                buildRequest("GET", "/orders/{id}").withPathParameters(Map.of("id", "ORD-001")), null);

        assertEquals(500, gatewayResponse.getStatusCode());
        assertEquals("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Unexpected error\"}", gatewayResponse.getBody());
    }

    private static APIGatewayProxyRequestEvent buildDeleteRequest(String orderIdentifier) {
        return buildRequest("DELETE", "/orders/{id}").withPathParameters(Map.of("id", orderIdentifier));
    }

    private static APIGatewayProxyRequestEvent buildRequest(String httpMethod, String resourcePath) {
        return new APIGatewayProxyRequestEvent().withHttpMethod(httpMethod).withResource(resourcePath);
    }
}
