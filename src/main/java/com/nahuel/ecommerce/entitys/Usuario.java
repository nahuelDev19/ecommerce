package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
public class Usuario implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Carrito> carritos= new HashSet<>();
    private String nombreUsuario;
    private String contrasena;
    private String email;
    private Boolean activo;

    @Enumerated(EnumType.STRING)
    private Rol rol;


    private UUID supabaseId;
    @Enumerated(EnumType.STRING)
    private OrigenRegistro origenRegistro;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public @Nullable String getPassword() {
        return getContrasena();
    }

    @Override
    public String getUsername() {
        return getEmail();
    }
}
