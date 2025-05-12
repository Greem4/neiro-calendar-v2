package ru.greemlab.neirocalendarv2.mapper;

import ru.greemlab.neirocalendarv2.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;

/**
 * Entity ↔ DTO преобразования для AttendanceRecord
 */
public interface AttendanceRecordMapper {
    AttendanceRecordDto toDto(AttendanceRecord entity);
    AttendanceRecord toEntity(AttendanceRecordDto dto);
}
