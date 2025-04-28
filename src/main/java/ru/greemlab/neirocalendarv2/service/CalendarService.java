package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendarv2.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendarv2.domain.dto.ChildSummaryDto;
import ru.greemlab.neirocalendarv2.domain.dto.DaySummaryDto;
import ru.greemlab.neirocalendarv2.domain.dto.MonthSummaryDto;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;
import ru.greemlab.neirocalendarv2.mapper.UserAttendanceRecordMap;
import ru.greemlab.neirocalendarv2.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    /** Цена одного посещения */
    private static final int COST_PER_ATTENDANCE = 1250;

    private final AttendanceRecordRepository repository;
    private final UserAttendanceRecordMap mapper;

    /**
     * Создать или обновить запись о посещении.
     * @param dto DTO записи о посещении
     */
    @Transactional
    public void saveAttendance(AttendanceRecordDto dto) {
        AttendanceRecord entity;
        if (dto.id() != null) {
            entity = repository.findById(dto.id()).orElse(new AttendanceRecord());
        } else {
            entity = new AttendanceRecord();
        }

        entity.setPersonName(dto.personName());
        entity.setVisitDate(dto.visitDate());
        entity.setAttended(Boolean.TRUE.equals(dto.attended()));

        var saved = repository.save(entity);
        mapper.toDto(saved);
    }

    /**
     * Создать пустые записи посещений на 1 месяц вперёд с интервалом в 1 неделю.
     * @param personName Имя человека
     * @param startDate Дата начала
     */
    @Transactional
    public void saveAttendanceFor3Month(String personName, LocalDate startDate) {
        var endDate = startDate.plusMonths(1);
        var current = startDate;

        while (!current.isAfter(endDate)) {
            var dto = new AttendanceRecordDto(null, personName, current, false);
            saveAttendance(dto);
            current = current.plusWeeks(1);
        }
    }

    /**
     * Отметить запись как присутствие.
     * @param id Идентификатор записи
     */
    @Transactional
    public void markAttendanceTrue(Long id) {
        setAttendance(id, true);
    }

    /**
     * Отметить запись как отсутствие.
     * @param id Идентификатор записи
     */
    @Transactional
    public void markAttendanceFalse(Long id) {
        setAttendance(id, false);
    }

    /**
     * Удалить запись о посещении по ID.
     * @param recordId Идентификатор записи
     */
    @Transactional
    public void deleteAttendance(Long recordId) {
        repository.deleteById(recordId);
    }

    /**
     * Получить список записей о посещении за указанный период.
     * @param start Начальная дата (включительно)
     * @param end Конечная дата (включительно)
     * @return Список записей
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        return repository.findByVisitDateBetween(start, end)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Рассчитать общую стоимость всех посещений за период.
     * Учитываются только записи с attended = true.
     * @param start Начальная дата
     * @param end Конечная дата
     * @return Общая стоимость
     */
    @Transactional(readOnly = true)
    public int calculateTotalCost(LocalDate start, LocalDate end) {
        var list = getRecordsBetween(start, end);
        var sum = 0;
        for (var record : list) {
            if (Boolean.TRUE.equals(record.attended())) {
                sum += COST_PER_ATTENDANCE;
            }
        }
        return sum;
    }

    /**
     * Получить ежедневные сводки посещений за указанный период.
     * @param start Начальная дата
     * @param end Конечная дата
     * @return Список дневных сводок
     */
    @Transactional(readOnly = true)
    public List<DaySummaryDto> getDailySummaries(LocalDate start, LocalDate end) {
        var records = getRecordsBetween(start, end);

        var dailyRecords = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecordDto::visitDate));

        List<DaySummaryDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AttendanceRecordDto>> entry : dailyRecords.entrySet()) {
            var all = entry.getValue().size();
            var attendedCount = (int) entry.getValue().stream()
                    .filter(r -> Boolean.TRUE.equals(r.attended()))
                    .count();
            var earnings = attendedCount * COST_PER_ATTENDANCE;
            result.add(new DaySummaryDto(entry.getKey(), all, attendedCount, earnings));
        }
        result.sort(Comparator.comparing(DaySummaryDto::date));
        return result;
    }

    /**
     * Получить сводку по месяцу.
     * @param year Год
     * @param month Месяц (1-12)
     * @param pivot Опорная дата для разделения прошлых и будущих посещений
     * @return Месячная сводка
     */
    @Transactional(readOnly = true)
    public MonthSummaryDto getMonthSummary(int year, int month, LocalDate pivot) {
        var p = (pivot == null) ? LocalDate.now() : pivot;
        var start = LocalDate.of(year, month, 1);
        var end = start.withDayOfMonth(start.lengthOfMonth());

        var list = getRecordsBetween(start, end);

        var total = list.size();
        var completed = count(list, r -> !r.visitDate().isAfter(p) && r.attended());
        var missed = count(list, r -> !r.visitDate().isAfter(p) && !r.attended());
        var future = count(list, r -> r.visitDate().isAfter(p));

        var costTotal = total * COST_PER_ATTENDANCE;
        var costEarned = completed * COST_PER_ATTENDANCE;
        var costMissed = missed * COST_PER_ATTENDANCE;
        var costPossible = costEarned + future * COST_PER_ATTENDANCE;

        Map<String, List<AttendanceRecordDto>> byChild = list.stream()
                .collect(Collectors.groupingBy(AttendanceRecordDto::personName));

        List<ChildSummaryDto> children = byChild.entrySet().stream()
                .map(e -> childSummary(e.getKey(), e.getValue(), p))
                .sorted(Comparator.comparing(ChildSummaryDto::name))
                .toList();

        return new MonthSummaryDto(year, month,
                total, completed, missed, future,
                costTotal, costEarned, costMissed, costPossible,
                children
        );
    }

    /**
     * Построить краткую сводку для ребёнка.
     * @param name Имя ребёнка
     * @param recs Список посещений
     * @param p Опорная дата
     * @return Сводка по ребёнку
     */
    private ChildSummaryDto childSummary(String name, List<AttendanceRecordDto> recs, LocalDate p) {
        List<LocalDate> att = recs.stream()
                .filter(r -> r.visitDate().isAfter(p) && r.attended())
                .map(AttendanceRecordDto::visitDate).sorted().toList();
        List<LocalDate> mis = recs.stream()
                .filter(r -> r.visitDate().isAfter(p) && !r.attended())
                .map(AttendanceRecordDto::visitDate).sorted().toList();

        return new ChildSummaryDto(
                name, att, mis,
                att.size() * COST_PER_ATTENDANCE,
                mis.size() * COST_PER_ATTENDANCE
        );
    }

    /**
     * Подсчитать количество записей, удовлетворяющих условию.
     * @param l Список записей
     * @param p Условие фильтрации
     * @return Количество подходящих записей
     */
    private int count(List<AttendanceRecordDto> l, Predicate<AttendanceRecordDto> p) {
        return (int) l.stream().filter(p).count();
    }

    /**
     * Установить статус посещения для записи.
     * @param id Идентификатор записи
     * @param value Статус посещения (true/false)
     */
    private void setAttendance(Long id, boolean value) {
        repository.findById(id).ifPresent(r -> {
            r.setAttended(value);
            repository.save(r);
        });
    }
}
