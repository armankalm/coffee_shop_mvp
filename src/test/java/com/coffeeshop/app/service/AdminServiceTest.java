package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.*;
import com.coffeeshop.app.service.print.PrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CoffeeShopRepository coffeeShopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PrintService printService;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private CoffeeShop shop;
    private Product product;
    private Topping topping;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@test.com").role(Role.USER).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city("Almaty")
                .address("123 St").status(ShopStatus.OPEN).build();
        product = Product.builder().id(1L).name("Latte").category(ProductCategory.COFFEE)
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
        topping = Topping.builder().id(1L).name("Oat Milk").type(ToppingType.MILK)
                .price(BigDecimal.valueOf(100)).build();
        order = Order.builder().id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).build();
    }

    @Test
    void getAllOrders_noFilters_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getAllOrders_filterByStatus_returnsMatchingOrders() {
        when(orderRepository.findByStatus(OrderStatus.NEW)).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders(OrderStatus.NEW, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void getAllOrders_filterByShopId_returnsMatchingOrders() {
        when(orderRepository.findByShopId(1L)).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders(null, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllOrders_filterByStatusAndShopId_returnsMatchingOrders() {
        when(orderRepository.findByStatus(OrderStatus.NEW)).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders(OrderStatus.NEW, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrderById_existingOrder_returnsDto() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto result = adminService.getOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_notFound_throwsNoSuchElement() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getOrderById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateOrderStatus_validOrder_updatesStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = adminService.updateOrderStatus(1L, OrderStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    void updateOrderStatus_notFound_throwsNoSuchElement() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateOrderStatus(99L, OrderStatus.IN_PROGRESS))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createShop_validRequest_returnsDto() {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("New Shop");
        request.setCity("Astana");
        request.setAddress("456 Ave");
        request.setStatus(ShopStatus.OPEN);

        when(coffeeShopRepository.save(any(CoffeeShop.class))).thenAnswer(inv -> {
            CoffeeShop s = inv.getArgument(0);
            s = CoffeeShop.builder().id(2L).name(s.getName()).city(s.getCity())
                    .address(s.getAddress()).status(s.getStatus()).build();
            return s;
        });

        CoffeeShopDto result = adminService.createShop(request);

        assertThat(result.getName()).isEqualTo("New Shop");
        assertThat(result.getCity()).isEqualTo("Astana");
    }

    @Test
    void updateShop_notFound_throwsNoSuchElement() {
        when(coffeeShopRepository.findById(99L)).thenReturn(Optional.empty());

        CreateShopRequest request = new CreateShopRequest();
        request.setName("Updated");
        request.setCity("City");
        request.setAddress("Addr");
        request.setStatus(ShopStatus.OPEN);

        assertThatThrownBy(() -> adminService.updateShop(99L, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteShop_existingShop_deletesIt() {
        when(coffeeShopRepository.existsById(1L)).thenReturn(true);

        adminService.deleteShop(1L);

        verify(coffeeShopRepository).deleteById(1L);
    }

    @Test
    void deleteShop_notFound_throwsNoSuchElement() {
        when(coffeeShopRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteShop(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createProduct_validRequest_returnsDto() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Cappuccino");
        request.setCategory(ProductCategory.COFFEE);
        request.setBasePrice(BigDecimal.valueOf(450));
        request.setAvailable(true);

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return Product.builder().id(2L).name(p.getName()).category(p.getCategory())
                    .basePrice(p.getBasePrice()).available(p.isAvailable()).build();
        });

        ProductDto result = adminService.createProduct(request);

        assertThat(result.getName()).isEqualTo("Cappuccino");
    }

    @Test
    void updateProduct_notFound_throwsNoSuchElement() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated");
        request.setCategory(ProductCategory.COFFEE);
        request.setBasePrice(BigDecimal.valueOf(500));

        assertThatThrownBy(() -> adminService.updateProduct(99L, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteProduct_existingProduct_deletesIt() {
        when(productRepository.existsById(1L)).thenReturn(true);

        adminService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void createTopping_validRequest_returnsDto() {
        CreateToppingRequest request = new CreateToppingRequest();
        request.setName("Soy Milk");
        request.setType(ToppingType.MILK);
        request.setPrice(BigDecimal.valueOf(80));

        when(toppingRepository.save(any(Topping.class))).thenAnswer(inv -> {
            Topping t = inv.getArgument(0);
            return Topping.builder().id(2L).name(t.getName()).type(t.getType())
                    .price(t.getPrice()).build();
        });

        ToppingDto result = adminService.createTopping(request);

        assertThat(result.getName()).isEqualTo("Soy Milk");
    }

    @Test
    void deleteTopping_notFound_throwsNoSuchElement() {
        when(toppingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteTopping(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void printOrder_existingOrder_callsPrintService() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        doNothing().when(printService).printReceipt(order);

        adminService.printOrder(1L);

        verify(printService).printReceipt(order);
    }

    @Test
    void printOrder_notFound_throwsNoSuchElement() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.printOrder(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
