package ru.greemlab.neirocalendarv2.domain.dto;

import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.List;

@Builder
public record CalendarResponseDto(
        int year,
        int month,
        List<List<DayCellDto>> weeks,
        LinkedHashMap<Integer, String> monthNames,
        long attendedCount,
        int totalCostWithoutTax,
        int totalCostWithTax
) {

}

