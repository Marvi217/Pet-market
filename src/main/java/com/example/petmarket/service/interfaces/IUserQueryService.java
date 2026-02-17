package com.example.petmarket.service.interfaces;

import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserQueryService {

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User getUserById(Long id);

    User getUserByEmail(String email);

    Page<User> getAllUsers(Pageable pageable);

    Page<User> filterUsers(UserRole role, Boolean active, Pageable pageable);

    Page<User> searchUsers(String query, Pageable pageable);
}