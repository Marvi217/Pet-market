package com.example.petmarket.service.user;

import com.example.petmarket.entity.User;
import com.example.petmarket.service.interfaces.IPasswordChangeService;
import com.example.petmarket.service.interfaces.IPasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordService implements PasswordOperations {

    private final IPasswordResetService passwordResetService;
    private final IPasswordChangeService passwordChangeService;

    @Override
    public boolean createPasswordResetToken(String email) {
        return passwordResetService.createPasswordResetToken(email);
    }

    @Override
    public User validatePasswordResetToken(String token) {
        return passwordResetService.validatePasswordResetToken(token);
    }

    @Override
    public boolean resetPasswordWithToken(String token, String newPassword) {
        return passwordResetService.resetPasswordWithToken(token, newPassword);
    }

    @Override
    public void sendPasswordResetEmailByUserId(Long userId) {
        passwordResetService.sendPasswordResetEmailByUserId(userId);
    }

    @Override
    public String validatePassword(String newPassword, String confirmPassword) {
        return passwordResetService.validatePassword(newPassword, confirmPassword);
    }

    @Override
    public void changePassword(Long id, String newPassword) {
        passwordChangeService.changePassword(id, newPassword);
    }

    @Override
    public void changePassword(String email, String currentPassword, String newPassword) {
        passwordChangeService.changePassword(email, currentPassword, newPassword);
    }
}