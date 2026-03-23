package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoDataServiceTest {

    @Mock private CityRepository cityRepository;
    @Mock private CoffeeShopRepository coffeeShopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private SavedCombinationRepository savedCombinationRepository;
    @Mock private FavoriteItemRepository favoriteItemRepository;
    @Mock private RefShopStatusRepository refShopStatusRepository;
    @Mock private RefOrderStatusRepository refOrderStatusRepository;
    @Mock private RefProductCategoryRepository refProductCategoryRepository;
    @Mock private RefToppingTypeRepository refToppingTypeRepository;
    @Mock private RefUserRoleRepository refUserRoleRepository;

    @InjectMocks
    private DemoDataService demoDataService;

    private RefShopStatus openStatus;
    private RefShopStatus closedStatus;
    private RefOrderStatus newStatus;
    private RefOrderStatus inProgressStatus;
    private RefOrderStatus readyStatus;
    private RefOrderStatus completedStatus;
    private RefOrderStatus cancelledStatus;
    private RefProductCategory coffeeCategory;
    private RefProductCategory teaCategory;
    private RefProductCategory coldDrinksCategory;
    private RefProductCategory foodCategory;
    private RefProductCategory dessertsCategory;
    private RefToppingType milkType;
    private RefToppingType syrupType;
    private RefToppingType toppingType;
    private RefToppingType extrasType;
    private RefUserRole userRole;
    private RefUserRole baristaRole;
    private RefUserRole managerRole;
    private RefUserRole adminRole;

    @BeforeEach
    void setUp() {
        openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        closedStatus = RefShopStatus.builder().id(2L).code("CLOSED").nameRu("Закрыто").nameEn("Closed").build();
        newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();
        inProgressStatus = RefOrderStatus.builder().id(2L).code("IN_PROGRESS").nameRu("В работе").nameEn("In Progress").build();
        readyStatus = RefOrderStatus.builder().id(3L).code("READY").nameRu("Готов").nameEn("Ready").build();
        completedStatus = RefOrderStatus.builder().id(4L).code("COMPLETED").nameRu("Завершён").nameEn("Completed").build();
        cancelledStatus = RefOrderStatus.builder().id(5L).code("CANCELLED").nameRu("Отменён").nameEn("Cancelled").build();
        coffeeCategory = RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
        teaCategory = RefProductCategory.builder().id(2L).code("TEA").nameRu("Чай").nameEn("Tea").build();
        coldDrinksCategory = RefProductCategory.builder().id(3L).code("COLD_DRINKS").nameRu("Холодные").nameEn("Cold Drinks").build();
        foodCategory = RefProductCategory.builder().id(4L).code("FOOD").nameRu("Еда").nameEn("Food").build();
        dessertsCategory = RefProductCategory.builder().id(5L).code("DESSERTS").nameRu("Десерты").nameEn("Desserts").build();
        milkType = RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build();
        syrupType = RefToppingType.builder().id(2L).code("SYRUP").nameRu("Сиропы").nameEn("Syrup").build();
        toppingType = RefToppingType.builder().id(3L).code("TOPPING").nameRu("Добавки").nameEn("Topping").build();
        extrasType = RefToppingType.builder().id(4L).code("EXTRAS").nameRu("Экстра").nameEn("Extras").build();
        userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        baristaRole = RefUserRole.builder().id(2L).code("BARISTA").nameRu("Бариста").nameEn("Barista").build();
        managerRole = RefUserRole.builder().id(3L).code("MANAGER").nameRu("Менеджер").nameEn("Manager").build();
        adminRole = RefUserRole.builder().id(4L).code("ADMIN").nameRu("Администратор").nameEn("Admin").build();
    }

    @Test
    void stats_returnsCountsFromAllRepositories() {
        when(cityRepository.count()).thenReturn(5L);
        when(coffeeShopRepository.count()).thenReturn(10L);
        when(productRepository.count()).thenReturn(50L);
        when(toppingRepository.count()).thenReturn(30L);
        when(userRepository.count()).thenReturn(5L);
        when(orderRepository.count()).thenReturn(20L);
        when(savedCombinationRepository.count()).thenReturn(10L);

        Map<String, Long> stats = demoDataService.stats();

        assertThat(stats).containsEntry("cities", 5L);
        assertThat(stats).containsEntry("shops", 10L);
        assertThat(stats).containsEntry("products", 50L);
        assertThat(stats).containsEntry("toppings", 30L);
        assertThat(stats).containsEntry("users", 5L);
        assertThat(stats).containsEntry("orders", 20L);
        assertThat(stats).containsEntry("savedCombinations", 10L);
    }

    @Test
    void seedIfEmpty_whenUsersExist_doesNotSeed() {
        when(userRepository.count()).thenReturn(3L);

        demoDataService.seedIfEmpty();

        verify(cityRepository, never()).save(any());
    }

    @Test
    void seedIfEmpty_whenEmpty_seedsData() {
        when(userRepository.count()).thenReturn(0L);
        stubAllRefRepositories();
        stubSaveRepositories();

        demoDataService.seedIfEmpty();

        verify(cityRepository, atLeast(5)).save(any(City.class));
        verify(coffeeShopRepository, atLeast(10)).save(any(CoffeeShop.class));
        verify(toppingRepository, atLeast(30)).save(any(Topping.class));
        verify(productRepository, atLeast(50)).save(any(Product.class));
        verify(userRepository, atLeast(5)).save(any(User.class));
    }

    @Test
    void reset_clearsAndReseeds() {
        stubAllRefRepositories();
        stubSaveRepositories();

        demoDataService.reset();

        verify(savedCombinationRepository).deleteAll();
        verify(orderRepository).deleteAll();
        verify(userRepository).deleteAll();
        verify(coffeeShopRepository).deleteAll();
        verify(productRepository).deleteAll();
        verify(toppingRepository).deleteAll();
        verify(cityRepository).deleteAll();

        verify(cityRepository, atLeast(5)).save(any(City.class));
    }

    @Test
    void reset_whenShopStatusMissing_throwsNoSuchElement() {
        stubAllRefRepositoriesExcept("OPEN");

        assertThatThrownBy(() -> demoDataService.reset())
                .isInstanceOf(NoSuchElementException.class);
    }

    private void stubAllRefRepositories() {
        when(refShopStatusRepository.findByCode("OPEN")).thenReturn(Optional.of(openStatus));
        when(refShopStatusRepository.findByCode("CLOSED")).thenReturn(Optional.of(closedStatus));
        when(refOrderStatusRepository.findByCode("NEW")).thenReturn(Optional.of(newStatus));
        when(refOrderStatusRepository.findByCode("IN_PROGRESS")).thenReturn(Optional.of(inProgressStatus));
        when(refOrderStatusRepository.findByCode("READY")).thenReturn(Optional.of(readyStatus));
        when(refOrderStatusRepository.findByCode("COMPLETED")).thenReturn(Optional.of(completedStatus));
        when(refOrderStatusRepository.findByCode("CANCELLED")).thenReturn(Optional.of(cancelledStatus));
        when(refProductCategoryRepository.findByCode("COFFEE")).thenReturn(Optional.of(coffeeCategory));
        when(refProductCategoryRepository.findByCode("TEA")).thenReturn(Optional.of(teaCategory));
        when(refProductCategoryRepository.findByCode("COLD_DRINKS")).thenReturn(Optional.of(coldDrinksCategory));
        when(refProductCategoryRepository.findByCode("FOOD")).thenReturn(Optional.of(foodCategory));
        when(refProductCategoryRepository.findByCode("DESSERTS")).thenReturn(Optional.of(dessertsCategory));
        when(refToppingTypeRepository.findByCode("MILK")).thenReturn(Optional.of(milkType));
        when(refToppingTypeRepository.findByCode("SYRUP")).thenReturn(Optional.of(syrupType));
        when(refToppingTypeRepository.findByCode("TOPPING")).thenReturn(Optional.of(toppingType));
        when(refToppingTypeRepository.findByCode("EXTRAS")).thenReturn(Optional.of(extrasType));
        when(refUserRoleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(refUserRoleRepository.findByCode("BARISTA")).thenReturn(Optional.of(baristaRole));
        when(refUserRoleRepository.findByCode("MANAGER")).thenReturn(Optional.of(managerRole));
        when(refUserRoleRepository.findByCode("ADMIN")).thenReturn(Optional.of(adminRole));
    }

    private void stubAllRefRepositoriesExcept(String excludeShopStatusCode) {
        if (!"OPEN".equals(excludeShopStatusCode)) {
            when(refShopStatusRepository.findByCode("OPEN")).thenReturn(Optional.of(openStatus));
        } else {
            when(refShopStatusRepository.findByCode("OPEN")).thenReturn(Optional.empty());
        }
    }

    private void stubSaveRepositories() {
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> {
            City c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(coffeeShopRepository.save(any(CoffeeShop.class))).thenAnswer(inv -> {
            CoffeeShop s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(toppingRepository.save(any(Topping.class))).thenAnswer(inv -> {
            Topping t = inv.getArgument(0);
            t.setId(1L);
            t.setPrice(t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO);
            return t;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(savedCombinationRepository.save(any(SavedCombination.class))).thenAnswer(inv -> {
            SavedCombination sc = inv.getArgument(0);
            sc.setId(1L);
            return sc;
        });
    }
}
