/* (C)2024 */
package com.ndt.identity_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.exception.AppException;
import com.ndt.identity_service.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("/test.properties")
class UserServiceTest {
    @Autowired private UserService userService;

    @MockBean private UserRepository userRepository;

    private User user;
    private UserCreationRequest request;
    private UserResponse userResponse;
    private LocalDate dob;

    @BeforeEach
    void initData() {
        dob = LocalDate.of(1990, 1, 1);
        request =
                UserCreationRequest.builder()
                        .username("taind0")
                        .firstName("Nguyen Dinh")
                        .lastName("Tai")
                        .password("12345678")
                        .dob(dob)
                        .build();

        userResponse =
                UserResponse.builder()
                        .id("3849cd3604d4")
                        .username("taind0")
                        .firstName("Nguyen Dinh")
                        .lastName("Tai")
                        .dob(dob)
                        .build();

        user =
                User.builder()
                        .id("3849cd3604d4")
                        .username("taind0")
                        .firstName("Nguyen Dinh")
                        .lastName("Tai")
                        .dob(dob)
                        .build();
    }

    @Test
    void createUser_validRequest_success() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        // WHEN
        var response = userService.createUser(request);

        // THEN
        Assertions.assertThat(response.getId()).isEqualTo("3849cd3604d4");
        Assertions.assertThat(response.getUsername()).isEqualTo("taind0");
    }

    @Test
    void createUser_userExisted_fail() {
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        // THEN
        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    @Test
    @WithMockUser(username = "taind0")
    void getMyInfo_valid_success() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        var response = userService.getMyInfo();

        Assertions.assertThat(response.getUsername()).isEqualTo("taind0");
        Assertions.assertThat(response.getId()).isEqualTo("3849cd3604d4");
    }

    @Test
    @WithMockUser(username = "john")
    void getMyInfo_userNotFound_error() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.ofNullable(null));

        // WHEN
        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1005);
    }
}
