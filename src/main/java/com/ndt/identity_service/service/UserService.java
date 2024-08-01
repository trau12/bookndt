package com.ndt.identity_service.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ndt.identity_service.constant.PredefinedRole;
import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.Role;
import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.entity.UserDocument;
import com.ndt.identity_service.exception.AppException;
import com.ndt.identity_service.exception.ErrorCode;
import com.ndt.identity_service.mapper.Mapper;
import com.ndt.identity_service.mapper.UserMapper;
import com.ndt.identity_service.repository.RoleRepository;
import com.ndt.identity_service.repository.UserRepository;
import com.ndt.identity_service.repository.UserRepositoryElasticsearch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private final UserRepositoryElasticsearch userRepositoryElasticsearch;
    UserRepository userRepository;
    UserMapper userMapper;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    public UserService(ElasticsearchClient elasticsearchClient, UserRepositoryElasticsearch userRepositoryElasticsearch, UserRepository userRepository, UserMapper userMapper, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.elasticsearchClient = elasticsearchClient;
        this.userRepositoryElasticsearch = userRepositoryElasticsearch;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String syncUser(List<User> users) {
        String message = "Error synchronize data";
        List<UserDocument> userDocuments = users.stream()
                .map(Mapper::convertToUserDocument)
                .collect(Collectors.toList());
        try {
            userRepositoryElasticsearch.saveAll(userDocuments);
            message = "Synchronized data Mysql and Elasticsearch";
            log.info(message);
        } catch (Exception e) {
            log.error(message);
        }
        return message;
    }

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);

        try {
            user = userRepository.save(user);
            userRepositoryElasticsearch.save(Mapper.convertToUserDocument(user));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
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
    @Cacheable(value = "user", key = "#firstName")
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

    public List<User> getAllUsersUnpageable() {
        List<User> users = userRepository.findAll();
        log.info("Get all users unpagenable");
        return users;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDocument> findByLastName(String keyword) {
        log.info("Normal search user by last name: " + keyword);
        return userRepositoryElasticsearch.findByLastName(keyword);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<UserDocument> getAllUsers() {
        log.info("get all user from elasticsearch");
        return userRepositoryElasticsearch.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDocument> findByLastNameFuzzy(String address) {
        log.info("Fuzzy search address: " + address);
        return userRepositoryElasticsearch.findByLastNameFuzzy(address);
    }
}
