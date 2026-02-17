package com.example.petmarket.service.user;

import com.example.petmarket.dto.UserRegistrationDto;
import com.example.petmarket.entity.User;
import com.example.petmarket.enums.UserRole;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.email.AccountEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService implements UserRegistration {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailSender accountEmailSender;

    @Override
    @Transactional
    public User register(UserRegistrationDto dto) {
        validateRegistration(dto);

        User user = mapDtoToEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(UserRole.USER);
        user.setEnabled(false);
        user.setActive(true);

        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);

        User savedUser = userRepository.save(user);

        try {
            accountEmailSender.sendActivationEmail(user.getEmail(), activationCode);
            log.info("Użytkonik zarejestrowany: {} kod aktywacyjny wysłany na maila", user.getEmail());
        } catch (Exception e) {
            log.warn("Nie udało się wysłać emaila aktywacyjnego do {}: {}. Użytkownik może aktywować konto ręcznie.",
                    user.getEmail(), e.getMessage());
        }

        return savedUser;
    }

    @Override
    @Transactional
    public void activateUser(String activationCode) {
        User user = userRepository.findByActivationCode(activationCode)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy kod aktywacyjny"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Konto jest już aktywne");
        }

        user.setEnabled(true);
        user.setActivationCode(null);
        userRepository.save(user);

        log.info("Użytkonik aktywny: {}", user.getEmail());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private void validateRegistration(UserRegistrationDto dto) {
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email jest wymagany");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Użytkownik o tym adresie email już istnieje");
        }

        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new IllegalArgumentException("Hasło musi mieć co najmniej 6 znaków");
        }

        if (dto.getFirstName() == null || dto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Imię jest wymagane");
        }

        if (dto.getLastName() == null || dto.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nazwisko jest wymagane");
        }
    }

    private User mapDtoToEntity(UserRegistrationDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        return user;
    }
}