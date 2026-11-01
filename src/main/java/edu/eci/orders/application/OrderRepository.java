package edu.eci.orders.application;

import edu.eci.orders.domain.Order;

import java.util.List;
import java.util.Optional;

/** Storage port. The service depends on this interface, not on a concrete store. */
public interface OrderRepository {

    String generateNextOrderId();

    Order storeOrder(Order orderToStore);

    Optional<Order> findOrderById(String orderId);

    List<Order> findOrdersForCustomer(String customerIdentifier);

    /** @return true if the order existed and was removed */
    boolean removeOrderById(String orderId);
}
