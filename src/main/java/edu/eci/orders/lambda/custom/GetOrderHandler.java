package edu.eci.orders.lambda.custom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.orders.application.OrderService;
import edu.eci.orders.domain.Order;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;

/**
 * Lambda handler: edu.eci.orders.lambda.custom.GetOrderHandler::handleRequest
 * Input  JSON: {"id": "ORD-001"}
 * Output JSON: the order.
 */
public class GetOrderHandler implements RequestHandler<GetOrderInput, Order> {

    private static final OrderService PRELOADED_ORDER_SERVICE =
            new OrderService(InMemoryOrderRepository.createPreloadedRepository());

    private final OrderService orderService;

    public GetOrderHandler() {
        this(PRELOADED_ORDER_SERVICE);
    }

    GetOrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Order handleRequest(GetOrderInput lookupInput, Context runtimeContext) {
        String orderId = lookupInput == null ? null : lookupInput.getId();
        return Errors.mapDomainFailure(() -> orderService.retrieveOrder(orderId));
    }
}
