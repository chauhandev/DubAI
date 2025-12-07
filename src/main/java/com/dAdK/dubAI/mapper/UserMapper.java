package com.dAdK.dubAI.mapper;

import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.util.DateUtil;

public class UserMapper {
    public static UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? DateUtil.format(user.getDateOfBirth()) : null)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
