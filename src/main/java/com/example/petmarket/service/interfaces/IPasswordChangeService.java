package com.example.petmarket.service.interfaces;

public interface IPasswordChangeService {

    void changePassword(Long id, String newPassword);

    void changePassword(String email, String currentPassword, String newPassword);
}