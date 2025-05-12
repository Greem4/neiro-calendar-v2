package ru.greemlab.neirocalendarv2.domain.dto;

import java.time.LocalDate;
import java.util.List;

public record ChildSummaryDto(
        String name,
        List<LocalDate> datesAttended,
        List<LocalDate> datesMissed,
        int costEarned,
        int costMissed
) {
}
