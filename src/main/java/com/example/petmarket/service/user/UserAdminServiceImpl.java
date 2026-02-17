package com.example.petmarket.service.user;

import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.interfaces.IUserAdminService;
import com.example.petmarket.service.interfaces.IUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements IUserAdminService {

    private final UserRepository userRepository;
    private final IUserQueryService userQueryService;

    @Override
    @Transactional
    public void toggleActive(Long id) {
        User user = userQueryService.getUserById(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeRole(Long id, UserRole role) {
        User user = userQueryService.getUserById(id);
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public long getNewUsersCount(LocalDateTime from, LocalDateTime to) {
        return userRepository.countByCreatedAtBetween(from, to);
    }

    @Override
    public List<User> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }
}