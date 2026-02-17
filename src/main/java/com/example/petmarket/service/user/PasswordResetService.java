package com.example.petmarket.service.user;

import com.example.petmarket.entity.User;
import com.example.petmarket.repository.UserRepository;
import com.example.petmarket.service.email.AccountEmailSender;
import com.example.petmarket.service.interfaces.IPasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService implements IPasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailSender accountEmailSender;

    @Override
    @Transactional
    public boolean createPasswordResetToken(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setPasswordResetToken(resetToken);
                    user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
                    userRepository.save(user);

                    try {
                        accountEmailSender.sendPasswordResetEmail(user.getEmail(), resetToken);
                        log.info("Mail z linkiem do resetowania hasła został wysłany do : {}", email);
                        return true;
                    } catch (Exception e) {
                        log.error("Nie udało się wysłać maila z resetem hasła do {}: {}", email, e.getMessage());
                        throw new RuntimeException("Nie udało się wysłać emaila z linkiem do resetowania hasła");
                    }
                })
                .orElse(false);
    }

    @Override
    public User validatePasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token)
                .filter(user -> user.getPasswordResetTokenExpiry() != null
                        && user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        User user = validatePasswordResetToken(token);
        if (user == null) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Hasło zresetowane dla : {}", user.getEmail());
        return true;
    }

    @Override
    @Transactional
    public void sendPasswordResetEmailByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika o ID: " + userId));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        accountEmailSender.sendPasswordResetEmail(user.getEmail(), resetToken);
        log.info("Admin resetuje hasło: {}", user.getEmail());
    }

    @Override
    public String validatePassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return "Hasło nie może być puste";
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return "Potwierdzenie hasła nie może być puste";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "Hasła nie są identyczne";
        }

        if (newPassword.length() < 6) {
            return "Hasło musi mieć co najmniej 6 znaków";
        }

        if (newPassword.length() > 100) {
            return "Hasło nie może być dłuższe niż 100 znaków";
        }

        if (!newPassword.matches(".*[A-Z].*")) {
            return "Hasło musi zawierać co najmniej jedną wielką literę";
        }

        if (!newPassword.matches(".*[a-z].*")) {
            return "Hasło musi zawierać co najmniej jedną małą literę";
        }

        if (!newPassword.matches(".*\\d.*")) {
            return "Hasło musi zawierać co najmniej jedną cyfrę";
        }

        return null;
    }
}