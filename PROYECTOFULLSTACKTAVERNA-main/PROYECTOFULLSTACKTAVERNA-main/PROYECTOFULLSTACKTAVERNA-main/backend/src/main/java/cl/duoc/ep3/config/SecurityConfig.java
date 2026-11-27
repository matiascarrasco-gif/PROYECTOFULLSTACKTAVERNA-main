package cl.duoc.ep3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
                // Las lecturas son públicas; el resto se limita según sesión y rol.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/admin/register", "/api/salud",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/uploads/product-images/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/uploads/product-images").hasRole("ADMIN")
                        .requestMatchers("/api/uploads/product-images/**").denyAll()
                        .requestMatchers("/api/orders/**", "/api/rewards/**").authenticated()
                        // Solo ADMIN puede modificar el catálogo.
                        .requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> disableContainerJwtFilterRegistration(
            JwtAuthenticationFilter jwtFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
