package ru.greemlab.neirocalendarv2.domain.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
        @NotNull Boolean attended
) {
}
