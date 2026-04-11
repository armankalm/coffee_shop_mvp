package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PrintServiceTest {

    private EscPosPrintService printService;
    private ReceiptFormatter receiptFormatter;

    @BeforeEach
    void setUp() {
        // Printer disabled so no real socket connections are made
        printService = new EscPosPrintService("localhost", 9100, false, 5000);
        receiptFormatter = new ReceiptFormatter();
    }

    private RefUserRole userRole() {
        return RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
    }

    private RefShopStatus openStatus() {
        return RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
    }

    private RefOrderStatus newStatus() {
        return RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();
    }

    private RefProductCategory coffeeCategory() {
        return RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
    }

    private RefToppingType milkType() {
        return RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build();
    }

    private Order buildOrder() {
        User user = User.builder().id(1L).email("customer@test.com").role(userRole()).build();
        City almatyCity = City.builder().id(1L).name("Almaty").active(true).build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Central Coffee")
                .city(almatyCity).address("10 Main St").status(openStatus()).build();
        Product product = Product.builder().id(1L).name("Latte").category(coffeeCategory())
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
        Topping topping = Topping.builder().id(1L).name("Oat Milk").type(milkType())
                .price(BigDecimal.valueOf(100)).build();

        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(product)
                .toppings(Set.of(topping))
                .quantity(2)
                .price(BigDecimal.valueOf(1200))
                .build();

        Order order = Order.builder()
                .id(42L)
                .user(user)
                .shop(shop)
                .status(newStatus())
                .total(BigDecimal.valueOf(1200))
                .items(Set.of(item))
                .build();
        // Set createdAt manually since @PrePersist won't run in unit tests
        order.setCreatedAt(Instant.parse("2026-03-22T10:00:00Z"));
        return order;
    }

    @Test
    void printReceipt_printerDisabled_doesNotThrow() {
        Order order = buildOrder();
        assertThatNoException().isThrownBy(() -> printService.printReceipt(order));
    }

    @Test
    void isPrinterAvailable_printerDisabled_returnsFalse() {
        assertThat(printService.isPrinterAvailable()).isFalse();
    }

    @Test
    void receiptFormatter_containsOrderId() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("42");
    }

    @Test
    void receiptFormatter_containsShopName() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("Central Coffee");
    }

    @Test
    void receiptFormatter_containsProductName() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("Latte");
    }

    @Test
    void receiptFormatter_containsToppingName() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("Oat Milk");
    }

    @Test
    void receiptFormatter_containsTotal() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("1200");
    }

    @Test
    void receiptFormatter_containsCustomerEmail() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        assertThat(receipt).contains("c***@test.com");
    }

    @Test
    void receiptFormatter_containsBarcodePattern() {
        Order order = buildOrder();
        String receipt = receiptFormatter.format(order);
        // Barcode line: * 000000000042 *
        assertThat(receipt).contains("000000000042");
    }

    @Test
    void receiptFormatter_noItemOrder_stillFormats() {
        Order order = Order.builder()
                .id(1L)
                .user(User.builder().id(1L).email("test@test.com").role(userRole()).build())
                .shop(CoffeeShop.builder().id(1L).name("Shop")
                        .city(City.builder().id(1L).name("City").active(true).build())
                        .address("Addr").status(openStatus()).build())
                .status(newStatus())
                .total(BigDecimal.ZERO)
                .items(Set.of())
                .build();
        order.setCreatedAt(Instant.now());

        String receipt = receiptFormatter.format(order);
        assertThat(receipt).isNotBlank();
        assertThat(receipt).contains("Shop");
    }
}
