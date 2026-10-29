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

                .authorizeHttpRequests(auth -> auth
                        // ROTTE PUBBLICHE
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/**",
                                "/error",
                                "/error/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/uploads/**",  // Permettiamo l'accesso alle immagini caricate
                                "/api/upload"
                        ).permitAll()

                        // ROTTE VIAGGI:
                        // 1. Solo l'organizzatore può vedere i "suoi" viaggi, crearne di nuovi o eliminarli
                        .requestMatchers("/api/trips/my-trips", "/api/trips/create").hasRole("ORGANIZZATORE")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/trips/**").hasRole("ORGANIZZATORE")
                        // 2. Qualsiasi utente loggato (Viaggiatore o Organizzatore) può visualizzare i viaggi e i dettagli
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/trips/**").authenticated()
                        .requestMatchers("/api/trips/**").hasRole("ORGANIZZATORE")

                        /// ROTTE VIAGGIATORE
                        // Solo il viaggiatore può creare prenotazioni e vedere il SUO storico
                        .requestMatchers("/api/reservations/create/**", "/api/reservations/my-reservations", "/api/lists/**").hasRole("VIAGGIATORE")

                        // ROTTE AGENZIA
                        // Solo l'organizzatore può vedere gli ordini ricevuti
                        .requestMatchers("/api/reservations/agency-orders").hasRole("ORGANIZZATORE")

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
