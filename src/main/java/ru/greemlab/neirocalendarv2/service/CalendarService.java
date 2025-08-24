package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendarv2.domain.dto.*;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;
import ru.greemlab.neirocalendarv2.mapper.AttendanceRecordMapper;
import ru.greemlab.neirocalendarv2.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final int COST_PER_ATTENDANCE = 1_400;
    private static final int TAX_AMOUNT = 6_500;

    private final AttendanceRecordRepository repository;
    private final AttendanceRecordMapper mapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public void saveAttendance(AttendanceRecordDto dto) {
        var user = currentUserService.getCurrentUser();
        var entity = new AttendanceRecord();
        entity.setUser(user);
        entity.setPersonName(Objects.requireNonNull(dto.personName(), "personName is required"));
        entity.setVisitDate(Objects.requireNonNull(dto.visitDate(), "visitDate is required"));
        entity.setAttended(Boolean.TRUE.equals(dto.attended()));
        repository.save(entity);
    }

    @Transactional
    public void deleteAttendance(Long id) {
        var user = currentUserService.getCurrentUser();
        var record = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found or not yours: " + id));
        repository.delete(record);
    }

    @Transactional
    public void updateAttendance(Long id, boolean attended) {
        var user = currentUserService.getCurrentUser();
        var record = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Attendance not found: " + id));
        record.setAttended(attended);
        repository.save(record);
    }

    @Transactional
    public void initMonthlySchedule(String person, LocalDate startDate) {
        var user = currentUserService.getCurrentUser();

        var endOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth());
        var monthStart = startDate.withDayOfMonth(1);

        // уже существующие даты для этой связки (user + personName)
        Set<LocalDate> existingDates = repository.findBetweenForUser(user.getId(), monthStart, endOfMonth).stream()
                .filter(r -> person.equals(r.getPersonName()))
                .map(AttendanceRecord::getVisitDate)
                .collect(Collectors.toSet());

        for (LocalDate d = startDate; !d.isAfter(endOfMonth); d = d.plusWeeks(1)) {
            if (!existingDates.contains(d)) {
                var record = new AttendanceRecord();
                record.setUser(user);
                record.setPersonName(person);
                record.setVisitDate(d);
                record.setAttended(false);
                repository.save(record);
            }
        }
    }

    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        var user = currentUserService.getCurrentUser();
        return repository.findBetweenForUser(user.getId(), start, end).stream()
                .map(mapper::toDto)
                .toList();
    }

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
                .toList();
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
                        .toList()
                )
                .toList();
    }

    private LinkedHashMap<Integer, String> monthNames() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        for (Month m : Month.values()) {
            var name = m.getDisplayName(TextStyle.FULL_STANDALONE, java.util.Locale.forLanguageTag("ru-RU"));
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

    private record MonthContext(int year, int month, LocalDate startOfMonth,
                                LocalDate endOfMonth) {
    }
}
