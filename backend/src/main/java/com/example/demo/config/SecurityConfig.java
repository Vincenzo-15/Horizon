package com.example.demo.config;

import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disabilitiamo il CSRF (necessario per le API REST con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Creiamo le regole del nostro "Buttafuori"
                .authorizeHttpRequests(auth -> auth
                        // ROTTE PUBBLICHE: Login, Registrazione e Documentazione Swagger aperti a tutti
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ROTTE ORGANIZZATORE: Solo chi ha questo ruolo può gestire i viaggi
                        .requestMatchers("/api/trips/**").hasRole("ORGANIZZATORE")

                        // ROTTE VIAGGIATORE: Solo chi ha questo ruolo prenota e crea liste
                        .requestMatchers("/api/reservations/**", "/api/lists/**").hasRole("VIAGGIATORE")

                        // QUALSIASI ALTRA ROTTA: Devi essere almeno loggato
                        .anyRequest().authenticated()
                )

                // Essendo REST, non usiamo le sessioni lato server (gestiamo tutto col Token)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Inseriamo il nostro Filtro JWT PRIMA di quello standard di Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
