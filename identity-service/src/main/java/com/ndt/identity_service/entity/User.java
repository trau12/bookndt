package com.ndt.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "username", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    @NotNull
    String username;
    String password;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "email", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String email;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    boolean emailVerified;

    @ManyToMany(fetch = FetchType.LAZY) Set<Role> roles;

    String city;
}
