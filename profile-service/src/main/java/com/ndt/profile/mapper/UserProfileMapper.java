package com.ndt.profile.mapper;

import org.mapstruct.Mapper;

import com.ndt.profile.dto.request.ProfileCreationRequest;
import com.ndt.profile.dto.response.UserProfileResponse;
import com.ndt.profile.entity.UserProfile;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfile toUserProfile(ProfileCreationRequest request);

    UserProfileResponse toUserProfileReponse(UserProfile entity);
}
