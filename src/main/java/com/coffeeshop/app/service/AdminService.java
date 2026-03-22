package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    private final OrderRepository orderRepository;
    private final CoffeeShopRepository coffeeShopRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final UserRepository userRepository;

    public AdminService(OrderRepository orderRepository,
                        CoffeeShopRepository coffeeShopRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.coffeeShopRepository = coffeeShopRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders(OrderStatus status, Long shopId) {
        List<Order> orders;
        if (status != null && shopId != null) {
            orders = orderRepository.findByStatus(status).stream()
                    .filter(o -> o.getShop().getId().equals(shopId))
                    .collect(Collectors.toList());
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else if (shopId != null) {
            orders = orderRepository.findByShopId(shopId);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(OrderDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        return OrderDto.from(order);
    }

    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        order.setStatus(newStatus);
        return OrderDto.from(orderRepository.save(order));
    }

    public CoffeeShopDto createShop(CreateShopRequest request) {
        CoffeeShop shop = CoffeeShop.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .status(request.getStatus())
                .build();
        return CoffeeShopDto.from(coffeeShopRepository.save(shop));
    }

    public CoffeeShopDto updateShop(Long shopId, CreateShopRequest request) {
        CoffeeShop shop = coffeeShopRepository.findById(shopId)
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + shopId));
        shop.setName(request.getName());
        shop.setCity(request.getCity());
        shop.setAddress(request.getAddress());
        shop.setStatus(request.getStatus());
        return CoffeeShopDto.from(coffeeShopRepository.save(shop));
    }

    public void deleteShop(Long shopId) {
        if (!coffeeShopRepository.existsById(shopId)) {
            throw new NoSuchElementException("Coffee shop not found: " + shopId);
        }
        coffeeShopRepository.deleteById(shopId);
    }

    public ProductDto createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .basePrice(request.getBasePrice())
                .available(request.isAvailable())
                .build();
        return ProductDto.from(productRepository.save(product));
    }

    public ProductDto updateProduct(Long productId, CreateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBasePrice(request.getBasePrice());
        product.setAvailable(request.isAvailable());
        return ProductDto.from(productRepository.save(product));
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
        }
        Topping topping = Topping.builder()
                .name(request.getName())
                .type(request.getType())
                .price(request.getPrice())
                .incompatibleWith(incompatible)
                .build();
        return ToppingDto.from(toppingRepository.save(topping));
    }

    public ToppingDto updateTopping(Long toppingId, CreateToppingRequest request) {
        Topping topping = toppingRepository.findById(toppingId)
                .orElseThrow(() -> new NoSuchElementException("Topping not found: " + toppingId));
        topping.setName(request.getName());
        topping.setType(request.getType());
        topping.setPrice(request.getPrice());
        Set<Topping> incompatible = new HashSet<>();
        if (request.getIncompatibleWithIds() != null && !request.getIncompatibleWithIds().isEmpty()) {
            incompatible = new HashSet<>(toppingRepository.findAllById(request.getIncompatibleWithIds()));
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
        return userRepository.findAll().stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
    }
}
