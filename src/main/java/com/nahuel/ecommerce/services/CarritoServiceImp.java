package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class CarritoServiceImp implements CarritoService{

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public Carrito guardar(UUID usuarioId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);

        return carritoRepository.save(carrito);
    }

    @Override
    public List<Carrito> listar() {
        return carritoRepository.findAll();
    }

    @Override
    public Optional<Carrito> buscarPorId(UUID id) {
        return carritoRepository.findById(id);
    }

    @Override
    public void eliminar(UUID id) {
        carritoRepository.deleteById(id);
    }
}
