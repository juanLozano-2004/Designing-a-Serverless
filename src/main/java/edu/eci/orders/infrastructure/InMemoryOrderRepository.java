package edu.eci.orders.infrastructure;

import edu.eci.orders.application.OrderRepository;
import edu.eci.orders.domain.Order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory store. NOT persistent: on Lambda, data lives only as long as the
 * execution environment, and each Lambda function has its own copy.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> ordersById = new ConcurrentHashMap<>();
    private final AtomicInteger orderIdSequence = new AtomicInteger();

    /** Repository preloaded with predefined orders, so reads work on a fresh Lambda. */
    public static InMemoryOrderRepository createPreloadedRepository() {
        InMemoryOrderRepository sampleRepository = new InMemoryOrderRepository();
        sampleRepository.storeOrder(new Order(sampleRepository.generateNextOrderId(), "CUST-42", "MacBook Air", 1));
        sampleRepository.storeOrder(new Order(sampleRepository.generateNextOrderId(), "CUST-42", "Magic Mouse", 2));
        sampleRepository.storeOrder(new Order(sampleRepository.generateNextOrderId(), "CUST-7", "Keyboard", 2));
        return sampleRepository;
    }

    @Override
    public String generateNextOrderId() {
        return String.format("ORD-%03d", orderIdSequence.incrementAndGet());
    }

    @Override
    public Order storeOrder(Order orderToStore) {
        ordersById.put(orderToStore.getId(), orderToStore);
        return orderToStore;
    }

    @Override
    public Optional<Order> findOrderById(String orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    @Override
    public List<Order> findOrdersForCustomer(String customerIdentifier) {
        List<Order> matchingOrders = new ArrayList<>();
        for (Order storedOrder : ordersById.values()) {
            if (storedOrder.getCustomerId().equals(customerIdentifier)) {
                matchingOrders.add(storedOrder);
            }
        }
        matchingOrders.sort(Comparator.comparing(Order::getId));
        return matchingOrders;
    }

    @Override
    public boolean removeOrderById(String orderId) {
        return ordersById.remove(orderId) != null;
    }
}
