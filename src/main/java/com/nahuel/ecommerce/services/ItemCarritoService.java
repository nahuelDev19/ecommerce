package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;

import java.util.UUID;

public interface ItemCarritoService {

    /*
    Agregar producto.
    Modificar cantidad.
    Eliminar producto.
    Buscar un ítem.
    */

    CarritoDto agregarItem(ItemRequestDto dto, UUID idUsuario);
    CarritoDto disminuirItem(ItemRequestDto dto, UUID idUsuario);
    CarritoDto eliminarItem(EliminarItemCarritoDto dto, UUID idUsuario);


}
