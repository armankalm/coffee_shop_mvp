package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemBoardDto;
import com.coffeeshop.app.service.board.OrderStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.*;
import com.coffeeshop.app.repository.CityRepository;
import com.coffeeshop.app.service.print.PrintService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final OrderItemRepository orderItemRepository;
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
    private final ApplicationEventPublisher eventPublisher;

    public AdminService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CoffeeShopRepository coffeeShopRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        UserRepository userRepository,
                        PrintService printService,
                        RefOrderStatusRepository refOrderStatusRepository,
                        RefShopStatusRepository refShopStatusRepository,
                        RefProductCategoryRepository refProductCategoryRepository,
                        RefToppingTypeRepository refToppingTypeRepository,
                        CityRepository cityRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
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
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders(String userEmail, String statusCode, Long shopId) {
        // Staff (BARISTA/MANAGER/ADMIN) can only see orders for shops they are assigned to.
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        Set<Long> assignedShopIds = user.getAssignedShops().stream()
                .map(CoffeeShop::getId)
                .collect(Collectors.toSet());
        if (assignedShopIds.isEmpty()) {
            return List.of();
        }

        // If a specific shop is requested it must be one the user is assigned to.
        Set<Long> scopeShopIds;
        if (shopId != null) {
            if (!assignedShopIds.contains(shopId)) {
                throw new com.coffeeshop.app.config.AccessDeniedException(
                        "You are not assigned to shop: " + shopId);
            }
            scopeShopIds = Set.of(shopId);
        } else {
            scopeShopIds = assignedShopIds;
        }

        List<Order> orders;
        if (statusCode != null) {
            RefOrderStatus status = resolveOrderStatus(statusCode);
            orders = orderRepository.findByStatusAndShopIdInWithDetails(status, scopeShopIds);
        } else {
            orders = orderRepository.findByShopIdInWithDetails(scopeShopIds);
        }
        return orders.stream().map(OrderDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        requireAssignedToShop(user, order.getShop().getId());
        return OrderDto.from(order);
    }

    public OrderDto updateOrderStatus(String userEmail, Long orderId, String newStatusCode) {
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // Staff can only update orders for shops they are assigned to.
        requireAssignedToShop(user, order.getShop().getId());

        String currentCode = order.getStatus().getCode();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentCode, Set.of());
        if (!allowed.contains(newStatusCode)) {
            throw new IllegalStateException(
                    "Cannot transition order from " + currentCode + " to " + newStatusCode);
        }
        RefOrderStatus newStatus = resolveOrderStatus(newStatusCode);
        order.setStatus(newStatus);
        OrderDto result = OrderDto.from(orderRepository.save(order));
        eventPublisher.publishEvent(new OrderStatusChangedEvent(this, order.getShop().getId(), order.getId()));
        return result;
    }

    /** Verifies the staff member may view the given shop's board (SSE subscription). */
    @Transactional(readOnly = true)
    public void assertAssignedToShop(String userEmail, Long shopId) {
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        requireAssignedToShop(user, shopId);
    }

    @Transactional(readOnly = true)
    public List<OrderItemBoardDto> getOrderItems(String userEmail, String statusCode, Long shopId) {
        // Staff (BARISTA/MANAGER/ADMIN) can only see items for shops they are assigned to.
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        Set<Long> assignedShopIds = user.getAssignedShops().stream()
                .map(CoffeeShop::getId)
                .collect(Collectors.toSet());
        if (assignedShopIds.isEmpty()) {
            return List.of();
        }

        Set<Long> scopeShopIds;
        if (shopId != null) {
            if (!assignedShopIds.contains(shopId)) {
                throw new com.coffeeshop.app.config.AccessDeniedException(
                        "You are not assigned to shop: " + shopId);
            }
            scopeShopIds = Set.of(shopId);
        } else {
            scopeShopIds = assignedShopIds;
        }

        List<OrderItem> items;
        if (statusCode != null) {
            RefOrderStatus status = resolveOrderStatus(statusCode);
            items = orderItemRepository.findByStatusAndShopIdInWithDetails(status, scopeShopIds);
        } else {
            items = orderItemRepository.findByShopIdInWithDetails(scopeShopIds);
        }
        return items.stream().map(OrderItemBoardDto::from).collect(Collectors.toList());
    }

    public OrderItemBoardDto updateOrderItemStatus(String userEmail, Long itemId, String newStatusCode) {
        User user = userRepository.findByEmailWithAssignedShops(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        OrderItem item = orderItemRepository.findByIdWithDetails(itemId)
                .orElseThrow(() -> new NoSuchElementException("Order item not found: " + itemId));

        // Staff can only update items for shops they are assigned to.
        requireAssignedToShop(user, item.getOrder().getShop().getId());

        String currentCode = item.getStatus().getCode();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentCode, Set.of());
        if (!allowed.contains(newStatusCode)) {
            throw new IllegalStateException(
                    "Cannot transition order item from " + currentCode + " to " + newStatusCode);
        }

        RefOrderStatus newStatus = resolveOrderStatus(newStatusCode);
        item.setStatus(newStatus);
        orderItemRepository.save(item);

        // The parent order only advances to a status once every one of its items
        // has reached (or passed) that status. This keeps the order-level status
        // meaningful while items are tracked independently on the board.
        syncOrderStatusFromItems(item.getOrder(), newStatusCode);

        return OrderItemBoardDto.from(item);
    }

    /**
     * Advances the parent order to {@code candidateCode} only if all of its items
     * are at that status. Never moves the order backwards.
     */
    private void syncOrderStatusFromItems(Order order, String candidateCode) {
        List<OrderItem> siblings = orderItemRepository.findByOrderIdWithStatus(order.getId());
        boolean allAtCandidate = siblings.stream()
                .allMatch(sibling -> candidateCode.equals(sibling.getStatus().getCode()));

        if (allAtCandidate && !candidateCode.equals(order.getStatus().getCode())) {
            order.setStatus(resolveOrderStatus(candidateCode));
            orderRepository.save(order);
            eventPublisher.publishEvent(new OrderStatusChangedEvent(this, order.getShop().getId(), order.getId()));
        }
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
        CoffeeShop shop = coffeeShopRepository.findByIdWithDetails(shopId)
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

    @Transactional(readOnly = true)
    public List<ProductDto> getProducts(Long shopId) {
        return productRepository.findAllNotDeletedWithToppingsByShop(shopId).stream()
                .map(ProductDto::from)
                .collect(Collectors.toList());
    }

    public ProductDto createProduct(CreateProductRequest request) {
        Set<Topping> availableToppings = resolveToppings(request.getAvailableToppingIds());
        RefProductCategory category = resolveProductCategory(request.getCategoryCode());
        CoffeeShop shop = coffeeShopRepository.findById(request.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + request.getShopId()));
        Product product = Product.builder()
                .name(request.getName())
                .coffeeShop(shop)
                .category(category)
                .basePrice(request.getBasePrice())
                .available(request.isAvailable())
                .description(request.getDescription())
                .availableToppings(availableToppings)
                .build();
        return ProductDto.from(productRepository.save(product));
    }

    public ProductDto updateProduct(Long productId, CreateProductRequest request) {
        Product product = productRepository.findByIdWithToppings(productId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        CoffeeShop shop = coffeeShopRepository.findById(request.getShopId())
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + request.getShopId()));
        product.setName(request.getName());
        product.setCoffeeShop(shop);
        product.setCategory(resolveProductCategory(request.getCategoryCode()));
        product.setBasePrice(request.getBasePrice());
        product.setAvailable(request.isAvailable());
        product.setDescription(request.getDescription());
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

    /**
     * Soft delete: orders, favorites and saved combinations still reference the product,
     * so it is only hidden. Marking it unavailable also blocks new orders for it, since
     * menus and order creation already check availability.
     */
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        product.setDeletedAt(Instant.now());
        product.setAvailable(false);
        productRepository.save(product);
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
        Topping saved = toppingRepository.save(topping);
        for (Topping other : incompatible) {
            other.getIncompatibleWith().add(saved);
            toppingRepository.save(other);
        }
        return ToppingDto.from(saved);
    }

    public ToppingDto updateTopping(Long toppingId, CreateToppingRequest request) {
        Topping topping = toppingRepository.findByIdWithIncompatibilities(toppingId)
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
        Set<Topping> oldIncompatible = new HashSet<>(topping.getIncompatibleWith());
        topping.setIncompatibleWith(incompatible);
        Topping saved = toppingRepository.save(topping);
        for (Topping old : oldIncompatible) {
            if (!incompatible.contains(old)) {
                old.getIncompatibleWith().remove(saved);
                toppingRepository.save(old);
            }
        }
        for (Topping other : incompatible) {
            if (!oldIncompatible.contains(other)) {
                other.getIncompatibleWith().add(saved);
                toppingRepository.save(other);
            }
        }
        return ToppingDto.from(saved);
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

    @Transactional(readOnly = true)
    public List<CoffeeShopDto> getAssignedShops(Long userId) {
        User user = userRepository.findByIdWithAssignedShops(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        return user.getAssignedShops().stream()
                .map(CoffeeShopDto::from)
                .collect(Collectors.toList());
    }

    public UserDto assignShop(Long userId, Long shopId) {
        User user = userRepository.findByIdWithAssignedShops(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        if ("USER".equals(user.getRole().getCode())) {
            throw new IllegalArgumentException("Cannot assign shops to a regular USER account");
        }
        CoffeeShop shop = coffeeShopRepository.findByIdWithDetails(shopId)
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + shopId));
        user.getAssignedShops().add(shop);
        return UserDto.fromWithAssignedShops(userRepository.save(user));
    }

    public UserDto unassignShop(Long userId, Long shopId) {
        User user = userRepository.findByIdWithAssignedShops(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        boolean removed = user.getAssignedShops().removeIf(s -> s.getId().equals(shopId));
        if (!removed) {
            throw new NoSuchElementException(
                    "User " + userId + " is not assigned to shop " + shopId);
        }
        return UserDto.fromWithAssignedShops(userRepository.save(user));
    }

    /** Throws AccessDeniedException unless the staff user is assigned to the given shop. */
    private void requireAssignedToShop(User user, Long shopId) {
        boolean assigned = user.getAssignedShops().stream()
                .anyMatch(s -> s.getId().equals(shopId));
        if (!assigned) {
            throw new com.coffeeshop.app.config.AccessDeniedException(
                    "You are not assigned to the shop for this order");
        }
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
