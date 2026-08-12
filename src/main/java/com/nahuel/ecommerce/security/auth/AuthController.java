package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.security.auth.dto.GoogleLoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.RegistroRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {


    private final AuthService authService;

    @PostMapping("/oauth")
    public ResponseEntity<TokenResponseDto> loginOAuth(
            @Valid @RequestBody GoogleLoginRequestDto request) {

        return ResponseEntity.ok(
                authService.loginOAuth(request.tokenSupabase())
        );
    }


    @PostMapping("/register")
    public ResponseEntity<TokenResponseDto> registrar(
            @Valid @RequestBody RegistroRequestDto request) {

        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.loguear(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        authService.logout();
        return ResponseEntity.ok("Logout exitoso");
    }


}
