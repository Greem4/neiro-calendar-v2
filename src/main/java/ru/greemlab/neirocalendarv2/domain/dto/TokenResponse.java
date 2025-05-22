package ru.greemlab.neirocalendarv2.domain.dto;

public record TokenResponse(
        String token,
        long expiresInMinutes
) {
}
