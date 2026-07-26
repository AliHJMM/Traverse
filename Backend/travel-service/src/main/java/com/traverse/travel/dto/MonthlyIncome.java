package com.traverse.travel.dto;

import java.math.BigDecimal;

/** Income for a single calendar month, keyed as {@code YYYY-MM}. */
public record MonthlyIncome(
        String month,
        BigDecimal income
) {
}
