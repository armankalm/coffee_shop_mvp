package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemRequest;
import com.coffeeshop.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CoffeeShopRepository coffeeShopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private ToppingService toppingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private CoffeeShop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").role(Role.USER).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city("Almaty")
                .address("123 St").status(ShopStatus.OPEN).build();
        product = Product.builder().id(1L).name("Latte").category(ProductCategory.COFFEE)
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
    }

    @Test
    void createOrder_validRequest_returnsOrderDto() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Order savedOrder = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW)
                .total(BigDecimal.valueOf(1000))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDto result = orderService.createOrder("test@example.com", request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(com.coffeeshop.app.service.print.NewOrderEvent.class));
    }

    @Test
    void createOrder_shopNotFound_throwsNoSuchElement() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(99L);
        request.setItems(List.of(new OrderItemRequest()));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createOrder_closedShop_throwsIllegalState() {
        shop.setStatus(ShopStatus.CLOSED);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(new OrderItemRequest()));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void createOrder_unavailableProduct_throwsIllegalArgument() {
        product.setAvailable(false);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createOrder_calculatesCorrectTotal() {
        Topping topping = Topping.builder().id(1L).name("Extra Shot")
                .type(ToppingType.EXTRAS).price(BigDecimal.valueOf(100)).build();
        product = Product.builder().id(1L).name("Latte").category(ProductCategory.COFFEE)
                .basePrice(BigDecimal.valueOf(500)).available(true)
                .availableToppings(Set.of(topping)).build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setToppingIds(Set.of(1L));
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(toppingRepository.findAllById(Set.of(1L))).thenReturn(List.of(topping));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        Order savedOrder = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW)
                .total(BigDecimal.valueOf(1200)) // (500 + 100) * 2
                .build();
        when(orderRepository.save(orderCaptor.capture())).thenReturn(savedOrder);

        OrderDto result = orderService.createOrder("test@example.com", request);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(orderCaptor.getValue().getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        verify(eventPublisher).publishEvent(any(com.coffeeshop.app.service.print.NewOrderEvent.class));
    }

    @Test
    void getUserOrders_returnsUserOrders() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).build();
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getUserOrders("test@example.com");

        assertThat(result).hasSize(1);
    }

    @Test
    void cancelOrder_newOrder_setsStatusCancelled() {
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.cancelOrder("test@example.com", 1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_completedOrder_throwsIllegalState() {
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(OrderStatus.COMPLETED).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.cancelOrder("test@example.com", 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getOrderById_otherUsersOrder_throwsAccessDenied() {
        User otherUser = User.builder().id(2L).email("other@example.com").role(Role.USER).build();
        Order order = Order.builder().id(1L).user(otherUser).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.getOrderById("test@example.com", 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getOrderById_adminCanSeeAnyOrder() {
        User admin = User.builder().id(2L).email("admin@example.com").role(Role.ADMIN).build();
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        OrderDto result = orderService.getOrderById("admin@example.com", 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }
}
