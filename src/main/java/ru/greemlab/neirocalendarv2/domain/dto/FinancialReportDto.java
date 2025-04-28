package ru.greemlab.neirocalendarv2.domain.dto;

public record FinancialReportDto(
        int year,
        int month,

        // общее по месяцу
        int totalLessons,
        int totalRevenue,         // totalLessons * COST_PER_ATTENDANCE

        // уже отработано
        int attendedCount,
        int earnedRevenue,        // attendedCount * COST_PER_ATTENDANCE

        // пропущено
        int missedCount,
        int missedRevenue,        // missedCount * COST_PER_ATTENDANCE

        // будущие
        int futureCount,
        int futurePotentialRevenue, // futureCount * COST_PER_ATTENDANCE

        // ожидаемая выручка и «чистыми»
        int expectedGrossRevenue, // earnedRevenue + futurePotentialRevenue
        int tax,                  // = 6500
        int expectedNetRevenue,   // expectedGrossRevenue - tax

        // сколько «упущено» денег на сегодня
        int lostRevenue           // = missedRevenue
) {
}
