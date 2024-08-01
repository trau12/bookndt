package com.ndt.identity_service.mapper;

import com.ndt.identity_service.entity.User;
import com.ndt.identity_service.entity.UserDocument;

public class Mapper {
    public static UserDocument convertToUserDocument(User user) {
        UserDocument userDocument = new UserDocument();
        userDocument.setId(user.getId());
        userDocument.setUsername(user.getUsername());
        userDocument.setPassword(user.getPassword());
        userDocument.setFirstName(user.getFirstName());
        userDocument.setLastName(user.getLastName());
        userDocument.setDob(user.getDob());
        return userDocument;
    }
}
