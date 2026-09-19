package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;
import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.mappers.CarritoMapper;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.nahuel.ecommerce.entitys.EstadoCarrito.ACTIVO;

@Transactional
@RequiredArgsConstructor
@Service
public class ItemCarritoServiceImp implements ItemCarritoService {

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoMapper carritoMapper;

    @Override
    public CarritoDto agregarItem(ItemRequestDto dto, UUID idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));


        Carrito carrito = carritoRepository.findByUsuarioIdAndEstadoCarrito(usuario.getId(), ACTIVO)
                .orElseGet(() -> {
                    Carrito carrito1 = new Carrito();
                    carrito1.setUsuario(usuario);
                    carrito1.setEstadoCarrito(ACTIVO);
                    carrito1.setCreadoEn(Instant.now());
                    return carritoRepository.save(carrito1);
                });

        Optional<ItemCarrito> itemExistente = itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId());

        if(dto.getCantidad()<1){
            throw new ReglaNegocioException("Cantidad no puede ser menor a 1 o negativo");
        }

        Instant ahora=Instant.now();
        if (itemExistente.isPresent()) {
            ItemCarrito item = itemExistente.get();
            item.setCantidad(item.getCantidad() + dto.getCantidad());
            itemCarritoRepository.save(item);
        } else {
            ItemCarrito item = new ItemCarrito();
            item.congelarPrecio(producto.getPrecioBase(),ahora);
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(dto.getCantidad());

            carrito.getItems().add(item);
            itemCarritoRepository.save(item);
        }

        carrito.setActualizadoEn(ahora);
        carrito.setUltimaInteraccion(ahora);

        return carritoMapper.toDto(carrito);
    }

    @Override
    public CarritoDto disminuirItem(ItemRequestDto dto, UUID idUsuario) {

        if (dto.getCantidad() <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor a cero");
        }

        Carrito carrito = carritoRepository
                .findByUsuarioIdAndEstadoCarrito(idUsuario, ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado o inactivo"));

        ItemCarrito item = itemCarritoRepository
                .findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe en el carrito"));

        int nuevaCantidad = item.getCantidad() - dto.getCantidad();

        if (nuevaCantidad <= 0) {
            carrito.getItems().remove(item);
            itemCarritoRepository.delete(item);
        } else {
            item.setCantidad(nuevaCantidad);
        }

        Instant ahora = Instant.now();
        carrito.setActualizadoEn(ahora);
        carrito.setUltimaInteraccion(ahora);

        carritoRepository.save(carrito);

        return carritoMapper.toDto(carrito);
    }

    @Override
    public CarritoDto eliminarItem(EliminarItemCarritoDto dto, UUID idUsuario) {

        Carrito carrito = carritoRepository
                .findByUsuarioIdAndEstadoCarrito(idUsuario, ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado o inactivo"));

        ItemCarrito item = itemCarritoRepository
                .findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe en el carrito"));

        carrito.getItems().remove(item);
        itemCarritoRepository.delete(item);

        Instant ahora = Instant.now();
        carrito.setActualizadoEn(ahora);
        carrito.setUltimaInteraccion(ahora);

        carritoRepository.save(carrito);

        return carritoMapper.toDto(carrito);
    }


}