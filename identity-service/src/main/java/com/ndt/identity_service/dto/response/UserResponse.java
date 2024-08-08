package com.ndt.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class UserResponse {

    String id;
    String username;
    String firstName;
    String lastName;
    String email;
    boolean emailVerified;
    Set<RoleResponse> roles;
//    String username;
}
