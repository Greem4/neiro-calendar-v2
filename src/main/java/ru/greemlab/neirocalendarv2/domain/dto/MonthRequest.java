package ru.greemlab.neirocalendarv2.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

public record MonthRequest(
        @RequestParam("year")
        int year,

        @RequestParam("month")
        @Min(1) @Max(12)
        int month,

        @RequestParam(value = "pivot", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate pivot
) {
}
