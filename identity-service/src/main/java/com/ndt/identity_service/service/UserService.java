package com.ndt.identity_service.service;

import com.ndt.event.dto.NotificationEvent;
import com.ndt.identity_service.constant.PredefinedRole;
import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.Role;
import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.entity.UserDocument;
import com.ndt.identity_service.exception.AppException;
import com.ndt.identity_service.exception.ErrorCode;
import com.ndt.identity_service.mapper.ProfileMapper;
import com.ndt.identity_service.mapper.UserMapper;
import com.ndt.identity_service.repository.RoleRepository;
import com.ndt.identity_service.repository.UserRepository;
import com.ndt.identity_service.repository.UserRepositoryElasticSearch;
import com.ndt.identity_service.repository.httpclient.ProfileClient;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    UserRepositoryElasticSearch userRepositoryElasticSearch;


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

        // Map User entity to UserDocument for Elasticsearch
        UserDocument userDocument = userMapper.toUserDocument(user);

        // Save the user document into Elasticsearch
        userRepositoryElasticSearch.save(userDocument);

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
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
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


    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsersEs(String lastName) {
        log.info("Get all users from Elasticsearch");
        List<UserDocument> users = userRepositoryElasticSearch.findByLastName(lastName);
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> findByLastNameFuzzy(String lastName) {
        log.info("Fuzzy search lastName: " + lastName);
        List<UserDocument> users = userRepositoryElasticSearch.findByLastNameFuzzy(lastName);
        return users.stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

}
