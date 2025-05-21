package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.greemlab.neirocalendarv2.domain.dto.TokenResponse;
import ru.greemlab.neirocalendarv2.repository.UserAccountRepository;
import ru.greemlab.neirocalendarv2.domain.entity.UserAccount;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public TokenResponse login(String username, String rawPassword) {
        try {
            var auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );
            var token = jwtService.generateToken(auth.getName());
            return new TokenResponse(token, jwtService.getTtlMinutes());
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password"
            );
        }
    }

    @Transactional
    public void register(String username, String rawPassword) {
        if (userRepo.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username already exists"
            );
        }
        var user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepo.save(user);
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }
}
