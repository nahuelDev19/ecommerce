package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.ImportacionJob;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportacionJobRepository extends JpaRepository<ImportacionJob, UUID> {
}
