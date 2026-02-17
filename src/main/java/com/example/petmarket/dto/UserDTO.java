package com.example.petmarket.dto;

import com.example.petmarket.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Nieprawidłowy format email")
    @Size(max = 100, message = "Email może mieć maksymalnie 100 znaków")
    private String email;

    @Size(min = 8, message = "Hasło musi mieć minimum 8 znaków")
    private String password;

    @NotBlank(message = "Imię jest wymagane")
    @Size(min = 2, max = 50, message = "Imię musi mieć od 2 do 50 znaków")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    @Size(min = 2, max = 50, message = "Nazwisko musi mieć od 2 do 50 znaków")
    private String lastName;

    @Pattern(regexp = "^[0-9]{9,15}$", message = "Nieprawidłowy numer telefonu")
    private String phone;

    @NotNull(message = "Rola jest wymagana")
    private UserRole role;

    private boolean active = true;
}