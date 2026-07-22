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
    @Mock private RefOrderStatusRepository refOrderStatusRepository;
    @Mock private OrderDailyCounterRepository orderDailyCounterRepository;

    private OrderService orderService;

    private RefUserRole userRole;
    private RefShopStatus openStatus;
    private RefShopStatus closedStatus;
    private RefOrderStatus newStatus;
    private RefOrderStatus pendingPaymentStatus;
    private RefOrderStatus cancelledStatus;
    private RefOrderStatus completedStatus;
    private RefOrderStatus inProgressStatus;
    private RefProductCategory coffeeCategory;
    private RefToppingType extrasType;

    private User user;
    private CoffeeShop shop;
    private Product product;

    @BeforeEach
    void setUp() {
        userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        closedStatus = RefShopStatus.builder().id(2L).code("CLOSED").nameRu("Закрыто").nameEn("Closed").build();
        newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();
        pendingPaymentStatus = RefOrderStatus.builder().id(6L).code("PENDING_PAYMENT")
                .nameRu("Ожидает оплаты").nameEn("Awaiting Payment").build();
        cancelledStatus = RefOrderStatus.builder().id(5L).code("CANCELLED").nameRu("Отменён").nameEn("Cancelled").build();
        completedStatus = RefOrderStatus.builder().id(4L).code("COMPLETED").nameRu("Завершён").nameEn("Completed").build();
        inProgressStatus = RefOrderStatus.builder().id(2L).code("IN_PROGRESS").nameRu("В работе").nameEn("In Progress").build();
        coffeeCategory = RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
        extrasType = RefToppingType.builder().id(4L).code("EXTRAS").nameRu("Добавки").nameEn("Extras").build();

        user = User.builder().id(1L).email("test@example.com").role(userRole).build();
        City almatyCity = City.builder().id(1L).name("Almaty").active(true).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city(almatyCity)
                .address("123 St").status(openStatus).build();
        product = Product.builder().id(1L).name("Latte").category(coffeeCategory)
                .basePrice(BigDecimal.valueOf(500)).available(true).coffeeShop(shop).build();

        orderService = new OrderService(orderRepository, userRepository, coffeeShopRepository,
                productRepository, toppingRepository, toppingService, eventPublisher,
                refOrderStatusRepository, orderDailyCounterRepository, "Asia/Almaty");
    }

    @Test
    void createOrder_validRequest_returnsOrderDto() {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(refOrderStatusRepository.findByCode("PENDING_PAYMENT")).thenReturn(Optional.of(pendingPaymentStatus));
        when(orderDailyCounterRepository.nextNumber(eq(1L), any())).thenReturn(7);

        Order savedOrder = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(pendingPaymentStatus)
                .total(BigDecimal.valueOf(1000))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(savedOrder));

        OrderDto result = orderService.createOrder("test@example.com", request);

        assertThat(result).isNotNull();
        // Orders are created as an unpaid draft; a payment webhook later promotes them to NEW.
        assertThat(result.getStatus()).isEqualTo("PENDING_PAYMENT");

        // The order persisted must carry the per-shop daily number and its local date.
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getDailyNumber()).isEqualTo(7);
        assertThat(orderCaptor.getValue().getOrderDate()).isNotNull();

        // Draft creation must NOT notify kitchen/print/board — those fire only after payment.
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }

    @Test
    void createOrder_shopNotFound_throwsNoSuchElement() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(99L);
        request.setItems(List.of(new OrderItemRequest()));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createOrder_closedShop_throwsIllegalState() {
        shop.setStatus(closedStatus);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(new OrderItemRequest()));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not accepting orders");
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

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(refOrderStatusRepository.findByCode("PENDING_PAYMENT")).thenReturn(Optional.of(pendingPaymentStatus));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void createOrder_productFromWrongShop_throwsIllegalArgument() {
        City otherCity = City.builder().id(2L).name("Astana").active(true).build();
        CoffeeShop otherShop = CoffeeShop.builder().id(2L).name("Other Shop").city(otherCity)
                .address("456 St").status(openStatus).build();
        product.setCoffeeShop(otherShop);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(refOrderStatusRepository.findByCode("PENDING_PAYMENT")).thenReturn(Optional.of(pendingPaymentStatus));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the selected shop");
    }

    @Test
    void createOrder_calculatesCorrectTotal() {
        Topping topping = Topping.builder().id(1L).name("Extra Shot")
                .type(extrasType).price(BigDecimal.valueOf(100)).build();
        product = Product.builder().id(1L).name("Latte").category(coffeeCategory)
                .basePrice(BigDecimal.valueOf(500)).available(true)
                .coffeeShop(shop).availableToppings(Set.of(topping)).build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setToppingIds(Set.of(1L));
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(toppingRepository.findAllByIdWithIncompatibilities(Set.of(1L))).thenReturn(List.of(topping));
        when(refOrderStatusRepository.findByCode("PENDING_PAYMENT")).thenReturn(Optional.of(pendingPaymentStatus));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        Order savedOrder = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(pendingPaymentStatus)
                .total(BigDecimal.valueOf(1200)) // (500 + 100) * 2
                .build();
        when(orderRepository.save(orderCaptor.capture())).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(savedOrder));

        OrderDto result = orderService.createOrder("test@example.com", request);

        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(orderCaptor.getValue().getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        // Draft creation fires no events; notifications happen after payment.
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }

    @Test
    void createOrder_toppingNotAvailableForProduct_throwsIllegalArgument() {
        Topping allowedTopping = Topping.builder().id(1L).name("Extra Shot")
                .type(extrasType).price(BigDecimal.valueOf(100)).build();
        Topping disallowedTopping = Topping.builder().id(2L).name("Soy Milk")
                .type(extrasType).price(BigDecimal.valueOf(120)).build();

        product = Product.builder().id(1L).name("Lemonade").category(coffeeCategory)
                .basePrice(BigDecimal.valueOf(800)).available(true)
                .coffeeShop(shop).availableToppings(Set.of(allowedTopping)).build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setToppingIds(Set.of(2L));
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(toppingRepository.findAllByIdWithIncompatibilities(Set.of(2L))).thenReturn(List.of(disallowedTopping));
        when(refOrderStatusRepository.findByCode("PENDING_PAYMENT")).thenReturn(Optional.of(pendingPaymentStatus));

        assertThatThrownBy(() -> orderService.createOrder("test@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available for product");
    }

    @Test
    void getUserOrders_returnsUserOrders() {
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();
        when(orderRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getUserOrders("test@example.com");

        assertThat(result).hasSize(1);
    }

    @Test
    void cancelOrder_newOrder_setsStatusCancelled() {
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));
        when(refOrderStatusRepository.findByCode("CANCELLED")).thenReturn(Optional.of(cancelledStatus));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.cancelOrder("test@example.com", 1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_completedOrder_throwsIllegalState() {
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(completedStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.cancelOrder("test@example.com", 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelOrder_userCancelsOtherUsersOrder_throwsAccessDenied() {
        User otherUser = User.builder().id(2L).email("other@example.com").role(userRole).build();
        Order order = Order.builder().id(1L).user(otherUser).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.cancelOrder("test@example.com", 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void cancelOrder_inProgressOrder_throwsIllegalState() {
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(inProgressStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.cancelOrder("test@example.com", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel order in status");
    }

    @Test
    void getOrderById_otherUsersOrder_throwsAccessDenied() {
        User otherUser = User.builder().id(2L).email("other@example.com").role(userRole).build();
        Order order = Order.builder().id(1L).user(otherUser).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> orderService.getOrderById("test@example.com", 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getOrderById_adminCanSeeAnyOrder() {
        RefUserRole adminRole = RefUserRole.builder().id(4L).code("ADMIN").nameRu("Администратор").nameEn("Admin").build();
        User admin = User.builder().id(2L).email("admin@example.com").role(adminRole).build();
        Order order = Order.builder().id(1L).user(user).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithRole("admin@example.com")).thenReturn(Optional.of(admin));

        OrderDto result = orderService.getOrderById("admin@example.com", 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void createOrder_staffPos_createsNewImmediatelyAndFiresEvents() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA")
                .nameRu("Бариста").nameEn("Barista").build();
        User barista = User.builder().id(3L).email("barista@example.com").role(baristaRole).build();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        when(userRepository.findByEmailWithRole("barista@example.com")).thenReturn(Optional.of(barista));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(refOrderStatusRepository.findByCode("NEW")).thenReturn(Optional.of(newStatus));
        when(orderDailyCounterRepository.nextNumber(eq(1L), any())).thenReturn(3);

        Order savedOrder = Order.builder()
                .id(1L).user(barista).shop(shop)
                .status(newStatus)
                .total(BigDecimal.valueOf(500))
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(savedOrder));

        OrderDto result = orderService.createOrder("barista@example.com", request);

        // Staff POS orders skip the payment draft and go straight to the kitchen queue.
        assertThat(result.getStatus()).isEqualTo("NEW");
        verify(eventPublisher).publishEvent(any(com.coffeeshop.app.service.print.NewOrderEvent.class));
        verify(eventPublisher).publishEvent(any(com.coffeeshop.app.service.board.OrderStatusChangedEvent.class));
    }
}
