package ru.greemlab.neirocalendarv2.mapper;

import org.springframework.stereotype.Component;
import ru.greemlab.neirocalendarv2.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;

/**
 * Преобразование Entity -> DTO.
 * Чтобы не повторять код, делаем отдельный метод.
 */
@Component
public class UserAttendanceRecordMap {

    public AttendanceRecordDto toDto(AttendanceRecord entity) {
        return new AttendanceRecordDto(
                entity.getId(),
                entity.getPersonName(),
                entity.getVisitDate(),
                entity.getAttended()

        );
    }
}
