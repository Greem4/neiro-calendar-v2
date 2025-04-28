package ru.greemlab.neirocalendarv2.domain.dto;

import java.time.LocalDate;

/**
 * DTO для передачи/отображения записей о посещении
 */
public record AttendanceRecordDto(
        Long id,
        String personName,
        LocalDate visitDate,
        Boolean attended
) {
}
