package com.example.petmarket.service.user;

import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.interfaces.IUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements IUserQueryService {

    private final UserRepository userRepository;

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika o ID: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + email));
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> filterUsers(UserRole role, Boolean active, Pageable pageable) {
        return userRepository.filterUsers(role, active, pageable);
    }

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }
}