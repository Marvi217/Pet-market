package com.example.petmarket.dto;

import lombok.Data;

@Data
public class UserRegistrationDto {
    private String email;
    private String password;
    private String FirstName;
    private String LastName;
}