package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.AgregarItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.nahuel.ecommerce.entitys.EstadoCarrito.ACTIVO;

@RequiredArgsConstructor
@Service
public class ItemCarritoServiceImp implements ItemCarritoService{

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;

    /*
    Buscar carrito

↓

Buscar producto

↓

¿Existe ese producto en el carrito?

        Sí -----------------> aumentar cantidad

        No -----------------> crear ItemCarrito

↓

Guardar

↓

Devolver carrito actualizado
     */

    public CarritoDto agregarItem(AgregarItemRequestDto dto){

        Usuario usuario= usuarioRepository.findById(dto.getUsuarioId()).orElseThrow(()-> new RuntimeException("usuario no encontrado"));
        Producto producto = productoRepository.findById(dto.getProductoId()).orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Carrito carrito= carritoRepository.findByUsuarioIdAndEstadoCarrito(dto.getUsuarioId(),ACTIVO)
                .orElseGet(()-> {
                    Carrito carrito1 = new Carrito();
                    carrito1.setUsuario(usuario);
                    carrito1.setEstadoCarrito(ACTIVO);
                    return carritoRepository.save(carrito1);
                });
        Optional<ItemCarrito> itemExistente =itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId());


        if (itemExistente.isPresent()) {

            ItemCarrito item = itemExistente.get();
            item.setCantidad(item.getCantidad() + dto.getCantidad());

            itemCarritoRepository.save(item);

        } else {

            ItemCarrito item = new ItemCarrito();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(dto.getCantidad());
            item.setPrecioUnitario(producto.getPrecioBase());

            carrito.getItems().add(item);
            itemCarritoRepository.save(item);
        }


        return toDto(carrito);
    }
/*

    @Override
    public CarritoDto agregarProducto(UUID carritoId, AgregarItemRequestDto dto) {

        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Optional<ItemCarrito> itemExistente =
                itemCarritoRepository.findByCarritoIdAndProductoId(carritoId, producto.getId());

        if (itemExistente.isPresent()) {

            ItemCarrito item = itemExistente.get();
            item.setCantidad(item.getCantidad() + dto.getCantidad());

            itemCarritoRepository.save(item);

        } else {

            ItemCarrito item = new ItemCarrito();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(dto.getCantidad());
            item.setPrecioUnitario(producto.getPrecioBase());

            itemCarritoRepository.save(item);
        }

        CarritoDto carritoDto= new CarritoDto();
        carritoDto.setItems(carrito.getItems());
        // después devolverás el carrito actualizado
        return carritoDto;
    }
 */

    @Override
    public CarritoDto actualizarCantidad(UUID carritoId, UUID itemId, Integer cantidad) {

        ItemCarrito item = itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        item.setCantidad(cantidad);

        itemCarritoRepository.save(item);

        return null;
    }

    @Override
    public CarritoDto eliminarProducto(UUID carritoId, UUID itemId) {

        ItemCarrito item = itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));

        itemCarritoRepository.delete(item);

        return null;
    }

    @Override
    public ItemCarritoDto obtenerPorId(UUID itemId) {

        ItemCarrito item = itemCarritoRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado"));
        return null;
    }

    @Override
    public List<ItemCarritoDto> listarPorCarrito(UUID carritoId) {

        return itemCarritoRepository.findByCarritoId(carritoId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ItemCarritoDto toDto1(ItemCarrito item) {

        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());

        dto.setSubtotal(
                item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad()))
        );

        return dto;
    }

    private CarritoDto toDto(Carrito carrito) {

        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());
        dto.setSubtotal(carrito.getSubtotal());
        dto.setDescuentoTotal(carrito.getDescuentoTotal());
        dto.setTotal(carrito.getTotal());

        dto.setItems(
                carrito.getItems()
                        .stream()
                        .map(this::toDto)
                        .toList()
        );

        return dto;
    }

    private ItemCarritoDto toDto(ItemCarrito item) {

        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(item.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(item.getCantidad())));

        return dto;
    }

}
