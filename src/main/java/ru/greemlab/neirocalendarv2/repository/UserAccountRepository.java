package ru.greemlab.neirocalendarv2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.greemlab.neirocalendarv2.domain.entity.UserAccount;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
}
