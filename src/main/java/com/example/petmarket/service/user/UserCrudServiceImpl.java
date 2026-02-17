package com.example.petmarket.service.user;

import com.example.petmarket.dto.UserDTO;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.interfaces.IUserCrudService;
import com.example.petmarket.service.interfaces.IUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCrudServiceImpl implements IUserCrudService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IUserQueryService userQueryService;

    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            if (user.getRole() == null) {
                user.setRole(UserRole.USER);
            }
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User createUser(UserDTO userDTO) {
        if (userQueryService.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Użytkownik o podanym emailu już istnieje");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setRole(userDTO.getRole() != null ? userDTO.getRole() : UserRole.USER);
        user.setActive(true);
        user.setEnabled(true);

        if (userDTO.getPassword() != null && !userDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}