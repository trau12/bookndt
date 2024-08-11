package com.ndt.identity_service.controller;

import com.ndt.identity_service.dto.ApiResponse;
import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    @Autowired
    CacheManager cacheManager;

    UserService userService;

    @PostMapping("/create-user")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request")})
    @Operation(method = "POST", summary = "Add new user", description = "Send a request via this API to create new user")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        log.info("Controller: create user");
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get list of users", description = "Send a request via this API to get a list of all users")
    ApiResponse<List<UserResponse>> getUsers(){
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user detail", description = "Send a request via this API to get user information")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    @Operation(summary = "Get user information", description = "Send a request via this API to get user information")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Send a request via this API to update user")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user permanently", description = "Send a request via this API to delete user permanently")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }


    /**
     * Get Employee by firstName
     * @param firstName
     * @return
     */
    @GetMapping("/firstName/{firstName}/nocache")
    @Operation(summary = "Search users with cache", description = "Search users by first name using cache")
    public ApiResponse<List<UserResponse>> getUserByFirstNameWithoutCache(@PathVariable String firstName) {
        List<UserResponse> users = userService.getUserByFirstName(firstName);
        return ApiResponse.<List<UserResponse>>builder()
                .result(users)
                .build();
    }
    @Cacheable(value = "user", key = "#firstName")
    @GetMapping("/firstName/{firstName}")
    @Operation(summary = "Search users with no cache", description = "Search users by first name without using cache")
    public ApiResponse<List<UserResponse>> getUserByFirstName(@PathVariable String firstName) { //check user/ userResponse
        if (!cacheHit(firstName)) {
            log.warn("Cache miss for Employee with firstName: " + firstName);
        }
        List<UserResponse> users = userService.getUserByFirstName(firstName);
        return ApiResponse.<List<UserResponse>>builder()
                .result(users)
                .build();
    }
    /**
     * Check cache
     * @param name
     * @return
     */
    private boolean cacheHit(String name) {
        Cache cache = cacheManager.getCache("user");
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(name);
            return valueWrapper != null;
        }
        return false;
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by last name", description = "Search users by last name using Elasticsearch")
    public ApiResponse<List<UserResponse>> searchUsersByLastName(@RequestParam String lastName) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsersEs(lastName))
                .build();
    }

    @GetMapping("/search/fuzzy/{lastName}")
    @Operation(summary = "Fuzzy search users by last name", description = "Fuzzy search users by last name using Elasticsearch")
    public ApiResponse<List<UserResponse>> fuzzySearchUsersByLastName(@PathVariable String lastName) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.findByLastNameFuzzy(lastName))
                .build();
    }

}
