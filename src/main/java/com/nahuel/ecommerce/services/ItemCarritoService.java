package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.AgregarItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;

public interface ItemCarritoService {

    /*
    Agregar producto.
    Modificar cantidad.
    Eliminar producto.
    Buscar un ítem.
    */

    CarritoDto agregarItem(AgregarItemRequestDto dto);
    CarritoDto disminuirItem(AgregarItemRequestDto dto);
    CarritoDto eliminarItem(AgregarItemRequestDto dto);


}
