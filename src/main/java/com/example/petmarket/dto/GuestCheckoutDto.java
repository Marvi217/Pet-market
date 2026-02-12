package com.example.petmarket.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GuestCheckoutDto {
    private String email;
    private String name;
    private String phone;
    private String street;
    private String city;
    private String zipCode;
    private String country = "Poland";

}