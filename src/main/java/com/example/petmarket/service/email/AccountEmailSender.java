package com.example.petmarket.service.email;

public interface AccountEmailSender {
    void sendActivationEmail(String to, String activationCode);
    void sendPasswordResetEmail(String to, String resetToken);
}