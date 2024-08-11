package com.ndt.identity_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndt.event.dto.NotificationEvent;
import com.ndt.identity_service.constant.PredefinedRole;
import com.ndt.identity_service.dto.request.PasswordChangeRequest;
import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.Role;
import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.exception.AppException;
import com.ndt.identity_service.exception.ErrorCode;
import com.ndt.identity_service.mapper.ProfileMapper;
import com.ndt.identity_service.mapper.UserMapper;
import com.ndt.identity_service.repository.RoleRepository;
import com.ndt.identity_service.repository.UserRepository;
import com.ndt.identity_service.repository.httpclient.ProfileClient;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    ProfileMapper profileMapper;
    ProfileClient profileClient;
    KafkaTemplate<String, Object> kafkaTemplate;
    RedisTemplate<String, String> redisTemplate;
    CacheManager cacheManager;
    ObjectMapper objectMapper;

    private static final String PASSWORD_CHANGE_QUEUE = "password-change-queue";
    private static final String PASSWORD_CHANGE_LOCK_PREFIX = "password-change-lock:";

    public UserResponse createUser(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<Role> roles = new HashSet<>();

        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);
        user.setEmailVerified(false);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        var profileRequest = profileMapper.toProfileCreationRequest(request);
        profileRequest.setUserId(user.getId());

        var profile = profileClient.createProfile(profileRequest);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(request.getEmail())
                .subject("Welcome to bookndt")
                .body("Hello, " + request.getUsername())
                .build();

        // Publish message to kafka
        kafkaTemplate.send("notification-delivery", notificationEvent);

        var userCreationResponse = userMapper.toUserResponse(user);
        userCreationResponse.setId(profile.getResult().getId());

        return userCreationResponse;
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.username == authentication.name")
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.username == authentication.name")
    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If password change is requested
        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            // Verify current password and queue the password change
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            queuePasswordChange(userId, request.getCurrentPassword(), request.getNewPassword());
        }

        // Update other fields using UserMapper
        userMapper.updateUser(user, request);

        // Update roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            var roles = roleRepository.findAllById(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        }

        User savedUser = userRepository.save(user);

        // Invalidate user cache
        cacheManager.getCache("users").evict(userId);

        return userMapper.toUserResponse(savedUser);
    }

    private void queuePasswordChange(String userId, String currentPassword, String newPassword) {
        String lockKey = PASSWORD_CHANGE_LOCK_PREFIX + userId;
        log.info("Attempting to set lock for user: {}", userId);

        // Attempt to set a lock for this user
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofMinutes(5));

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest(userId, currentPassword, newPassword);
                String jsonRequest = objectMapper.writeValueAsString(passwordChangeRequest);
                redisTemplate.opsForList().rightPush(PASSWORD_CHANGE_QUEUE, jsonRequest);
                log.info("Password change request queued for user: {}", userId);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error queuing password change request", e);
            }
        } else {
            log.warn("A password change is already in progress for user: {}", userId);
            throw new RuntimeException("A password change is already in progress for this user.");
        }
    }

    @Scheduled(fixedRate = 1000) // Run every second
    public void processPasswordChangeQueue() {
        String jsonRequest = redisTemplate.opsForList().leftPop(PASSWORD_CHANGE_QUEUE);
        if (jsonRequest != null) {
            PasswordChangeRequest request = null;
            try {
                request = objectMapper.readValue(jsonRequest, PasswordChangeRequest.class);
                log.info("Processing password change request for user: {}", request.getUserId());
                changePassword(request);
            } catch (JsonProcessingException e) {
                log.error("Error processing password change request", e);
            } finally {
                // Release the lock after processing
                if (request != null) {
                    redisTemplate.delete(PASSWORD_CHANGE_LOCK_PREFIX + request.getUserId());
                    log.info("Released lock for user: {}", request.getUserId());
                }
            }
        }
    }

    private void changePassword(PasswordChangeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.error("Current password is incorrect for user: {}", request.getUserId());
            throw new RuntimeException("Current password is incorrect");
        }

        // Encode and set new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate user cache
        cacheManager.getCache("users").evict(request.getUserId());

        log.info("Password changed successfully for user: {}", request.getUserId());
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.username == authentication.name")
    public void deleteUser(String userId){
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();

    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.username == authentication.name")
    public UserResponse getUser(String id){
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUserByFirstName(String firstName) { // user/ uR
        try {
            log.info("Get user with firstName: " + firstName);
            List<User> users = userRepository.findAllByFirstName(firstName);
            return users.stream()
                    .map(userMapper::toUserResponse)
                    .toList();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of(); // Trả về danh sách rỗng nếu có lỗi
        }
    }

}
