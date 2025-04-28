package ru.greemlab.neirocalendarv2.domain.dto;

import java.time.LocalDate;

public record MonthContext(
        int year,
        int month,
        LocalDate startOfMonth,
        LocalDate endOfMonth
) {
}
