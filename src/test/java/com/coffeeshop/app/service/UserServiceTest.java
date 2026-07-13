package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.City;
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

        City city = City.builder().id(1L).name("Almaty").active(true).build();
        shop = CoffeeShop.builder().id(10L).name("Test Shop").city(city).address("Test Address").status(openStatus).build();
        user = User.builder().id(1L).email("user@example.com").role(userRole).build();
    }

    @Test
    void getCurrentUser_existingUser_returnsDto() {
        user.setCoffeeShop(shop);
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));

        UserDto result = userService.getCurrentUser("user@example.com");

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getCoffeeShopId()).isEqualTo(10L);
        assertThat(result.getCoffeeShopName()).isEqualTo("Test Shop");
    }

    @Test
    void getCurrentUser_noShop_returnsNullShopFields() {
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));

        UserDto result = userService.getCurrentUser("user@example.com");

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getCoffeeShopId()).isNull();
        assertThat(result.getCoffeeShopName()).isNull();
    }

    @Test
    void getCurrentUser_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithDetails("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("unknown@example.com"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void updateUserShop_validRequest_updatesShopAndReturnsDto() {
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(shop));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateUserShop("user@example.com", 10L);

        assertThat(result.getCoffeeShopId()).isEqualTo(10L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShop_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithDetails("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserShop("unknown@example.com", 10L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    void updateUserShop_shopNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserShop("user@example.com", 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUserShop_shopNotOpen_throwsIllegalArgument() {
        RefShopStatus closedStatus = RefShopStatus.builder().id(2L).code("CLOSED").nameRu("Закрыто").nameEn("Closed").build();
        CoffeeShop closedShop = CoffeeShop.builder().id(20L).name("Closed Shop").address("Addr").status(closedStatus).build();
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(coffeeShopRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(closedShop));

        assertThatThrownBy(() -> userService.updateUserShop("user@example.com", 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    void updateProfile_setsNameAndPhone() {
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateProfile("user@example.com", "Алия Садыкова", "+7 701 555 24 10");

        assertThat(result.getName()).isEqualTo("Алия Садыкова");
        assertThat(result.getPhone()).isEqualTo("+7 701 555 24 10");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_blankValues_clearsFields() {
        user.setName("Old Name");
        user.setPhone("+1 000");
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateProfile("user@example.com", "  ", "  ");

        assertThat(result.getName()).isNull();
        assertThat(result.getPhone()).isNull();
    }

    @Test
    void updateProfile_nullValues_leavesFieldsUnchanged() {
        user.setName("Existing Name");
        user.setPhone("+7 000");
        when(userRepository.findByEmailWithDetails("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateProfile("user@example.com", null, null);

        assertThat(result.getName()).isEqualTo("Existing Name");
        assertThat(result.getPhone()).isEqualTo("+7 000");
    }

    @Test
    void updateProfile_userNotFound_throwsNoSuchElement() {
        when(userRepository.findByEmailWithDetails("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile("unknown@example.com", "Name", "Phone"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknown@example.com");
    }
}
