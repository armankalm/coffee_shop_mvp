package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.*;
import com.coffeeshop.app.repository.CityRepository;
import com.coffeeshop.app.service.print.PrintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CoffeeShopRepository coffeeShopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PrintService printService;
    @Mock private RefOrderStatusRepository refOrderStatusRepository;
    @Mock private RefShopStatusRepository refShopStatusRepository;
    @Mock private RefProductCategoryRepository refProductCategoryRepository;
    @Mock private RefToppingTypeRepository refToppingTypeRepository;
    @Mock private CityRepository cityRepository;

    @InjectMocks
    private AdminService adminService;

    private RefUserRole userRole;
    private RefShopStatus openStatus;
    private RefOrderStatus newStatus;
    private RefOrderStatus inProgressStatus;
    private RefProductCategory coffeeCategory;
    private RefToppingType milkType;

    private RefUserRole adminRole;
    private User user;
    private User adminUser;
    private CoffeeShop shop;
    private Product product;
    private Topping topping;
    private Order order;

    @BeforeEach
    void setUp() {
        userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        adminRole = RefUserRole.builder().id(4L).code("ADMIN").nameRu("Администратор").nameEn("Admin").build();
        openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();
        inProgressStatus = RefOrderStatus.builder().id(2L).code("IN_PROGRESS").nameRu("В работе").nameEn("In Progress").build();
        coffeeCategory = RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
        milkType = RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build();

        user = User.builder().id(1L).email("user@test.com").role(userRole).build();
        City almatyCity = City.builder().id(1L).name("Almaty").active(true).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city(almatyCity)
                .address("123 St").status(openStatus).build();
        adminUser = User.builder().id(2L).email("admin@test.com").role(adminRole)
                .assignedShops(new LinkedHashSet<>(Set.of(shop))).build();
        product = Product.builder().id(1L).name("Latte").category(coffeeCategory)
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
        topping = Topping.builder().id(1L).name("Oat Milk").type(milkType)
                .price(BigDecimal.valueOf(100)).build();
        order = Order.builder().id(1L).user(user).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).build();
    }

    @Test
    void getAllOrders_noFilters_returnsAssignedShopsOrders() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByShopIdInWithDetails(Set.of(1L))).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders("admin@test.com", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getAllOrders_noAssignedShops_returnsEmpty() {
        User unassigned = User.builder().id(9L).email("admin@test.com").role(adminRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(unassigned));

        List<OrderDto> result = adminService.getAllOrders("admin@test.com", null, null);

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findByShopIdInWithDetails(any());
    }

    @Test
    void getAllOrders_filterByStatus_returnsMatchingOrders() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(refOrderStatusRepository.findByCode("NEW")).thenReturn(Optional.of(newStatus));
        when(orderRepository.findByStatusAndShopIdInWithDetails(newStatus, Set.of(1L))).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders("admin@test.com", "NEW", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("NEW");
    }

    @Test
    void getAllOrders_filterByShopId_returnsMatchingOrders() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByShopIdInWithDetails(Set.of(1L))).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders("admin@test.com", null, 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllOrders_filterByUnassignedShopId_throwsAccessDenied() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.getAllOrders("admin@test.com", null, 999L))
                .isInstanceOf(com.coffeeshop.app.config.AccessDeniedException.class);
    }

    @Test
    void getAllOrders_filterByStatusAndShopId_returnsMatchingOrders() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(refOrderStatusRepository.findByCode("NEW")).thenReturn(Optional.of(newStatus));
        when(orderRepository.findByStatusAndShopIdInWithDetails(newStatus, Set.of(1L))).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders("admin@test.com", "NEW", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(orderRepository).findByStatusAndShopIdInWithDetails(newStatus, Set.of(1L));
    }

    @Test
    void getAllOrders_asBarista_scopedToAssignedShops() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        User baristaUser = User.builder().id(3L).email("barista@test.com").role(baristaRole)
                .assignedShops(new LinkedHashSet<>(Set.of(shop))).build();
        when(userRepository.findByEmailWithAssignedShops("barista@test.com")).thenReturn(Optional.of(baristaUser));
        when(orderRepository.findByShopIdInWithDetails(Set.of(1L))).thenReturn(List.of(order));

        List<OrderDto> result = adminService.getAllOrders("barista@test.com", null, null);

        assertThat(result).hasSize(1);
        verify(orderRepository).findByShopIdInWithDetails(Set.of(1L));
    }

    @Test
    void getOrderById_existingOrder_returnsDto() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));

        OrderDto result = adminService.getOrderById("admin@test.com", 1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_notAssignedToShop_throwsAccessDenied() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        User baristaUser = User.builder().id(3L).email("barista@test.com").role(baristaRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailWithAssignedShops("barista@test.com")).thenReturn(Optional.of(baristaUser));

        assertThatThrownBy(() -> adminService.getOrderById("barista@test.com", 1L))
                .isInstanceOf(com.coffeeshop.app.config.AccessDeniedException.class);
    }

    @Test
    void getOrderById_notFound_throwsNoSuchElement() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getOrderById("admin@test.com", 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateOrderStatus_validOrder_updatesStatus() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(refOrderStatusRepository.findByCode("IN_PROGRESS")).thenReturn(Optional.of(inProgressStatus));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = adminService.updateOrderStatus("admin@test.com", 1L, "IN_PROGRESS");

        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void updateOrderStatus_notAssignedToShop_throwsAccessDenied() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        User baristaUser = User.builder().id(3L).email("barista@test.com").role(baristaRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(userRepository.findByEmailWithAssignedShops("barista@test.com")).thenReturn(Optional.of(baristaUser));
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminService.updateOrderStatus("barista@test.com", 1L, "IN_PROGRESS"))
                .isInstanceOf(com.coffeeshop.app.config.AccessDeniedException.class);
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsIllegalState() {
        RefOrderStatus completedStatus = RefOrderStatus.builder().id(3L).code("COMPLETED").nameRu("Завершён").nameEn("Completed").build();
        order.setStatus(completedStatus);
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminService.updateOrderStatus("admin@test.com", 1L, "IN_PROGRESS"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void updateOrderStatus_notFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithAssignedShops("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateOrderStatus("admin@test.com", 99L, "IN_PROGRESS"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void assignShop_validStaff_addsShop() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        User baristaUser = User.builder().id(3L).email("barista@test.com").role(baristaRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(userRepository.findByIdWithAssignedShops(3L)).thenReturn(Optional.of(baristaUser));
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = adminService.assignShop(3L, 1L);

        assertThat(result.getAssignedShops()).hasSize(1);
        assertThat(baristaUser.getAssignedShops()).contains(shop);
    }

    @Test
    void assignShop_toRegularUser_throwsIllegalArgument() {
        User regular = User.builder().id(1L).email("user@test.com").role(userRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(userRepository.findByIdWithAssignedShops(1L)).thenReturn(Optional.of(regular));

        assertThatThrownBy(() -> adminService.assignShop(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unassignShop_notAssigned_throwsNoSuchElement() {
        RefUserRole baristaRole = RefUserRole.builder().id(3L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        User baristaUser = User.builder().id(3L).email("barista@test.com").role(baristaRole)
                .assignedShops(new LinkedHashSet<>()).build();
        when(userRepository.findByIdWithAssignedShops(3L)).thenReturn(Optional.of(baristaUser));

        assertThatThrownBy(() -> adminService.unassignShop(3L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createShop_validRequest_returnsDto() {
        City astanaCity = City.builder().id(2L).name("Astana").active(true).build();
        CreateShopRequest request = new CreateShopRequest();
        request.setName("New Shop");
        request.setCityId(2L);
        request.setAddress("456 Ave");
        request.setStatusCode("OPEN");

        when(refShopStatusRepository.findByCode("OPEN")).thenReturn(Optional.of(openStatus));
        when(cityRepository.findById(2L)).thenReturn(Optional.of(astanaCity));
        when(coffeeShopRepository.save(any(CoffeeShop.class))).thenAnswer(inv -> {
            CoffeeShop s = inv.getArgument(0);
            return CoffeeShop.builder().id(2L).name(s.getName()).city(s.getCity())
                    .address(s.getAddress()).status(s.getStatus()).build();
        });

        CoffeeShopDto result = adminService.createShop(request);

        assertThat(result.getName()).isEqualTo("New Shop");
        assertThat(result.getCity().getName()).isEqualTo("Astana");
    }

    @Test
    void updateShop_notFound_throwsNoSuchElement() {
        when(coffeeShopRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        CreateShopRequest request = new CreateShopRequest();
        request.setName("Updated");
        request.setCityId(1L);
        request.setAddress("Addr");
        request.setStatusCode("OPEN");

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
        request.setShopId(1L);
        request.setName("Cappuccino");
        request.setCategoryCode("COFFEE");
        request.setBasePrice(BigDecimal.valueOf(450));
        request.setAvailable(true);

        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(refProductCategoryRepository.findByCode("COFFEE")).thenReturn(Optional.of(coffeeCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return Product.builder().id(2L).name(p.getName()).coffeeShop(p.getCoffeeShop()).category(p.getCategory())
                    .basePrice(p.getBasePrice()).available(p.isAvailable()).build();
        });

        ProductDto result = adminService.createProduct(request);

        assertThat(result.getName()).isEqualTo("Cappuccino");
    }

    @Test
    void updateProduct_notFound_throwsNoSuchElement() {
        when(productRepository.findByIdWithToppings(99L)).thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated");
        request.setCategoryCode("COFFEE");
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
        request.setTypeCode("MILK");
        request.setPrice(BigDecimal.valueOf(80));

        when(refToppingTypeRepository.findByCode("MILK")).thenReturn(Optional.of(milkType));
        when(toppingRepository.save(any(Topping.class))).thenAnswer(inv -> {
            Topping t = inv.getArgument(0);
            return Topping.builder().id(2L).name(t.getName()).type(t.getType())
                    .price(t.getPrice()).build();
        });

        ToppingDto result = adminService.createTopping(request);

        assertThat(result.getName()).isEqualTo("Soy Milk");
    }

    @Test
    void updateTopping_validRequest_returnsDto() {
        CreateToppingRequest request = new CreateToppingRequest();
        request.setName("Almond Milk");
        request.setTypeCode("MILK");
        request.setPrice(BigDecimal.valueOf(120));

        when(toppingRepository.findByIdWithIncompatibilities(1L)).thenReturn(Optional.of(topping));
        when(refToppingTypeRepository.findByCode("MILK")).thenReturn(Optional.of(milkType));
        when(toppingRepository.save(any(Topping.class))).thenAnswer(inv -> inv.getArgument(0));

        ToppingDto result = adminService.updateTopping(1L, request);

        assertThat(result.getName()).isEqualTo("Almond Milk");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(120));
    }

    @Test
    void deleteTopping_notFound_throwsNoSuchElement() {
        when(toppingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteTopping(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAllWithRole()).thenReturn(List.of(user));

        List<UserDto> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void printOrder_existingOrder_callsPrintService() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        doNothing().when(printService).printReceipt(order);

        adminService.printOrder(1L);

        verify(printService).printReceipt(order);
    }

    @Test
    void printOrder_notFound_throwsNoSuchElement() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.printOrder(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
