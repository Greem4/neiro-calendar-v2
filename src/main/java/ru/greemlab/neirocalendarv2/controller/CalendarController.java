package ru.greemlab.neirocalendarv2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.greemlab.neirocalendarv2.domain.dto.AddRecordsRequest;
import ru.greemlab.neirocalendarv2.domain.dto.CalendarResponseDto;
import ru.greemlab.neirocalendarv2.domain.dto.DaySummaryDto;
import ru.greemlab.neirocalendarv2.domain.dto.UpdateAttendanceRequest;
import ru.greemlab.neirocalendarv2.service.CalendarService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@Tag(name = "Календарь", description = "Операции управления посещаемостью (только свои записи)")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    @Operation(summary = "Получить календарь на месяц (текущий пользователь)")
    public CalendarResponseDto getCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        return calendarService.prepareCalendarData(year, month);
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Дневные сводки (текущий пользователь)")
    public List<DaySummaryDto> getDailySummaries(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return calendarService.getDailySummaries(start, end);
    }

    @PostMapping("/add")
    @Operation(summary = "Создать занятия на месяц вперёд (раз в неделю) для текущего пользователя")
    public void addAttendance(@ModelAttribute AddRecordsRequest req) {
        log.info("Create lessons for '{}', starting {}", req.personName(), req.startDate());
        calendarService.initMonthlySchedule(req.personName(), req.startDate());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить посещение (только свои записи)")
    public void updateAttendance(@PathVariable long id, @RequestBody UpdateAttendanceRequest body) {
        calendarService.updateAttendance(id, body.attended());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись (только свои записи)")
    public void deleteAttendance(@PathVariable long id) {
        calendarService.deleteAttendance(id);
    }
}
