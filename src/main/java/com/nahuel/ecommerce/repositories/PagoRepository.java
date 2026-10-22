package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    Optional<Pago> findByPagoExternoId(String pagoExternoId);

    List<Pago> findByOrdenId(UUID ordenId);


}
