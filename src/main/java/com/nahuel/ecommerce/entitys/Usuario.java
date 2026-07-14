package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
public class Usuario {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Carrito> carritos= new HashSet<>();
    private String nombreUsuario;
    private String contrasena;
    private String email;


}
