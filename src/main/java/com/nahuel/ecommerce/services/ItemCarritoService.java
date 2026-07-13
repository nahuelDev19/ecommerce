package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;

public interface ItemCarritoService {

    /*
    Agregar producto.
    Modificar cantidad.
    Eliminar producto.
    Buscar un ítem.
    */

    CarritoDto agregarItem(ItemRequestDto dto);
    CarritoDto disminuirItem(ItemRequestDto dto);
    CarritoDto eliminarItem(EliminarItemCarritoDto dto);


}
