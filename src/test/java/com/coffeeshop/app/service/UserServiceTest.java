package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.RefShopStatus;
import com.coffeeshop.app.domain.RefUserRole;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.admin.UserDto;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import com.coffeeshop.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CoffeeShopRepository coffeeShopRepository;

    private UserService userService;

    private User user;
    private CoffeeShop shop;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, coffeeShopRepository);

        RefUserRole userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        RefShopStatus openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();

        shop = CoffeeShop.builder().id(10L).name("Test Shop").address("Test Address").status(openStatus).build();
        user = User.builder().id(1L).email("user@example.com").role(userRole).build();
    }

    @Test
    void updateUserShop_validRequest_updatesShopAndReturnsDto() {
        when(userRepository.findByEmailWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(shop));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateUserShop("user@example.com", 10L);

        assertThat(result.getCoffeeShopId()).isEqualTo(10L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShop_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithRole("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserShop("unknown@example.com", 10L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void updateUserShop_shopNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserShop("user@example.com", 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
