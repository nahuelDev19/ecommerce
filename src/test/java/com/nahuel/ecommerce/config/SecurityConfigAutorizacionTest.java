package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.security.JwtFilter;
import com.nahuel.ecommerce.security.UserDetailsServiceImp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigAutorizacionTest {

    @Mock
    private UserDetailsServiceImp userDetailsServiceImp;

    @Mock
    private JwtFilter jwtFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Nested
    @DisplayName("Tests para Beans de Infraestructura")
    class InfrastructureBeansTests {

        @Test
        @DisplayName("passwordEncoder debe retornar una instancia de BCryptPasswordEncoder")
        void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
            PasswordEncoder encoder = securityConfig.passwordEncoder();

            assertThat(encoder).isNotNull();
            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        }

        @Test
        @DisplayName("authenticationProvider debe configurar DaoAuthenticationProvider con su UserDetailsService")
        void authenticationProvider_ShouldConfigureDaoAuthenticationProvider() {
            AuthenticationProvider provider = securityConfig.authenticationProvider();

            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);

            // Verificamos únicamente la inyección del UserDetailsService mediante reflexión
            Object userDetailsService = ReflectionTestUtils.getField(provider, "userDetailsService");
            assertThat(userDetailsService).isSameAs(userDetailsServiceImp);
        }

        @Test
        @DisplayName("authenticationManager debe delegar a AuthenticationConfiguration")
        void authenticationManager_ShouldDelegateToAuthenticationConfiguration() throws Exception {
            AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
            AuthenticationManager expectedManager = mock(AuthenticationManager.class);
            when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expectedManager);

            AuthenticationManager actualManager = securityConfig.authenticationManager(authenticationConfiguration);

            assertThat(actualManager).isSameAs(expectedManager);
            verify(authenticationConfiguration, times(1)).getAuthenticationManager();
        }

        @Test
        @DisplayName("desactivarRegistroAutomatico debe registrar el filtro con enabled=false")
        void desactivarRegistroAutomatico_ShouldReturnDisabledFilterRegistrationBean() {
            FilterRegistrationBean<JwtFilter> registrationBean = securityConfig.desactivarRegistroAutomatico(jwtFilter);

            assertThat(registrationBean).isNotNull();
            assertThat(registrationBean.getFilter()).isSameAs(jwtFilter);
            assertThat(registrationBean.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("desactivarRegistroAutomatico con filtro null debe lanzar IllegalArgumentException")
        void desactivarRegistroAutomatico_WithNullFilter_ShouldThrowException() {
            // Se valida el comportamiento real de Spring Boot cuando el filtro es nulo
            assertThrows(IllegalArgumentException.class, () -> {
                securityConfig.desactivarRegistroAutomatico(null);
            });
        }
    }

    @Nested
    @DisplayName("Tests para SecurityFilterChain")
    class SecurityFilterChainTests {

        @Test
        @DisplayName("filterChain debe configurar HttpSecurity y retornar el SecurityFilterChain")
        void filterChain_ShouldConfigureHttpSecurityAndBuild() throws Exception {
            HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);

            when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
            when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
            when(httpSecurity.exceptionHandling(any())).thenReturn(httpSecurity);
            when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
            when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
            when(httpSecurity.build()).thenReturn(expectedChain);

            SecurityFilterChain actualChain = securityConfig.filterChain(httpSecurity);

            assertThat(actualChain).isSameAs(expectedChain);
            verify(httpSecurity).build();
        }
    }
}