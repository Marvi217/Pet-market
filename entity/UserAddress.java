package com.example.petmarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_addresses")
@Data
@NoArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String name; // Imię i nazwisko

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 10)
    private String zipCode;

    @Column(nullable = false)
    private String city;

    private String country = "Polska";

    private String phoneNumber;

    @Column(nullable = false)
    private boolean isDefault = false;
}