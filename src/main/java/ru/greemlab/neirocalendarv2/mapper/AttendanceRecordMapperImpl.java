package ru.greemlab.neirocalendarv2.mapper;

import org.springframework.stereotype.Component;
import ru.greemlab.neirocalendarv2.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;

@Component
public class AttendanceRecordMapperImpl implements AttendanceRecordMapper {

    @Override
    public AttendanceRecordDto toDto(AttendanceRecord entity) {
        if (entity == null) {
            return null;
        }
        return new AttendanceRecordDto(
                entity.getId(),
                entity.getPersonName(),
                entity.getVisitDate(),
                entity.getAttended()
        );
    }
}
