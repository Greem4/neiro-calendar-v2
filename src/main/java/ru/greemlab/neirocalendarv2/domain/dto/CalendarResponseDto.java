package ru.greemlab.neirocalendarv2.domain.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CalendarResponseDto(
        int year,
        int month,
        List<List<DayCellDto>> weeks,
        Map<Integer, String> monthNames,
        long attendedCount,
        long totalCost
) {
}
