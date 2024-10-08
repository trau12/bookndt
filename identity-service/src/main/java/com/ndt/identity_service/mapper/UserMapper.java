/* (C)2024 */
package com.ndt.identity_service.mapper;

import com.ndt.identity_service.dto.request.UserCreationRequest;
import com.ndt.identity_service.dto.request.UserUpdateRequest;
import com.ndt.identity_service.dto.response.UserResponse;
import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.entity.UserDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    UserResponse toUserResponse(UserDocument userDocument);

    @Mapping(target = "roles", ignore = true)
    UserDocument toUserDocument(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

}
