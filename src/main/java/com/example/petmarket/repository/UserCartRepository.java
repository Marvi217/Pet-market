package com.example.petmarket.repository;

import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCartRepository extends JpaRepository<UserCart, Long> {
    Optional<UserCart> findByUser(User user);
}