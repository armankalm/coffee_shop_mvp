package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemRequest;
import com.coffeeshop.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CoffeeShopRepository coffeeShopRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final ToppingService toppingService;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CoffeeShopRepository coffeeShopRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        ToppingService toppingService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.coffeeShopRepository = coffeeShopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.toppingService = toppingService;
    }

    public OrderDto createOrder(String userEmail, CreateOrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        CoffeeShop shop = coffeeShopRepository.findById(request.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + request.getShopId()));

        if (shop.getStatus() == ShopStatus.CLOSED) {
            throw new IllegalStateException("Coffee shop is closed: " + shop.getName());
        }

        Order order = Order.builder()
                .user(user)
                .shop(shop)
                .status(OrderStatus.NEW)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + itemRequest.getProductId()));

            if (!product.isAvailable()) {
                throw new IllegalArgumentException("Product is not available: " + product.getName());
            }

            Set<Long> toppingIds = itemRequest.getToppingIds() != null ? itemRequest.getToppingIds() : new HashSet<>();
            toppingService.validateCompatibility(toppingIds);

            Set<Topping> toppings = new HashSet<>();
            if (!toppingIds.isEmpty()) {
                toppings = new HashSet<>(toppingRepository.findAllById(toppingIds));
                if (toppings.size() != toppingIds.size()) {
                    throw new IllegalArgumentException("One or more toppings not found");
                }
            }

            BigDecimal toppingPrice = toppings.stream()
                    .map(Topping::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal itemPrice = product.getBasePrice().add(toppingPrice)
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .toppings(toppings)
                    .quantity(itemRequest.getQuantity())
                    .price(itemPrice)
                    .build();

            order.getItems().add(orderItem);
            total = total.add(itemPrice);
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);
        return OrderDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // Users can only see their own orders; admins/managers/baristas can see all
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if (user.getRole() == Role.USER && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to order: " + orderId);
        }

        return OrderDto.from(order);
    }

    public OrderDto cancelOrder(String userEmail, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if (user.getRole() == Role.USER && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to order: " + orderId);
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        return OrderDto.from(orderRepository.save(order));
    }
}
