package com.coffeeshop.app.domain;

import com.coffeeshop.app.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntityRelationshipTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CoffeeShopRepository coffeeShopRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ToppingRepository toppingRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SavedCombinationRepository savedCombinationRepository;
    @Autowired
    private FavoriteItemRepository favoriteItemRepository;

    @Test
    void saveAndFindUser() {
        User user = User.builder()
                .email("test@example.com")
                .role(Role.USER)
                .build();
        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }

    @Test
    void saveAndFindCoffeeShop() {
        CoffeeShop shop = CoffeeShop.builder()
                .name("Central Coffee")
                .city("Almaty")
                .address("Dostyk 1")
                .status(ShopStatus.OPEN)
                .build();
        CoffeeShop saved = coffeeShopRepository.save(shop);
        assertThat(saved.getId()).isNotNull();
        assertThat(coffeeShopRepository.findByCity("Almaty")).hasSize(1);
    }

    @Test
    void saveProductWithTopping() {
        Topping milk = Topping.builder()
                .name("Oat Milk")
                .type(ToppingType.MILK)
                .price(new BigDecimal("150.00"))
                .build();
        toppingRepository.save(milk);

        Product latte = Product.builder()
                .name("Latte")
                .category(ProductCategory.COFFEE)
                .basePrice(new BigDecimal("1200.00"))
                .available(true)
                .build();
        latte.getAvailableToppings().add(milk);
        productRepository.save(latte);

        assertThat(productRepository.findByAvailableTrue()).hasSize(1);
        assertThat(toppingRepository.findByType(ToppingType.MILK)).hasSize(1);
    }

    @Test
    void saveOrderWithItems() {
        User user = userRepository.save(User.builder()
                .email("order@example.com").role(Role.USER).build());
        CoffeeShop shop = coffeeShopRepository.save(CoffeeShop.builder()
                .name("Shop").city("City").address("Addr").status(ShopStatus.OPEN).build());
        Product product = productRepository.save(Product.builder()
                .name("Espresso").category(ProductCategory.COFFEE)
                .basePrice(new BigDecimal("800.00")).available(true).build());

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .price(new BigDecimal("1600.00"))
                .build();

        Order order = Order.builder()
                .user(user)
                .shop(shop)
                .status(OrderStatus.NEW)
                .total(new BigDecimal("1600.00"))
                .build();
        item.setOrder(order);
        order.getItems().add(item);

        Order saved = orderRepository.save(order);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(orderRepository.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void saveFavoriteItem() {
        User user = userRepository.save(User.builder()
                .email("fav@example.com").role(Role.USER).build());
        Product product = productRepository.save(Product.builder()
                .name("Cappuccino").category(ProductCategory.COFFEE)
                .basePrice(new BigDecimal("1000.00")).available(true).build());

        SavedCombination combo = savedCombinationRepository.save(SavedCombination.builder()
                .user(user).product(product).name("My Cappuccino").build());

        FavoriteItem fav = favoriteItemRepository.save(FavoriteItem.builder()
                .user(user).savedCombination(combo).build());

        assertThat(fav.getId()).isNotNull();
        assertThat(favoriteItemRepository.findByUserId(user.getId())).hasSize(1);
        assertThat(favoriteItemRepository.existsByUserIdAndSavedCombinationId(
                user.getId(), combo.getId())).isTrue();
    }

    @Test
    void toppingIncompatibility() {
        Topping cow = Topping.builder()
                .name("Cow Milk").type(ToppingType.MILK).price(new BigDecimal("100.00")).build();
        Topping oat = Topping.builder()
                .name("Oat Milk").type(ToppingType.MILK).price(new BigDecimal("150.00")).build();
        toppingRepository.save(oat);
        cow.getIncompatibleWith().add(oat);
        toppingRepository.save(cow);

        Topping found = toppingRepository.findById(cow.getId()).orElseThrow();
        assertThat(found.getIncompatibleWith()).hasSize(1);
    }
}
