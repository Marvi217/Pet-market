package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class Address {

    private String street;
    private String city;
    private String zipCode;
    private String country = "Poland";


}