package com.example.petmarket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleProductNotFound(ProductNotFoundException ex, Model model) {
        model.addAttribute("error", "Product Not Found");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 404);
        return "error/error";
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientStock(InsufficientStockException ex, Model model) {
        model.addAttribute("error", "Insufficient Stock");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 400);
        return "error/error";
    }

    @ExceptionHandler(UnauthorizedOrderException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorizedOrder(UnauthorizedOrderException ex, Model model) {
        model.addAttribute("error", "Access Denied");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("status", 403);
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", "An unexpected error occurred");
        model.addAttribute("status", 500);
        return "error/error";
    }
}