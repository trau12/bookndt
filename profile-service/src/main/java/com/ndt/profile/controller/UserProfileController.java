package com.ndt.profile.controller;

import com.ndt.profile.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

import com.ndt.profile.dto.request.ProfileCreationRequest;
import com.ndt.profile.dto.response.UserProfileReponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {
    UserProfileService userProfileService;

    @PostMapping("/users")
    UserProfileReponse createProfile(@RequestBody ProfileCreationRequest request) {
        return userProfileService.createProfile(request);
    }

    @GetMapping("/users/{profileId}")
    UserProfileReponse getProfile(@PathVariable String profileId) {
        return userProfileService.getProfile(profileId);
    }
}
