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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    // Valid order status transitions (code-based)
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "NEW",         Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("READY", "CANCELLED"),
            "READY",       Set.of("COMPLETED", "CANCELLED"),
            "COMPLETED",   Set.of(),
            "CANCELLED",   Set.of()
    );

    private final OrderRepository orderRepository;
    private final CoffeeShopRepository coffeeShopRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final UserRepository userRepository;
    private final PrintService printService;
    private final RefOrderStatusRepository refOrderStatusRepository;
    private final RefShopStatusRepository refShopStatusRepository;
    private final RefProductCategoryRepository refProductCategoryRepository;
    private final RefToppingTypeRepository refToppingTypeRepository;
    private final CityRepository cityRepository;

    public AdminService(OrderRepository orderRepository,
                        CoffeeShopRepository coffeeShopRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        UserRepository userRepository,
                        PrintService printService,
                        RefOrderStatusRepository refOrderStatusRepository,
                        RefShopStatusRepository refShopStatusRepository,
                        RefProductCategoryRepository refProductCategoryRepository,
                        RefToppingTypeRepository refToppingTypeRepository,
                        CityRepository cityRepository) {
        this.orderRepository = orderRepository;
        this.coffeeShopRepository = coffeeShopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.userRepository = userRepository;
        this.printService = printService;
        this.refOrderStatusRepository = refOrderStatusRepository;
        this.refShopStatusRepository = refShopStatusRepository;
        this.refProductCategoryRepository = refProductCategoryRepository;
        this.refToppingTypeRepository = refToppingTypeRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders(String statusCode, Long shopId) {
        List<Order> orders;
        if (statusCode != null && shopId != null) {
            RefOrderStatus status = resolveOrderStatus(statusCode);
            orders = orderRepository.findByStatusAndShopIdWithDetails(status, shopId);
        } else if (statusCode != null) {
            RefOrderStatus status = resolveOrderStatus(statusCode);
            orders = orderRepository.findByStatusWithDetails(status);
        } else if (shopId != null) {
            orders = orderRepository.findByShopIdWithDetails(shopId);
        } else {
            orders = orderRepository.findAllWithDetails();
        }
        return orders.stream().map(OrderDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        return OrderDto.from(order);
    }

    public OrderDto updateOrderStatus(Long orderId, String newStatusCode) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        String currentCode = order.getStatus().getCode();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentCode, Set.of());
        if (!allowed.contains(newStatusCode)) {
            throw new IllegalStateException(
                    "Cannot transition order from " + currentCode + " to " + newStatusCode);
        }
        RefOrderStatus newStatus = resolveOrderStatus(newStatusCode);
        order.setStatus(newStatus);
        return OrderDto.from(orderRepository.save(order));
    }

    public CoffeeShopDto createShop(CreateShopRequest request) {
        RefShopStatus status = resolveShopStatus(request.getStatusCode());
        City city = resolveCity(request.getCityId());
        CoffeeShop shop = CoffeeShop.builder()
                .name(request.getName())
                .city(city)
                .address(request.getAddress())
                .status(status)
                .build();
        return CoffeeShopDto.from(coffeeShopRepository.save(shop));
    }

    public CoffeeShopDto updateShop(Long shopId, CreateShopRequest request) {
        CoffeeShop shop = coffeeShopRepository.findById(shopId)
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + shopId));
        shop.setName(request.getName());
        shop.setCity(resolveCity(request.getCityId()));
        shop.setAddress(request.getAddress());
        shop.setStatus(resolveShopStatus(request.getStatusCode()));
        return CoffeeShopDto.from(coffeeShopRepository.save(shop));
    }

    public void deleteShop(Long shopId) {
        if (!coffeeShopRepository.existsById(shopId)) {
            throw new NoSuchElementException("Coffee shop not found: " + shopId);
        }
        coffeeShopRepository.deleteById(shopId);
    }

    public ProductDto createProduct(CreateProductRequest request) {
        Set<Topping> availableToppings = resolveToppings(request.getAvailableToppingIds());
        RefProductCategory category = resolveProductCategory(request.getCategoryCode());
        Product product = Product.builder()
                .name(request.getName())
                .category(category)
                .basePrice(request.getBasePrice())
                .available(request.isAvailable())
                .availableToppings(availableToppings)
                .build();
        return ProductDto.from(productRepository.save(product));
    }

    public ProductDto updateProduct(Long productId, CreateProductRequest request) {
        Product product = productRepository.findByIdWithToppings(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        product.setName(request.getName());
        product.setCategory(resolveProductCategory(request.getCategoryCode()));
        product.setBasePrice(request.getBasePrice());
        product.setAvailable(request.isAvailable());
        product.setAvailableToppings(resolveToppings(request.getAvailableToppingIds()));
        return ProductDto.from(productRepository.save(product));
    }

    private Set<Topping> resolveToppings(Set<Long> toppingIds) {
        if (toppingIds == null || toppingIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Topping> toppings = new HashSet<>(toppingRepository.findAllByIdWithIncompatibilities(toppingIds));
        if (toppings.size() != toppingIds.size()) {
            throw new IllegalArgumentException("One or more topping IDs not found");
        }
        return toppings;
    }

    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new NoSuchElementException("Product not found: " + productId);
        }
        productRepository.deleteById(productId);
    }

    public ToppingDto createTopping(CreateToppingRequest request) {
        Set<Topping> incompatible = new HashSet<>();
        if (request.getIncompatibleWithIds() != null && !request.getIncompatibleWithIds().isEmpty()) {
            incompatible = new HashSet<>(toppingRepository.findAllById(request.getIncompatibleWithIds()));
            if (incompatible.size() != request.getIncompatibleWithIds().size()) {
                throw new IllegalArgumentException("One or more incompatible topping IDs not found");
            }
        }
        RefToppingType type = resolveToppingType(request.getTypeCode());
        Topping topping = Topping.builder()
                .name(request.getName())
                .type(type)
                .price(request.getPrice())
                .incompatibleWith(incompatible)
                .build();
        return ToppingDto.from(toppingRepository.save(topping));
    }

    public ToppingDto updateTopping(Long toppingId, CreateToppingRequest request) {
        Topping topping = toppingRepository.findById(toppingId)
                .orElseThrow(() -> new NoSuchElementException("Topping not found: " + toppingId));
        topping.setName(request.getName());
        topping.setType(resolveToppingType(request.getTypeCode()));
        topping.setPrice(request.getPrice());
        Set<Topping> incompatible = new HashSet<>();
        if (request.getIncompatibleWithIds() != null && !request.getIncompatibleWithIds().isEmpty()) {
            incompatible = new HashSet<>(toppingRepository.findAllById(request.getIncompatibleWithIds()));
            if (incompatible.size() != request.getIncompatibleWithIds().size()) {
                throw new IllegalArgumentException("One or more incompatible topping IDs not found");
            }
        }
        topping.setIncompatibleWith(incompatible);
        return ToppingDto.from(toppingRepository.save(topping));
    }

    public void deleteTopping(Long toppingId) {
        if (!toppingRepository.existsById(toppingId)) {
            throw new NoSuchElementException("Topping not found: " + toppingId);
        }
        toppingRepository.deleteById(toppingId);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAllWithRole().stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
    }

    public void printOrder(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        printService.printReceipt(order);
    }

    private RefOrderStatus resolveOrderStatus(String code) {
        return refOrderStatusRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Unknown order status code: " + code));
    }

    private RefShopStatus resolveShopStatus(String code) {
        return refShopStatusRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Unknown shop status code: " + code));
    }

    private RefProductCategory resolveProductCategory(String code) {
        return refProductCategoryRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Unknown product category code: " + code));
    }

    private RefToppingType resolveToppingType(String code) {
        return refToppingTypeRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Unknown topping type code: " + code));
    }

    private City resolveCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NoSuchElementException("City not found: " + cityId));
    }
}
