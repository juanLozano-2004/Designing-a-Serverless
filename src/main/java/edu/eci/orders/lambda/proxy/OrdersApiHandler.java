package edu.eci.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.orders.application.OrderService;
import edu.eci.orders.domain.CreateOrderRequest;
import edu.eci.orders.domain.Order;
import edu.eci.orders.domain.OrderNotFoundException;
import edu.eci.orders.domain.ValidationException;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP adapter for API Gateway Lambda proxy integration. One Lambda serves the
 * whole Orders API.
 *
 * Handler: edu.eci.orders.lambda.proxy.OrdersApiHandler::handleRequest
 *
 * This class is the only place that knows about HTTP: it reads the method,
 * resource, path/query parameters and body from the event, calls OrderService,
 * and chooses status codes, headers and the JSON body of the response.
 */
public class OrdersApiHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final OrderService PRELOADED_ORDER_SERVICE =
            new OrderService(InMemoryOrderRepository.createPreloadedRepository());

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OrderService orderService;

    public OrdersApiHandler() {
        this(PRELOADED_ORDER_SERVICE);
    }

    OrdersApiHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context logContext) {
        writeEventLog(requestEvent, logContext);
        try {
            return dispatchRequest(requestEvent);
        } catch (ValidationException validationFailure) {
            return createErrorResponse(400, "INVALID_REQUEST", validationFailure.getMessage());
        } catch (OrderNotFoundException missingOrderFailure) {
            return createErrorResponse(404, "ORDER_NOT_FOUND", missingOrderFailure.getMessage());
        } catch (RuntimeException unexpectedFailure) {
            // Logged to CloudWatch; the client only gets a generic message.
            unexpectedFailure.printStackTrace();
            return createErrorResponse(500, "INTERNAL_ERROR", "Unexpected error");
        }
    }

    /** Writes the received proxy event to CloudWatch, to inspect what API Gateway sends. */
    private static void writeEventLog(APIGatewayProxyRequestEvent requestEvent, Context logContext) {
        if (logContext == null) {
            return;
        }
        try {
            logContext.getLogger().log("EVENT " + JSON_MAPPER.writeValueAsString(requestEvent));
        } catch (JsonProcessingException serializationFailure) {
            logContext.getLogger().log("EVENT (not serializable) " + serializationFailure.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent dispatchRequest(APIGatewayProxyRequestEvent requestEvent) {
        String httpMethod = requestEvent.getHttpMethod();
        String resourcePath = requestEvent.getResource();

        if ("/orders".equals(resourcePath)) {
            if ("POST".equals(httpMethod)) {
                return respondToCreateOrder(requestEvent);
            }
            if ("GET".equals(httpMethod)) {
                return createJsonResponse(200, orderService.listOrdersForCustomer(
                        parameterValue(requestEvent.getQueryStringParameters(), "customerId")));
            }
        } else if ("/orders/{id}".equals(resourcePath)) {
            if ("GET".equals(httpMethod)) {
                return createJsonResponse(200,
                        orderService.retrieveOrder(parameterValue(requestEvent.getPathParameters(), "id")));
            }
            if ("DELETE".equals(httpMethod)) {
                orderService.removeOrder(parameterValue(requestEvent.getPathParameters(), "id"));
                return new APIGatewayProxyResponseEvent().withStatusCode(204);
            }
        } else {
            return createErrorResponse(404, "NOT_FOUND",
                    "Resource " + requestEvent.getPath() + " does not exist");
        }
        return createErrorResponse(405, "METHOD_NOT_ALLOWED",
                "Method " + httpMethod + " is not allowed on " + resourcePath);
    }

    private APIGatewayProxyResponseEvent respondToCreateOrder(APIGatewayProxyRequestEvent requestEvent) {
        CreateOrderRequest creationRequest = parseCreationPayload(requestEvent.getBody());
        Order createdOrder = orderService.registerOrder(creationRequest);

        APIGatewayProxyResponseEvent createdResponse = createJsonResponse(201, createdOrder);
        createdResponse.getHeaders().put("Location", requestCollectionPath(requestEvent) + "/" + createdOrder.getId());
        return createdResponse;
    }

    private static CreateOrderRequest parseCreationPayload(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return null; // OrderService rejects a missing request
        }
        try {
            return JSON_MAPPER.readValue(requestBody, CreateOrderRequest.class);
        } catch (JsonProcessingException parsingFailure) {
            throw new ValidationException("Request body is not valid JSON");
        }
    }

    /** Public path of /orders including the stage, e.g. "/dev/orders", so Location is resolvable. */
    private static String requestCollectionPath(APIGatewayProxyRequestEvent requestEvent) {
        if (requestEvent.getRequestContext() != null && requestEvent.getRequestContext().getPath() != null) {
            return requestEvent.getRequestContext().getPath();
        }
        return "/orders";
    }

    private static String parameterValue(Map<String, String> parameters, String parameterName) {
        return parameters == null ? null : parameters.get(parameterName);
    }

    private static APIGatewayProxyResponseEvent createErrorResponse(
            int httpStatus, String errorCode, String errorMessage) {
        return createJsonResponse(httpStatus, new ErrorBody(errorCode, errorMessage));
    }

    private static APIGatewayProxyResponseEvent createJsonResponse(int httpStatus, Object responseBody) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(httpStatus)
                    .withHeaders(new HashMap<>(Map.of("Content-Type", "application/json")))
                    .withBody(JSON_MAPPER.writeValueAsString(responseBody));
        } catch (JsonProcessingException serializationFailure) {
            throw new UncheckedIOException(serializationFailure);
        }
    }
}
