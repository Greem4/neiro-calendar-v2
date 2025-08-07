package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendarv2.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendarv2.domain.dto.CalendarResponseDto;
import ru.greemlab.neirocalendarv2.domain.dto.DayCellDto;
import ru.greemlab.neirocalendarv2.domain.dto.DaySummaryDto;
import ru.greemlab.neirocalendarv2.mapper.AttendanceRecordMapper;
import ru.greemlab.neirocalendarv2.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Сервис для работы с календарём посещений и формирования финансовых отчётов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final int COST_PER_ATTENDANCE = 1_400;
    private static final int TAX_AMOUNT = 6_500;

    private final AttendanceRecordRepository repository;
    private final AttendanceRecordMapper mapper;

    /**
     * Создание или обновление записи посещения.
     */
    @Transactional
    public void saveAttendance(AttendanceRecordDto dto) {
        repository.save(mapper.toEntity(dto));
    }

    /**
     * Удаление записи по идентификатору.
     */
    @Transactional
    public void deleteAttendance(Long id) {
        repository.deleteById(id);
    }

    /**
     * Обновление статуса посещения.
     */
    @Transactional
    public void updateAttendance(Long id, boolean attended) {
        var record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + id));
        record.setAttended(attended);
        repository.save(record);
    }

    /**
     * Создаёт пустые записи на весь месяц с интервалом 1 неделя.
     * Защита от дублей по (person, date) в пределах месяца.
     */
    @Transactional
    public void initMonthlySchedule(String person, LocalDate startDate) {
        var endOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth());
        var monthStart = startDate.withDayOfMonth(1);

        Set<LocalDate> existingDates = getRecordsBetween(monthStart, endOfMonth).stream()
                .filter(r -> person.equals(r.personName()))
                .map(AttendanceRecordDto::visitDate)
                .collect(Collectors.toSet());

        for (LocalDate d = startDate; !d.isAfter(endOfMonth); d = d.plusWeeks(1)) {
            if (!existingDates.contains(d)) {
                saveAttendance(new AttendanceRecordDto(null, person, d, false));
            }
        }
    }

    /**
     * Получить записи между датами включительно.
     */
    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        return repository.findBetween(start, end).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Ежедневные сводки: число визитов и заработок по дню.
     */
    public List<DaySummaryDto> getDailySummaries(LocalDate start, LocalDate end) {
        return getRecordsBetween(start, end).stream()
                .collect(Collectors.groupingBy(AttendanceRecordDto::visitDate))
                .entrySet().stream()
                .map(e -> {
                    int visits = e.getValue().size();
                    int attended = (int) e.getValue().stream()
                            .filter(r -> Boolean.TRUE.equals(r.attended()))
                            .count();
                    return new DaySummaryDto(e.getKey(), visits, attended, attended * COST_PER_ATTENDANCE);
                })
                .sorted(Comparator.comparing(DaySummaryDto::date))
                .collect(Collectors.toList());
    }

    /**
     * Подготовка данных для отображения календаря.
     * Один запрос в БД, остальное считаем в памяти.
     */
    public CalendarResponseDto prepareCalendarData(Integer year, Integer month) {
        var ctx = monthContext(year, month);

        var records = getRecordsBetween(ctx.startOfMonth(), ctx.endOfMonth());

        Map<LocalDate, List<AttendanceRecordDto>> byDate = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecordDto::visitDate));

        List<List<DayCellDto>> weeks = buildCalendarGrid(ctx.year(), ctx.month(), byDate);

        long attendedCount = records.stream()
                .filter(r -> Boolean.TRUE.equals(r.attended()))
                .count();

        int totalWithoutTax = Math.toIntExact(attendedCount * COST_PER_ATTENDANCE);
        int totalWithTax = Math.max(0, totalWithoutTax - TAX_AMOUNT); // не уходим в минус
        int potentialProfit = calculateFutureIncome(records);
        int totalCost = totalWithTax + potentialProfit;

        return CalendarResponseDto.builder()
                .year(ctx.year())
                .month(ctx.month())
                .weeks(weeks)
                .monthNames(monthNames())
                .attendedCount(attendedCount)
                .totalCostWithoutTax(totalWithoutTax)
                .totalCostWithTax(totalWithTax)
                .potentialProfit(potentialProfit)
                .totalCost(totalCost)
                .build();
    }

    private static MonthContext monthContext(Integer year, Integer month) {
        var now = LocalDate.now();
        var y = (year == null ? now.getYear() : year);
        var m = (month == null ? now.getMonthValue() : month);
        var start = LocalDate.of(y, m, 1);
        return new MonthContext(y, m, start, start.withDayOfMonth(start.lengthOfMonth()));
    }

    private List<List<DayCellDto>> buildCalendarGrid(int year, int month,
                                                     Map<LocalDate, List<AttendanceRecordDto>> map) {
        var firstOfMonth = LocalDate.of(year, month, 1);
        var shift = firstOfMonth.getDayOfWeek().getValue() - 1;
        var gridStart = firstOfMonth.minusDays(shift);

        return IntStream.range(0, 6)
                .mapToObj(week -> IntStream.range(0, 7)
                        .mapToObj(day -> {
                            var date = gridStart.plusDays(week * 7L + day);
                            var inMonth = date.getMonthValue() == month;
                            return new DayCellDto(date, inMonth, map.getOrDefault(date, List.of()));
                        })
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());
    }

    private LinkedHashMap<Integer, String> monthNames() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            var name = m.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru-RU"));
            map.put(m.getValue(), Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }
        return map;
    }

    private static int calculateFutureIncome(List<AttendanceRecordDto> records) {
        var today = LocalDate.now();
        long future = records.stream()
                .filter(r -> !Boolean.TRUE.equals(r.attended()) && !r.visitDate().isBefore(today))
                .count();
        return Math.toIntExact(future * COST_PER_ATTENDANCE);
    }

    private record MonthContext(int year, int month, LocalDate startOfMonth, LocalDate endOfMonth) {}
}
