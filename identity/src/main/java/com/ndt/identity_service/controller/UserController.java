package com.ndt.identity_service.controller;

import com.ndt.identity_service.dto.request.ApiResponse;
import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.UserDocument;
import com.ndt.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    @Autowired
    CacheManager cacheManager;

    final UserService userService;


    public UserController(CacheManager cacheManager, UserService userService) {
        this.cacheManager = cacheManager;
        this.userService = userService;
    }

    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request){
        log.info("Controller: create User");
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getUsers(){
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PostMapping("/sync")
    public String syncUsers(){
        return userService.syncUser(userService.getAllUsersUnpageable());
    }

    /**
     * Normal search
     * @param keyword
     * @return
     */
    @GetMapping("/search/lastname/{keyword}")
    public ApiResponse<List<UserResponse>> finByAddress(@PathVariable String keyword) {
        return (ApiResponse<List<UserResponse>>) userService.findByLastName(keyword);
    }

    /**
     * Get all from Elasticsearch
     * @return
     */
    @GetMapping("/getAllUsers")
    public Iterable<UserDocument> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Fuzzy search
     * @param lastName
     * @return
     */
    @GetMapping("/fuzzysearch/lastname/{lastName}")
    public List<UserDocument> findByAddressFuzzy(@PathVariable String lastName) {
        return userService.findByLastNameFuzzy(lastName);
    }
    /**
     * Get Employee by firstName
     * @param firstName
     * @return
     */
    @GetMapping("/firstName/{firstName}/nocache")
    public ApiResponse<List<UserResponse>> getUserByFirstNameWithoutCache(@PathVariable String firstName) {
        List<UserResponse> users = userService.getUserByFirstName(firstName);
        return ApiResponse.<List<UserResponse>>builder()
                .result(users)
                .build();
    }
    @Cacheable(value = "user", key = "#firstName")
    @GetMapping("/firstName/{firstName}")
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

}
