package ru.greemlab.neirocalendarv2.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.greemlab.neirocalendarv2.domain.dto.LoginRequest;
import ru.greemlab.neirocalendarv2.domain.dto.RegisterRequest;
import ru.greemlab.neirocalendarv2.domain.dto.TokenResponse;
import ru.greemlab.neirocalendarv2.service.AuthService;


@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(
            @RequestBody @Valid LoginRequest req
    ) {
        return authService.login(req.username(), req.password());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(
            @RequestBody @Valid RegisterRequest req
    ) {
        authService.register(req.username(), req.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout() {
        authService.logout();
    }
}
