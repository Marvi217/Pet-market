package com.example.petmarket;

public class StarHelper {

    public String getFilledStarsWidth(Double rating) {
        if (rating == null || rating < 0) return "0%";
        if (rating > 5) rating = 5.0;
        return (rating * 20) + "%";
    }
}