package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Profile("demo")
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final CityRepository cityRepository;
    private final CoffeeShopRepository coffeeShopRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SavedCombinationRepository savedCombinationRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefShopStatusRepository refShopStatusRepository;
    private final RefOrderStatusRepository refOrderStatusRepository;
    private final RefProductCategoryRepository refProductCategoryRepository;
    private final RefToppingTypeRepository refToppingTypeRepository;
    private final RefUserRoleRepository refUserRoleRepository;

    public DemoDataService(CityRepository cityRepository,
                           CoffeeShopRepository coffeeShopRepository,
                           ProductRepository productRepository,
                           ToppingRepository toppingRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           SavedCombinationRepository savedCombinationRepository,
                           FavoriteItemRepository favoriteItemRepository,
                           PaymentTransactionRepository paymentTransactionRepository,
                           RefShopStatusRepository refShopStatusRepository,
                           RefOrderStatusRepository refOrderStatusRepository,
                           RefProductCategoryRepository refProductCategoryRepository,
                           RefToppingTypeRepository refToppingTypeRepository,
                           RefUserRoleRepository refUserRoleRepository) {
        this.cityRepository = cityRepository;
        this.coffeeShopRepository = coffeeShopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.savedCombinationRepository = savedCombinationRepository;
        this.favoriteItemRepository = favoriteItemRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.refShopStatusRepository = refShopStatusRepository;
        this.refOrderStatusRepository = refOrderStatusRepository;
        this.refProductCategoryRepository = refProductCategoryRepository;
        this.refToppingTypeRepository = refToppingTypeRepository;
        this.refUserRoleRepository = refUserRoleRepository;
    }

    @Transactional
    public void reset() {
        log.info("Resetting demo data...");
        clearAll();
        seed();
        log.info("Demo data reset complete.");
    }

    @Transactional
    public void seedIfEmpty() {
        if (userRepository.count() == 0) {
            log.info("No data found, seeding demo data...");
            seed();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("cities", cityRepository.count());
        stats.put("shops", coffeeShopRepository.count());
        stats.put("products", productRepository.count());
        stats.put("toppings", toppingRepository.count());
        stats.put("users", userRepository.count());
        stats.put("orders", orderRepository.count());
        stats.put("savedCombinations", savedCombinationRepository.count());
        return stats;
    }

    private void clearAll() {
        favoriteItemRepository.deleteAll();
        savedCombinationRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        coffeeShopRepository.deleteAll();
        productRepository.deleteAll();
        toppingRepository.deleteAllIncompatibilities();
        toppingRepository.deleteAll();
        cityRepository.deleteAll();
    }

    private void seed() {
        List<City> cities = seedCities();
        List<CoffeeShop> shops = seedShops(cities);
        List<Topping> toppings = seedToppings();
        List<Product> products = seedProducts(shops, toppings);
        List<User> users = seedUsers();
        seedOrders(users, shops, products, toppings);
        seedSavedCombinations(users, products, toppings);
    }

    private List<City> seedCities() {
        List<City> cities = new ArrayList<>();
        cities.add(cityRepository.save(City.builder().name("Алматы").region("Алматинская").country("Казахстан").active(true).build()));
        cities.add(cityRepository.save(City.builder().name("Астана").region("Акмолинская").country("Казахстан").active(true).build()));
        cities.add(cityRepository.save(City.builder().name("Шымкент").region("Туркестанская").country("Казахстан").active(true).build()));
        cities.add(cityRepository.save(City.builder().name("Актобе").region("Актюбинская").country("Казахстан").active(true).build()));
        cities.add(cityRepository.save(City.builder().name("Караганда").region("Карагандинская").country("Казахстан").active(true).build()));
        return cities;
    }

    private List<CoffeeShop> seedShops(List<City> cities) {
        RefShopStatus open = refShopStatusRepository.findByCode("OPEN")
                .orElseThrow(() -> new NoSuchElementException("Shop status OPEN not found"));
        RefShopStatus closed = refShopStatusRepository.findByCode("CLOSED")
                .orElseThrow(() -> new NoSuchElementException("Shop status CLOSED not found"));

        List<CoffeeShop> shops = new ArrayList<>();
        // Алматы - 2 shops
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Алматы Центр").city(cities.get(0)).address("пр. Достык, 1").status(open).build()));
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Алматы Юг").city(cities.get(0)).address("ул. Аль-Фараби, 77").status(open).build()));
        // Астана - 2 shops
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Астана Байтерек").city(cities.get(1)).address("пр. Нурсултан, 14").status(open).build()));
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Астана Хан Шатыр").city(cities.get(1)).address("пр. Туран, 37").status(closed).build()));
        // Шымкент - 2 shops
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Шымкент Центр").city(cities.get(2)).address("пр. Республики, 10").status(open).build()));
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Шымкент Север").city(cities.get(2)).address("ул. Байдибек би, 5").status(open).build()));
        // Актобе - 2 shops
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Актобе Центр").city(cities.get(3)).address("пр. Абилкайыр Хана, 15").status(open).build()));
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Актобе Сити").city(cities.get(3)).address("ул. Маресьева, 22").status(open).build()));
        // Карагнда - 2 shops
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Карагнда Центр").city(cities.get(4)).address("пр. Бухар-Жырау, 50").status(open).build()));
        shops.add(coffeeShopRepository.save(CoffeeShop.builder().name("Brew & Co Карагнда Михайловка").city(cities.get(4)).address("ул. Ерубаева, 3").status(open).build()));
        return shops;
    }

    private List<Topping> seedToppings() {
        RefToppingType milk = refToppingTypeRepository.findByCode("MILK")
                .orElseThrow(() -> new NoSuchElementException("Topping type MILK not found"));
        RefToppingType syrup = refToppingTypeRepository.findByCode("SYRUP")
                .orElseThrow(() -> new NoSuchElementException("Topping type SYRUP not found"));
        RefToppingType topping = refToppingTypeRepository.findByCode("TOPPING")
                .orElseThrow(() -> new NoSuchElementException("Topping type TOPPING not found"));
        RefToppingType extras = refToppingTypeRepository.findByCode("EXTRAS")
                .orElseThrow(() -> new NoSuchElementException("Topping type EXTRAS not found"));

        List<Topping> toppings = new ArrayList<>();

        // Молоко (MILK) - 7 items
        toppings.add(toppingRepository.save(Topping.builder().name("Цельное молоко").type(milk).price(new BigDecimal("50")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Обезжиренное молоко").type(milk).price(new BigDecimal("50")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Соевое молоко").type(milk).price(new BigDecimal("100")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Овсяное молоко").type(milk).price(new BigDecimal("120")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Кокосовое молоко").type(milk).price(new BigDecimal("120")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Миндальное молоко").type(milk).price(new BigDecimal("130")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Безлактозное молоко").type(milk).price(new BigDecimal("80")).build()));

        // Сиропы (SYRUP) - 8 items
        toppings.add(toppingRepository.save(Topping.builder().name("Ванильный сироп").type(syrup).price(new BigDecimal("60")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Карамельный сироп").type(syrup).price(new BigDecimal("60")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Лесной орех").type(syrup).price(new BigDecimal("70")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Кокосовый сироп").type(syrup).price(new BigDecimal("70")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Шоколадный сироп").type(syrup).price(new BigDecimal("60")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Лавандовый сироп").type(syrup).price(new BigDecimal("80")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Сироп маракуйи").type(syrup).price(new BigDecimal("80")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Имбирный сироп").type(syrup).price(new BigDecimal("70")).build()));

        // Добавки (TOPPING) - 8 items
        toppings.add(toppingRepository.save(Topping.builder().name("Взбитые сливки").type(topping).price(new BigDecimal("100")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Шоколадная крошка").type(topping).price(new BigDecimal("80")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Корица").type(topping).price(new BigDecimal("30")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Какао-порошок").type(topping).price(new BigDecimal("30")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Маршмелоу").type(topping).price(new BigDecimal("90")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Карамельная крошка").type(topping).price(new BigDecimal("80")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Цедра апельсина").type(topping).price(new BigDecimal("50")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Мята").type(topping).price(new BigDecimal("40")).build()));

        // Экстра (EXTRAS) - 7 items
        toppings.add(toppingRepository.save(Topping.builder().name("Дополнительный эспрессо").type(extras).price(new BigDecimal("150")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Без кофеина").type(extras).price(new BigDecimal("50")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Холодный лёд").type(extras).price(new BigDecimal("30")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Двойная порция").type(extras).price(new BigDecimal("200")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Сахар тростниковый").type(extras).price(new BigDecimal("20")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Подогрев стакана").type(extras).price(new BigDecimal("0")).build()));
        toppings.add(toppingRepository.save(Topping.builder().name("Без сахара").type(extras).price(new BigDecimal("0")).build()));

        return toppings;
    }

    private List<Product> seedProducts(List<CoffeeShop> shops, List<Topping> toppings) {
        List<Product> allProducts = new ArrayList<>();
        for (int shopIdx = 0; shopIdx < Math.min(shops.size(), 3); shopIdx++) {
            allProducts.addAll(seedProductsForShop(shops.get(shopIdx), toppings));
        }
        return allProducts;
    }

    private List<Product> seedProductsForShop(CoffeeShop shop, List<Topping> toppings) {
        CoffeeShop defaultShop = shop;
        RefProductCategory coffee = refProductCategoryRepository.findByCode("COFFEE")
                .orElseThrow(() -> new NoSuchElementException("Category COFFEE not found"));
        RefProductCategory tea = refProductCategoryRepository.findByCode("TEA")
                .orElseThrow(() -> new NoSuchElementException("Category TEA not found"));
        RefProductCategory coldDrinks = refProductCategoryRepository.findByCode("COLD_DRINKS")
                .orElseThrow(() -> new NoSuchElementException("Category COLD_DRINKS not found"));
        RefProductCategory food = refProductCategoryRepository.findByCode("FOOD")
                .orElseThrow(() -> new NoSuchElementException("Category FOOD not found"));
        RefProductCategory desserts = refProductCategoryRepository.findByCode("DESSERTS")
                .orElseThrow(() -> new NoSuchElementException("Category DESSERTS not found"));

        // Milk toppings (indices 0-6)
        Set<Topping> milkToppings = new HashSet<>(toppings.subList(0, 7));
        // Syrup toppings (indices 7-14)
        Set<Topping> syrupToppings = new HashSet<>(toppings.subList(7, 15));
        // Topping toppings (indices 15-22)
        Set<Topping> toppingAddons = new HashSet<>(toppings.subList(15, 23));
        // Extras (indices 23-29)
        Set<Topping> extrasToppings = new HashSet<>(toppings.subList(23, 30));

        Set<Topping> coffeeBaseToppings = new HashSet<>();
        coffeeBaseToppings.addAll(milkToppings);
        coffeeBaseToppings.addAll(syrupToppings);
        coffeeBaseToppings.addAll(toppingAddons);
        coffeeBaseToppings.addAll(extrasToppings);

        Set<Topping> teaToppings = new HashSet<>();
        teaToppings.addAll(syrupToppings);
        teaToppings.addAll(toppingAddons);

        Set<Topping> coldToppings = new HashSet<>();
        coldToppings.addAll(milkToppings);
        coldToppings.addAll(syrupToppings);
        coldToppings.add(toppings.get(25)); // Холодный лёд

        // Lemonade/fruit drinks: no milk toppings (syrups + ice only)
        Set<Topping> lemonadeToppings = new HashSet<>();
        lemonadeToppings.addAll(syrupToppings);
        lemonadeToppings.add(toppings.get(25)); // Холодный лёд

        List<Product> products = new ArrayList<>();

        // Кофе (COFFEE) - 15 items
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Эспрессо").category(coffee).basePrice(new BigDecimal("600")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Двойной эспрессо").category(coffee).basePrice(new BigDecimal("900")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Американо").category(coffee).basePrice(new BigDecimal("700")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Капучино").category(coffee).basePrice(new BigDecimal("900")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Латте").category(coffee).basePrice(new BigDecimal("1000")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Флэт уайт").category(coffee).basePrice(new BigDecimal("950")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Макиато").category(coffee).basePrice(new BigDecimal("850")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Мокко").category(coffee).basePrice(new BigDecimal("1050")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Раф кофе").category(coffee).basePrice(new BigDecimal("1100")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Доппио").category(coffee).basePrice(new BigDecimal("800")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Кортадо").category(coffee).basePrice(new BigDecimal("900")).available(true).availableToppings(coffeeBaseToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Колд брю").category(coffee).basePrice(new BigDecimal("1200")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Айс латте").category(coffee).basePrice(new BigDecimal("1100")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Айс американо").category(coffee).basePrice(new BigDecimal("800")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Дальгона кофе").category(coffee).basePrice(new BigDecimal("1300")).available(true).availableToppings(coffeeBaseToppings).build()));

        // Чай (TEA) - 8 items
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Чёрный чай").category(tea).basePrice(new BigDecimal("500")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Зелёный чай").category(tea).basePrice(new BigDecimal("500")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Матча латте").category(tea).basePrice(new BigDecimal("1000")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Чай масала").category(tea).basePrice(new BigDecimal("700")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Ромашковый чай").category(tea).basePrice(new BigDecimal("450")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Мятный чай").category(tea).basePrice(new BigDecimal("450")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Фруктовый чай").category(tea).basePrice(new BigDecimal("600")).available(true).availableToppings(teaToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Ройбос").category(tea).basePrice(new BigDecimal("550")).available(true).availableToppings(teaToppings).build()));

        // Холодные напитки (COLD_DRINKS) - 7 items
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Лимонад классический").category(coldDrinks).basePrice(new BigDecimal("800")).available(true).availableToppings(lemonadeToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Смузи клубника").category(coldDrinks).basePrice(new BigDecimal("1200")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Смузи манго").category(coldDrinks).basePrice(new BigDecimal("1200")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Молочный коктейль ваниль").category(coldDrinks).basePrice(new BigDecimal("1000")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Молочный коктейль шоколад").category(coldDrinks).basePrice(new BigDecimal("1000")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Холодный матча").category(coldDrinks).basePrice(new BigDecimal("1100")).available(true).availableToppings(coldToppings).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Апельсиновый фреш").category(coldDrinks).basePrice(new BigDecimal("900")).available(true).availableToppings(new HashSet<>()).build()));

        // Еда (FOOD) - 10 items
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Круассан классический").category(food).basePrice(new BigDecimal("500")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Круассан с миндалём").category(food).basePrice(new BigDecimal("700")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Сэндвич с лососем").category(food).basePrice(new BigDecimal("1500")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Сэндвич с курицей").category(food).basePrice(new BigDecimal("1300")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Авокадо тост").category(food).basePrice(new BigDecimal("1400")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Багель с сыром").category(food).basePrice(new BigDecimal("800")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Маффин черничный").category(food).basePrice(new BigDecimal("500")).available(false).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Маффин шоколадный").category(food).basePrice(new BigDecimal("550")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Гранола с йогуртом").category(food).basePrice(new BigDecimal("900")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Чиабатта с ветчиной").category(food).basePrice(new BigDecimal("1100")).available(true).availableToppings(new HashSet<>()).build()));

        // Десерты (DESSERTS) - 10 items
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Чизкейк Нью-Йорк").category(desserts).basePrice(new BigDecimal("800")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Тирамису").category(desserts).basePrice(new BigDecimal("900")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Брауни шоколадный").category(desserts).basePrice(new BigDecimal("600")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Эклер ванильный").category(desserts).basePrice(new BigDecimal("500")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Макарон ассорти").category(desserts).basePrice(new BigDecimal("1200")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Медовик").category(desserts).basePrice(new BigDecimal("700")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Карамельный пудинг").category(desserts).basePrice(new BigDecimal("650")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Панна котта").category(desserts).basePrice(new BigDecimal("750")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Штрудель яблочный").category(desserts).basePrice(new BigDecimal("600")).available(true).availableToppings(new HashSet<>()).build()));
        products.add(productRepository.save(Product.builder().coffeeShop(defaultShop).name("Торт Наполеон").category(desserts).basePrice(new BigDecimal("850")).available(true).availableToppings(new HashSet<>()).build()));

        return products;
    }

    private List<User> seedUsers() {
        RefUserRole userRole = refUserRoleRepository.findByCode("USER")
                .orElseThrow(() -> new NoSuchElementException("Role USER not found"));
        RefUserRole baristaRole = refUserRoleRepository.findByCode("BARISTA")
                .orElseThrow(() -> new NoSuchElementException("Role BARISTA not found"));
        RefUserRole managerRole = refUserRoleRepository.findByCode("MANAGER")
                .orElseThrow(() -> new NoSuchElementException("Role MANAGER not found"));
        RefUserRole adminRole = refUserRoleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new NoSuchElementException("Role ADMIN not found"));

        List<User> users = new ArrayList<>();
        users.add(userRepository.save(User.builder().email("user@demo.kz").role(userRole).build()));
        users.add(userRepository.save(User.builder().email("user2@demo.kz").role(userRole).build()));
        users.add(userRepository.save(User.builder().email("barista@demo.kz").role(baristaRole).build()));
        users.add(userRepository.save(User.builder().email("manager@demo.kz").role(managerRole).build()));
        users.add(userRepository.save(User.builder().email("admin@demo.kz").role(adminRole).build()));
        return users;
    }

    private void seedOrders(List<User> users, List<CoffeeShop> shops, List<Product> products, List<Topping> toppings) {
        RefOrderStatus newStatus = refOrderStatusRepository.findByCode("NEW")
                .orElseThrow(() -> new NoSuchElementException("Order status NEW not found"));
        RefOrderStatus inProgress = refOrderStatusRepository.findByCode("IN_PROGRESS")
                .orElseThrow(() -> new NoSuchElementException("Order status IN_PROGRESS not found"));
        RefOrderStatus ready = refOrderStatusRepository.findByCode("READY")
                .orElseThrow(() -> new NoSuchElementException("Order status READY not found"));
        RefOrderStatus completed = refOrderStatusRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new NoSuchElementException("Order status COMPLETED not found"));
        RefOrderStatus cancelled = refOrderStatusRepository.findByCode("CANCELLED")
                .orElseThrow(() -> new NoSuchElementException("Order status CANCELLED not found"));

        User user1 = users.get(0);
        User user2 = users.get(1);
        CoffeeShop shop1 = shops.get(0);
        CoffeeShop shop2 = shops.get(2);

        // Products for orders (use shop1 products at offset 0, shop2 products at offset 100)
        // Each shop has 50 products in the same order: coffee(0-14), tea(15-22), cold(23-29), food(30-39), desserts(40-49)
        int shop1Offset = 0;
        int shop2Offset = 100;
        Product espresso = products.get(shop1Offset);
        Product latte = products.get(shop1Offset + 4);
        Product americano = products.get(shop1Offset + 2);
        Product matcha = products.get(shop1Offset + 17);
        Product croissant = products.get(shop1Offset + 30);
        Product cheesecake = products.get(shop1Offset + 40);
        Product espressoShop2 = products.get(shop2Offset);
        Product latteShop2 = products.get(shop2Offset + 4);
        Product americanoShop2 = products.get(shop2Offset + 2);
        Product croissantShop2 = products.get(shop2Offset + 30);
        Product cheesecakeShop2 = products.get(shop2Offset + 40);

        // Topping refs
        Topping vanillaSyrup = toppings.get(7);
        Topping whippedCream = toppings.get(15);
        Topping cinnamon = toppings.get(17);

        // 20 orders with various statuses (use correct shop's products)
        createOrder(user1, shop1, newStatus, espresso, Set.of(), 1);
        createOrder(user1, shop1, newStatus, latte, Set.of(vanillaSyrup), 1);
        createOrder(user2, shop1, inProgress, americano, Set.of(), 2);
        createOrder(user2, shop2, inProgress, latteShop2, Set.of(vanillaSyrup, cinnamon), 1);
        createOrder(user1, shop1, ready, matcha, Set.of(), 1);
        createOrder(user1, shop2, ready, espressoShop2, Set.of(), 2);
        createOrder(user2, shop1, completed, americano, Set.of(), 1);
        createOrder(user2, shop1, completed, latte, Set.of(vanillaSyrup), 1);
        createOrder(user1, shop1, completed, croissant, Set.of(), 2);
        createOrder(user1, shop2, completed, cheesecakeShop2, Set.of(), 1);
        createOrder(user2, shop2, completed, espressoShop2, Set.of(), 1);
        createOrder(user1, shop1, cancelled, latte, Set.of(vanillaSyrup, whippedCream), 1);
        createOrder(user2, shop1, cancelled, americano, Set.of(), 1);
        createOrder(user1, shop1, completed, matcha, Set.of(), 1);
        createOrder(user2, shop2, completed, croissantShop2, Set.of(), 3);
        createOrder(user1, shop1, inProgress, cheesecake, Set.of(), 1);
        createOrder(user2, shop1, newStatus, espresso, Set.of(), 1);
        createOrder(user1, shop2, completed, latteShop2, Set.of(cinnamon), 1);
        createOrder(user2, shop2, ready, americanoShop2, Set.of(), 1);
        createOrder(user1, shop1, cancelled, matcha, Set.of(), 2);
    }

    private void createOrder(User user, CoffeeShop shop, RefOrderStatus status, Product product, Set<Topping> toppings, int quantity) {
        BigDecimal toppingPrice = toppings.stream().map(Topping::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemPrice = product.getBasePrice().add(toppingPrice).multiply(BigDecimal.valueOf(quantity));

        OrderItem item = OrderItem.builder()
                .product(product)
                .toppings(new HashSet<>(toppings))
                .quantity(quantity)
                .price(itemPrice)
                .build();

        Order order = Order.builder()
                .user(user)
                .shop(shop)
                .status(status)
                .total(itemPrice)
                .build();

        item.setOrder(order);
        order.getItems().add(item);
        orderRepository.save(order);
    }

    private void seedSavedCombinations(List<User> users, List<Product> products, List<Topping> toppings) {
        User user1 = users.get(0);
        User user2 = users.get(1);

        Product latte = products.get(4);
        Product cappuccino = products.get(3);
        Product americano = products.get(2);
        Product matchaLatte = products.get(17);
        Product coldbrew = products.get(11);

        Topping vanillaSyrup = toppings.get(7);
        Topping caramelSyrup = toppings.get(8);
        Topping oatMilk = toppings.get(3);
        Topping whippedCream = toppings.get(15);
        Topping cinnamon = toppings.get(17);

        // 10 saved combinations
        savedCombinationRepository.save(SavedCombination.builder().user(user1).product(latte).name("Любимый латте").toppings(Set.of(vanillaSyrup, oatMilk)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user1).product(cappuccino).name("Утренний капучино").toppings(Set.of(cinnamon)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user1).product(americano).name("Просто американо").toppings(new HashSet<>()).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user1).product(matchaLatte).name("Матча с овсяным").toppings(Set.of(oatMilk)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user1).product(coldbrew).name("Колд брю с карамелью").toppings(Set.of(caramelSyrup)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user2).product(latte).name("Латте с ванилью").toppings(Set.of(vanillaSyrup)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user2).product(cappuccino).name("Капучино со сливками").toppings(Set.of(whippedCream)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user2).product(americano).name("Двойной американо").toppings(new HashSet<>()).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user2).product(matchaLatte).name("Матча с корицей").toppings(Set.of(cinnamon)).build());
        savedCombinationRepository.save(SavedCombination.builder().user(user2).product(coldbrew).name("Колд брю классик").toppings(new HashSet<>()).build());
    }
}
