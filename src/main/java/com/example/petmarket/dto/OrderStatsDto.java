package com.example.petmarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderStatsDto {
    private long total;
    private BigDecimal totalSpent;
}