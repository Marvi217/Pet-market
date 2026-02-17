package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public interface IUserAdminService {

    void toggleActive(Long id);

    void changeRole(Long id, UserRole role);

    long getNewUsersCount(LocalDateTime from, LocalDateTime to);

    List<User> getRecentUsers();
}