package ru.greemlab.neirocalendarv2.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.greemlab.neirocalendarv2.service.JwtService;
import io.jsonwebtoken.JwtException;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserDetailsService uds;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        var auth = req.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var token = auth.substring(7);
            try {
                if (jwt.validateToken(token)) {
                    var username = jwt.extractUsername(token);
                    var user = uds.loadUserByUsername(username);

                    var at = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    at.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(at);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // подпись/срок/повреждение токена — чистим контекст и идём дальше
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
