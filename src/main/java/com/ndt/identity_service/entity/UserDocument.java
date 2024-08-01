package com.ndt.identity_service.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(indexName = "users")
public class UserDocument {

    String id;
    String username;
    String password;
    String firstName;
    String lastName;
    LocalDate dob;

//    @ManyToMany(fetch = FetchType.LAZY) Set<Role> roles;
}
