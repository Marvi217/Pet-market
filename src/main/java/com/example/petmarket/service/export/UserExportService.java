package com.example.petmarket.service.export;

import com.example.petmarket.entity.User;
import com.example.petmarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserExportService {

    private final UserRepository userRepository;

    public byte[] generateUsersCsv() {
        List<User> users = userRepository.findAll();

        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID;Imię;Nazwisko;Email;Rola;Status\n");

        for (User user : users) {
            csvContent.append(user.getId()).append(";")
                    .append(user.getFirstName()).append(";")
                    .append(user.getLastName()).append(";")
                    .append(user.getEmail()).append(";")
                    .append(user.getRole() != null ? user.getRole().name() : "").append(";")
                    .append(user.isActive() ? "Aktywny" : "Nieaktywny")
                    .append("\n");
        }

        byte[] csvBytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);

        ByteBuffer bb = ByteBuffer.allocate(bom.length + csvBytes.length);
        bb.put(bom);
        bb.put(csvBytes);

        return bb.array();
    }
}