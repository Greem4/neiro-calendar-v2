package ru.greemlab.neirocalendarv2.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Сущность для таблицы attendance_records
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "attendance_records",
        indexes = {
                @Index(name = "idx_attendance_user_date",
                        columnList = "user_id, visit_date")
        }
)
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne( optional = false,  fetch = FetchType.LAZY )
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_attendance_user"))
    private UserAccount user;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "attended", nullable = false)
    private Boolean attended = false;
}
