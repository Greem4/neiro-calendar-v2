package ru.greemlab.neirocalendarv2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.greemlab.neirocalendarv2.domain.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с таблицей "attendance_records".
 * Наследуемся от JpaRepository, чтобы получить базовые CRUD-методы:
 * save, findAll, findById, delete и др.
 */
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("""
            SELECT r
            FROM AttendanceRecord r
            WHERE r.user.id = :userId
            AND r.visitDate BETWEEN :start AND :end
            ORDER BY r.visitDate, r.id
            """)
    List<AttendanceRecord> findBetweenForUser(@Param("userId") Long userId,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

    @Query("""
            SELECT r
            FROM AttendanceRecord r
            WHERE r.id = :id AND r.user.id = :userId
            """)
    Optional<AttendanceRecord> findByIdAndUserId(@Param("id") Long id,
                                                 @Param("userId") Long userId);
}
