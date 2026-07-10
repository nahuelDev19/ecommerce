package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.AgregarItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;

import java.util.List;
import java.util.UUID;

public interface ItemCarritoService {

    /*
    Agregar producto.
    Modificar cantidad.
    Eliminar producto.
    Buscar un ítem.
    */

    CarritoDto agregarProducto(UUID carritoId, AgregarItemRequestDto dto);

    CarritoDto actualizarCantidad(UUID carritoId, UUID itemId, Integer cantidad);

    CarritoDto eliminarProducto(UUID carritoId, UUID itemId);

    ItemCarritoDto obtenerPorId(UUID itemId);

    List<ItemCarritoDto> listarPorCarrito(UUID carritoId);


}
