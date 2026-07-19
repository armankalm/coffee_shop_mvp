package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemRequest;
import com.coffeeshop.app.repository.*;
import com.coffeeshop.app.service.print.NewOrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final ApplicationEventPublisher eventPublisher;
    private final RefOrderStatusRepository refOrderStatusRepository;
    private final OrderDailyCounterRepository orderDailyCounterRepository;
    private final ZoneId businessZone;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        CoffeeShopRepository coffeeShopRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        ToppingService toppingService,
                        ApplicationEventPublisher eventPublisher,
                        RefOrderStatusRepository refOrderStatusRepository,
                        OrderDailyCounterRepository orderDailyCounterRepository,
                        @Value("${app.order.timezone:Asia/Almaty}") String businessZoneId) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.coffeeShopRepository = coffeeShopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.toppingService = toppingService;
        this.eventPublisher = eventPublisher;
        this.refOrderStatusRepository = refOrderStatusRepository;
        this.orderDailyCounterRepository = orderDailyCounterRepository;
        this.businessZone = ZoneId.of(businessZoneId);
    }

    public OrderDto createOrder(String userEmail, CreateOrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        CoffeeShop shop = coffeeShopRepository.findByIdWithDetails(request.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + request.getShopId()));

        if (!"OPEN".equals(shop.getStatus().getCode())) {
            throw new IllegalStateException("Coffee shop is not accepting orders: " + shop.getName());
        }

        RefOrderStatus newStatus = refOrderStatusRepository.findByCode("NEW")
                .orElseThrow(() -> new NoSuchElementException("Order status NEW not found in reference table"));

        // Per-shop daily order number, reset at local midnight (app.order.timezone).
        LocalDate orderDate = LocalDate.now(businessZone);
        int dailyNumber = orderDailyCounterRepository.nextNumber(shop.getId(), orderDate);

        String customerName = request.getCustomerName();
        Order order = Order.builder()
                .user(user)
                .shop(shop)
                .status(newStatus)
                .customerName(customerName != null && !customerName.isBlank() ? customerName.trim() : null)
                .dailyNumber(dailyNumber)
                .orderDate(orderDate)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdWithToppings(itemRequest.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + itemRequest.getProductId()));

            if (!product.isAvailable()) {
                throw new IllegalArgumentException("Product is not available: " + product.getName());
            }

            if (!product.getCoffeeShop().getId().equals(shop.getId())) {
                throw new IllegalArgumentException(
                        "Product '" + product.getName() + "' does not belong to the selected shop");
            }

            Set<Long> toppingIds = itemRequest.getToppingIds() != null ? itemRequest.getToppingIds() : new HashSet<>();

            Set<Topping> toppings = new HashSet<>();
            if (!toppingIds.isEmpty()) {
                toppings = new HashSet<>(toppingRepository.findAllByIdWithIncompatibilities(toppingIds));
                if (toppings.size() != toppingIds.size()) {
                    throw new IllegalArgumentException("One or more toppings not found");
                }
                toppingService.validateCompatibility(toppingIds);
                Set<Long> allowedIds = product.getAvailableToppings().stream()
                        .map(Topping::getId)
                        .collect(Collectors.toSet());
                for (Topping t : toppings) {
                    if (!allowedIds.contains(t.getId())) {
                        throw new IllegalArgumentException(
                                "Topping '" + t.getName() + "' is not available for product '" + product.getName() + "'");
                    }
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
                    .status(newStatus)
                    .toppings(toppings)
                    .quantity(itemRequest.getQuantity())
                    .price(itemPrice)
                    .build();

            order.getItems().add(orderItem);
            total = total.add(itemPrice);
        }

        order.setTotal(total);
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new NewOrderEvent(this, saved.getId()));
        Order withDetails = orderRepository.findByIdWithDetails(saved.getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found after save: " + saved.getId()));
        return OrderDto.from(withDetails);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(String userEmail) {
        User user = userRepository.findByEmailWithRole(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        return orderRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(user.getId()).stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // Users can only see their own orders; admins/managers/baristas can see all
        User user = userRepository.findByEmailWithRole(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if ("USER".equals(user.getRole().getCode()) && !order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to order: " + orderId);
        }

        return OrderDto.from(order);
    }

    public OrderDto cancelOrder(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        User user = userRepository.findByEmailWithRole(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to order: " + orderId);
        }

        String statusCode = order.getStatus().getCode();
        if (!"NEW".equals(statusCode)) {
            throw new IllegalStateException("Cannot cancel order in status: " + statusCode);
        }

        RefOrderStatus cancelledStatus = refOrderStatusRepository.findByCode("CANCELLED")
                .orElseThrow(() -> new NoSuchElementException("Order status CANCELLED not found in reference table"));
        order.setStatus(cancelledStatus);
        return OrderDto.from(orderRepository.save(order));
    }
}
