package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.security.JwtFilter;
import com.nahuel.ecommerce.security.UserDetailsServiceImp;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration @EnableMethodSecurity
public class SecurityConfig {


    private final UserDetailsServiceImp userDetailsServiceImp;
    private final JwtFilter jwtFilter;



    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(HttpMethod.POST,   "/api/productos/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT,    "/api/productos/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.GET, "/api/cupones/cupones/**").authenticated()
                            .requestMatchers("/api/cupones/**").hasRole("ADMIN")
                            .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                            .requestMatchers("/auth/**").permitAll()
                            .requestMatchers("/api/webhooks/mercadopago").permitAll()
                                .anyRequest().authenticated()


                )

                .exceptionHandling(ex->
                        ex.authenticationEntryPoint(
                                (request, rest,e)
                                        -> rest.sendError(HttpServletResponse.SC_UNAUTHORIZED,"No autorizado"))
                                .accessDeniedHandler(((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN,"Acceso denegado"))))
                .sessionManagement(sesion ->sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider= new DaoAuthenticationProvider(userDetailsServiceImp);
        provider.setPasswordEncoder(passwordEncoder());
        return  provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public FilterRegistrationBean<JwtFilter> desactivarRegistroAutomatico(JwtFilter filtro){
        FilterRegistrationBean<JwtFilter> registro =
                new FilterRegistrationBean<>(filtro);

        registro.setEnabled(false);

        return registro;
    }
}
