package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendarv2.domain.dto.*;
import ru.greemlab.neirocalendarv2.mapper.AttendanceRecordMapper;
import ru.greemlab.neirocalendarv2.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
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
    private static final int COST_PER_ATTENDANCE = 1_250;
    private static final int TAX_AMOUNT = 6_500;

    private final AttendanceRecordRepository repository;
    private final AttendanceRecordMapper mapper;

    /** Создание или обновление записи посещения. */
    @Transactional
    public void saveAttendance(AttendanceRecordDto dto) {
        repository.save(mapper.toEntity(dto));
    }

    /** Удаление записи по идентификатору. */
    @Transactional
    public void deleteAttendance(Long id) {
        repository.deleteById(id);
    }

    /** Обновление статуса посещения. */
    @Transactional
    public void updateAttendance(Long id, boolean attended) {
        repository.findById(id)
                .ifPresent(record -> {
                    record.setAttended(attended);
                    repository.save(record);
                });
    }

    /** Создаёт пустые записи на весь месяц с интервалом 1 неделя. */
    @Transactional
    public void initMonthlySchedule(String person, LocalDate startDate) {
        LocalDate endOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth());
        for (LocalDate d = startDate; !d.isAfter(endOfMonth); d = d.plusWeeks(1)) {
            saveAttendance(new AttendanceRecordDto(null, person, d, false));
        }
    }

    /** Получить записи между датами включительно. */
    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        return repository.findBetween(start, end).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /** Общая стоимость посещений за период (учитываются только явки). */
    public int calculateTotalCost(LocalDate start, LocalDate end) {
        long count = getRecordsBetween(start, end).stream()
                .filter(r -> Boolean.TRUE.equals(r.attended()))
                .count();
        return Math.toIntExact(count * COST_PER_ATTENDANCE);
    }

    /** Ежедневные сводки: число визитов и заработок по дню. */
    public List<DaySummaryDto> getDailySummaries(LocalDate start, LocalDate end) {
        return getRecordsBetween(start, end).stream()
                .collect(Collectors.groupingBy(AttendanceRecordDto::visitDate))
                .entrySet().stream()
                .map(e -> {
                    int visits = e.getValue().size();
                    int attended = (int) e.getValue().stream().filter(AttendanceRecordDto::attended).count();
                    return new DaySummaryDto(e.getKey(), visits, attended, attended * COST_PER_ATTENDANCE);
                })
                .sorted(Comparator.comparing(DaySummaryDto::date))
                .collect(Collectors.toList());
    }

    /** Формирует месячную сводку по посещениям и финансам. */
    public FinancialReportDto getMonthlyFinancialReport(int year, int month, LocalDate pivot) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        LocalDate today = Optional.ofNullable(pivot).orElse(LocalDate.now());

        List<AttendanceRecordDto> allRecords = getRecordsBetween(start, end);
        int total = allRecords.size();
        int completed = countBy(allRecords, r -> !r.visitDate().isAfter(today) && r.attended());
        int missed = countBy(allRecords, r -> !r.visitDate().isAfter(today) && !r.attended());
        int future = countBy(allRecords, r -> r.visitDate().isAfter(today));

        int totalRevenue = total * COST_PER_ATTENDANCE;
        int earnedRevenue = completed * COST_PER_ATTENDANCE;
        int missedRevenue = missed * COST_PER_ATTENDANCE;
        int futureRevenue = future * COST_PER_ATTENDANCE;

        int grossExpected = earnedRevenue + futureRevenue;
        int netExpected = grossExpected - TAX_AMOUNT;

        return new FinancialReportDto(
                year, month,
                total, totalRevenue,
                completed, earnedRevenue,
                missed, missedRevenue,
                future, futureRevenue,
                grossExpected, TAX_AMOUNT, netExpected,
                missedRevenue
        );
    }

    /** Подготовка данных для отображения календаря. */
    public CalendarResponseDto prepareCalendarData(Integer year, Integer month) {
        MonthContext ctx = monthContext(year, month);
        Map<LocalDate, List<AttendanceRecordDto>> byDate = getRecordsBetween(
                ctx.startOfMonth(), ctx.endOfMonth())
                .stream().collect(Collectors.groupingBy(AttendanceRecordDto::visitDate));

        List<List<DayCellDto>> weeks = buildCalendarGrid(ctx.year(), ctx.month(), byDate);
        long attendedCount = byDate.values().stream()
                .flatMap(Collection::stream)
                .filter(AttendanceRecordDto::attended)
                .count();

        return CalendarResponseDto.builder()
                .year(ctx.year())
                .month(ctx.month())
                .weeks(weeks)
                .monthNames(monthNames())
                .attendedCount(attendedCount)
                .totalCost(calculateTotalCost(
                        ctx.startOfMonth(), ctx.endOfMonth()))
                .build();
    }

    // Вспомогательные методы
    private int countBy(List<AttendanceRecordDto> list, Predicate<AttendanceRecordDto> filter) {
        return (int) list.stream().filter(filter).count();
    }

    private static MonthContext monthContext(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year == null ? now.getYear() : year);
        int m = (month == null ? now.getMonthValue() : month);
        LocalDate start = LocalDate.of(y, m, 1);
        return new MonthContext(y, m, start, start.withDayOfMonth(start.lengthOfMonth()));
    }

    private List<List<DayCellDto>> buildCalendarGrid(int year, int month,
                                                     Map<LocalDate, List<AttendanceRecordDto>> map) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        int shift = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate gridStart = firstOfMonth.minusDays(shift);

        return IntStream.range(0, 6)
                .mapToObj(week -> IntStream.range(0, 7)
                        .mapToObj(day -> {
                            LocalDate date = gridStart.plusDays(week * 7L + day);
                            boolean inMonth = date.getMonthValue() == month;
                            return new DayCellDto(date, inMonth, map.getOrDefault(date, List.of()));
                        })
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());
    }

    private LinkedHashMap<Integer, String> monthNames() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            String name = m.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru-RU"));
            map.put(m.getValue(), Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }
        return map;
    }
}
