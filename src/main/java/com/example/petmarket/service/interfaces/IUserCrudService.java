package com.example.petmarket.service.interfaces;

import com.example.petmarket.dto.UserDTO;
import com.example.petmarket.entity.User;

public interface IUserCrudService {

    User save(User user);

    User createUser(UserDTO userDTO);

    void deleteUser(Long id);
}