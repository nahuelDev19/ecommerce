package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.AgregarItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ItemCarritoServiceImp implements ItemCarritoService{

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
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

        // después devolverás el carrito actualizado
        return null;
    }

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

        return toDto(item);
    }

    @Override
    public List<ItemCarritoDto> listarPorCarrito(UUID carritoId) {

        return itemCarritoRepository.findByCarritoId(carritoId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ItemCarritoDto toDto(ItemCarrito item) {

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

}
