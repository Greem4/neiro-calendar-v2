package ru.greemlab.neirocalendarv2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)              // CSRF не нужен REST-у
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())        // вообще ВСЁ под пароль
                .httpBasic(Customizer.withDefaults());     // самый простой способ
        return http.build();
    }
}
