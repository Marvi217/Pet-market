package com.example.petmarket.service;

import com.example.petmarket.entity.User;
import com.example.petmarket.entity.UserAddress;
import com.example.petmarket.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository addressRepository;

    public List<UserAddress> getUserAddresses(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescIdDesc(user);
    }

    public Optional<UserAddress> getAddressById(Long id, User user) {
        return addressRepository.findByIdAndUser(id, user);
    }


    @Transactional
    public void saveAddress(UserAddress address, User user) {
        address.setUser(user);

        boolean isNewAddress = (address.getId() == null);

        if (address.isDefault()) {
            addressRepository.clearDefaultForUser(user);
        }
        else if (isNewAddress) {
            Optional<UserAddress> existingDefault = addressRepository.findByUserAndIsDefaultTrue(user);
            if (existingDefault.isEmpty()) {
                address.setDefault(true);
            }
        }

        addressRepository.save(address);
    }

    @Transactional
    public void setAsDefault(Long addressId, User user) {
        UserAddress address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new IllegalArgumentException("Adres nie istnieje"));

        addressRepository.clearDefaultForUser(user);

        address.setDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long addressId, User user) {
        UserAddress address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new IllegalArgumentException("Adres nie istnieje"));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<UserAddress> remaining = addressRepository.findByUserOrderByIsDefaultDescIdDesc(user);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }
}