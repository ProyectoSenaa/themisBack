package co.sena.edu.themis.config;

import co.sena.edu.themis.Security.JWTFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JWTFilter jwtFilter() {
        return new JWTFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // RequestMatcher that allows any path that ends with /graphql (handles federated services)
        RequestMatcher graphqlMatcher = request -> {
            String uri = request.getRequestURI();
            return uri != null && uri.endsWith("/graphql");
        };

        http
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Allow developer UIs and actuator
                        .requestMatchers("/graphiql/**", "/actuator/**").permitAll()
                        // Allow any GraphQL endpoint (use custom RequestMatcher because PathPattern "**/graphql" is not supported)
                        .requestMatchers(graphqlMatcher).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}