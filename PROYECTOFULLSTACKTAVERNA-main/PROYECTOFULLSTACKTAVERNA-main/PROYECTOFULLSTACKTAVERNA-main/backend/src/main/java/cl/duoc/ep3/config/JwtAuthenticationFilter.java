package cl.duoc.ep3.config;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            try {
                // El rol de la cuenta autenticada queda disponible para las reglas de SecurityConfig.
                jwtService.authenticate(token).ifPresent(authenticated -> {
                    Account account = authenticated.session().getAccount();
                    var authentication = new UsernamePasswordAuthenticationToken(
                            account,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
