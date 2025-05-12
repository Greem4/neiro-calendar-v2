package ru.greemlab.neirocalendarv2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Репозиторий для работы с таблицей "attendance_records".
 * Наследуемся от JpaRepository, чтобы получить базовые CRUD-методы:
 * save, findAll, findById, delete и др.
 */
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    /**
     * Возвращает записи в диапазоне дат (включительно), отсортированные по дате и ID.
     */
    @Query("""
            SELECT r
              FROM AttendanceRecord r
             WHERE r.visitDate BETWEEN :start AND :end
             ORDER BY r.visitDate, r.id
            """)
    List<AttendanceRecord> findBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

}
