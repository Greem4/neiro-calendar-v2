package ru.greemlab.neirocalendarv2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.greemlab.neirocalendarv2.domain.dto.*;
import ru.greemlab.neirocalendarv2.service.CalendarService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@Tag(name = "Календарь", description = "Операции управления посещаемостью")
public class CalendarRestController {

    private final CalendarService calendarService;

    @GetMapping
    @Operation(summary = "Получить календарь на месяц")
    public CalendarResponseDto getCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        return calendarService.prepareCalendarData(year, month);
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Дневные сводки")
    public List<DaySummaryDto> getDailySummaries(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return calendarService.getDailySummaries(start, end);
    }

    @PostMapping("/add")
    @Operation(summary = "Создать занятия на месяц вперёд")
    public void addAttendance(@ModelAttribute AddRecordsRequest req) {
        log.info("Create lessons for '{}', starting {}", req.personName(), req.startDate());
        calendarService.initMonthlySchedule(req.personName(), req.startDate());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить посещение")
    public void updateAttendance(@PathVariable long id, @RequestBody UpdateAttendanceRequest body) {
        calendarService.updateAttendance(id, body.attended());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись")
    public void deleteAttendance(@PathVariable long id) {
        calendarService.deleteAttendance(id);
    }
}
