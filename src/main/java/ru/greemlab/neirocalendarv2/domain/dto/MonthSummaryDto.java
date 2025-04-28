package ru.greemlab.neirocalendarv2.domain.dto;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public record MonthSummaryDto(
        int year,
        int month,
        int lessonsTotal,
        int lessonsCompleted,
        int lessonsMissed,
        int lessonsFuture,
        int costTotal,
        int costEarned,
        int costMissed,
        int costPossible,
        List<ChildSummaryDto> children
) {
    public String monthNameRu() {
        return Month.of(month)
                .getDisplayName(TextStyle.FULL_STANDALONE,
                        Locale.forLanguageTag("ru-RU"));
    }
}
