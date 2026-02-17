package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.User;

public interface IPasswordResetService {

    boolean createPasswordResetToken(String email);

    User validatePasswordResetToken(String token);

    boolean resetPasswordWithToken(String token, String newPassword);

    void sendPasswordResetEmailByUserId(Long userId);

    String validatePassword(String newPassword, String confirmPassword);
}