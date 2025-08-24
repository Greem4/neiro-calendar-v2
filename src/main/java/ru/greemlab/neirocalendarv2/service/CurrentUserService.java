package ru.greemlab.neirocalendarv2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendarv2.domain.entity.UserAccount;
import ru.greemlab.neirocalendarv2.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public UserAccount getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        var username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Username not found"));
    }
}
