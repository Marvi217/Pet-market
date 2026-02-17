package com.example.petmarket.dto;

import lombok.Data;

@Data
public class CheckoutForm {
    private String firstName;
    private String lastName;
    private String street;
    private String zipCode;
    private String city;
    private String phone;
    private String email;
    private String notes;
}