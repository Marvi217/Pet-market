package com.example.petmarket.repository;

import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserOrderByIsDefaultDescIdDesc(User user);

    Optional<UserAddress> findByUserAndIsDefaultTrue(User user);

    Optional<UserAddress> findByIdAndUser(Long id, User user);

    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.user = :user")
    void clearDefaultForUser(@Param("user") User user);

}