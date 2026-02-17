package com.example.petmarket.service.email;

public interface EmailSender {
    void sendSimpleMessage(String to, String subject, String text);
}