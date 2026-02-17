package com.example.petmarket.service.user;

import com.example.petmarket.dto.UserRegistrationDto;
import com.example.petmarket.entity.User;

public interface UserRegistration {
    User register(UserRegistrationDto dto);
    void activateUser(String activationCode);
    boolean existsByEmail(String email);
}