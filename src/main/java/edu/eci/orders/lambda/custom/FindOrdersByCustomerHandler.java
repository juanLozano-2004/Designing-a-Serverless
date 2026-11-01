package edu.eci.orders.lambda.custom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.eci.orders.application.OrderService;
import edu.eci.orders.domain.Order;
import edu.eci.orders.infrastructure.InMemoryOrderRepository;

import java.util.List;

/**
 * Lambda handler: edu.eci.orders.lambda.custom.FindOrdersByCustomerHandler::handleRequest
 * Input  JSON: {"customerId": "CUST-42"}
 * Output JSON: array of orders (possibly empty).
 */
public class FindOrdersByCustomerHandler implements RequestHandler<FindOrdersInput, List<Order>> {

    private static final OrderService PRELOADED_ORDER_SERVICE =
            new OrderService(InMemoryOrderRepository.createPreloadedRepository());

    private final OrderService orderService;

    public FindOrdersByCustomerHandler() {
        this(PRELOADED_ORDER_SERVICE);
    }

    FindOrdersByCustomerHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public List<Order> handleRequest(FindOrdersInput searchInput, Context runtimeContext) {
        String customerIdentifier = searchInput == null ? null : searchInput.getCustomerId();
        return Errors.mapDomainFailure(() -> orderService.listOrdersForCustomer(customerIdentifier));
    }
}
