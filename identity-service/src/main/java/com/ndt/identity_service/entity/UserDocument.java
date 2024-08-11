package com.ndt.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(indexName = "user")
public class UserDocument {
    String id;
    String username;
    String password;
    String firstName;
    String lastName;
    String email;
    boolean emailVerified;
    @ManyToMany(fetch = FetchType.LAZY) Set<Role> roles;
    String city;
}
