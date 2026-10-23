# Ecommerce

Backend de un ecommerce desarrollado como proyecto de aprendizaje con Java 17, Spring Boot 4.1, Spring Security, JPA/Hibernate, PostgreSQL y Flyway. Incluye productos, importación de Excel, usuarios, carrito, cupones, órdenes y pagos con Mercado Pago.

## Checkout Pro y Checkout API

[Checkout Pro](https://www.mercadopago.com.ar/developers/en/reference/online-payments/checkout-pro-preferences/overview) redirige al comprador al entorno de Mercado Pago para completar el pago. [Checkout API](https://www.mercadopago.com.ar/developers/en/reference/online-payments/checkout-api/overview) permite integrar el cobro dentro del sitio o aplicación, con mayor control sobre la experiencia y más trabajo de integración.

En este proyecto usamos **Checkout Pro mediante preferencias**: permite concentrarnos en calcular la compra, conservar sus datos y confirmar el cobro por webhook, sin desarrollar un formulario de pago propio. El backend devuelve una URL; abrirla no significa que la orden esté pagada.

### SDK de Java o RestClient

Mercado Pago ofrece un [SDK oficial para Java](https://github.com/mercadopago/sdk-java), publicado como `com.mercadopago:sdk-java`, con clientes y modelos para sus operaciones. Es una alternativa válida para evitar escribir parte del intercambio HTTP.

Elegimos **RestClient**, disponible mediante `spring-boot-starter-restclient`. Para las operaciones actuales permite ver explícitamente los endpoints, los datos enviados y los errores, sin agregar el SDK. La contrapartida es que debemos mantener los DTO y las traducciones de la API. `MercadoPagoAdapter` concentra esas llamadas; `PasarelaPago` entrega tipos propios para que el checkout no dependa de los modelos del proveedor.

## Requisitos

- JDK 17 y Maven, o el Maven Wrapper incluido.
- PostgreSQL para ejecutar la aplicación.
- Docker Desktop funcionando para los tests de integración con Testcontainers.
- Un proyecto Supabase para validar el inicio de sesión externo.
- Una aplicación de Mercado Pago y cuentas de prueba para comprobar el pago real.

El `pom.xml` configura Java 17. El Dockerfile actual compila y ejecuta con Java 21.

## Variables de entorno

Configurar las variables en la ejecución de IntelliJ, en la terminal o en Render. Los valores de ejemplo son ficticios; no deben usarse como secretos reales. El proyecto no carga automáticamente archivos `.env`.

| Variable | Uso / ejemplo |
| --- | --- |
| `DB_URL` | JDBC de PostgreSQL: `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USER` | Usuario de la base |
| `DB_PASS` | Contraseña de la base |
| `JWT_SECRET` | Clave aleatoria privada codificada en Base64, con al menos 32 bytes decodificados |
| `JWT_EXPIRATION` | Duración del JWT en milisegundos; por defecto `3600000` |
| `SUPABASE_URL` | URL del proyecto Supabase; se usa para consultar sus claves públicas y validar el emisor |
| `ADMIN_EMAIL` | Email requerido por la configuración inicial del administrador |
| `PASS_ADMIN` | Contraseña inicial; vacía o ausente omite la creación del administrador |
| `MERCADOPAGO_ACCESS_TOKEN` | Access token privado de la aplicación vendedora utilizada en la prueba |
| `MERCADOPAGO_WEBHOOK_SECRET` | Clave secreta de firma del webhook; es distinta del access token |
| `MERCADOPAGO_API_URL` | `https://api.mercadopago.com` |
| `MERCADOPAGO_NOTIFICATION_URL` | URL HTTPS pública del endpoint `/api/webhooks/mercadopago` |
| `PORT` | Puerto HTTP; por defecto `8080` |

`PASS_ADMIN` sirve para crear el administrador si no existe; cambiarla no actualiza automáticamente la contraseña de un administrador existente.

## Ejecución local

1. Clonar el repositorio y entrar en su carpeta:

   ```powershell
   git clone https://github.com/nahuelDev19/ecommerce.git
   cd ecommerce
   ```

2. Crear una base PostgreSQL vacía para desarrollo y configurar las variables anteriores con sus valores locales. No usar la base de producción para ejecutar tests.
3. Comprobar que Maven use el JDK esperado:

   ```powershell
   .\mvnw.cmd -version
   ```

4. Iniciar la aplicación:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

La API queda en `http://localhost:8080`, salvo que se cambie `PORT`. También se puede ejecutar la clase principal desde IntelliJ con las mismas variables.

Flyway aplica las migraciones de `src/main/resources/db/migration` y Hibernate valida el esquema con `ddl-auto: validate`. Las migraciones aplicadas no se editan: un cambio posterior del esquema requiere otra versión. El vencimiento actual está en `checkout.vigencia-orden: PT2H`, dentro de `application.yml`.

Para empaquetar después de configurar el entorno de tests:

```powershell
.\mvnw.cmd clean package
```

## Flujo de checkout

1. El usuario se autentica y envía su JWT en `Authorization: Bearer <token>`.
2. El servidor busca y bloquea su carrito activo, valida los ítems y la moneda y calcula el total, incluyendo descuentos.
3. Copia productos, cantidades y precios a `ItemOrden`. El precio congelado del carrito dura 48 horas; después se usa el precio actual. Si el precio guardado no tiene fecha, se conserva.
4. Guarda la orden en `PENDIENTE_PAGO` y un pago en `PENDIENTE`.
5. Solicita la preferencia, guarda `preferenciaId` y `urlPago` y devuelve `ordenId` y `urlPago`. Si la llamada falla y propaga una excepción, la transacción local revierte los registros. Eso no revierte una preferencia que el proveedor pudiera haber creado antes de un fallo de comunicación.
6. El carrito pasa a `ORDENADO`, conservando sus ítems. El bloqueo y el cambio de estado evitan reutilizar el mismo carrito al repetir el checkout.
7. Mercado Pago cobra en su entorno y envía un aviso al webhook. El servicio valida su firma, consulta el pago real y comprueba referencia, importe y moneda antes de actualizar los registros.

La preferencia incluye `external_reference` con el UUID de nuestra orden. El cobro se representa como un único concepto por el total final; el detalle permanece en los ítems de la orden. El enum interno conserva `ARG`, traducido a `ARS` al comunicarse con Mercado Pago.

### Endpoints

Todos los endpoints de checkout requieren autenticación. El usuario se obtiene del JWT y el POST no recibe un monto ni un ID de usuario en el cuerpo.

| Método | Ruta | Resultado |
| --- | --- | --- |
| POST | `/api/checkout` | `ordenId` y `urlPago` |
| GET | `/api/checkout/ordenes` | Órdenes del usuario, más nuevas primero |
| GET | `/api/checkout/ordenes/{ordenId}` | Estado, ítems, importes, moneda y fechas |
| GET | `/api/checkout/ordenes/{ordenId}/estado` | Consulta del estado conservada por compatibilidad |
| POST | `/api/webhooks/mercadopago` | Recepción de avisos firmados del proveedor |

Una orden de otro usuario responde `404`. Un carrito vacío se rechaza con `400`.

### Webhook y vencimiento

El cuerpo del aviso no se usa como prueba del cobro: el estado y los importes se consultan en la API autenticada. El adapter consulta el pago y su orden comercial para identificar la preferencia.

El flujo utiliza bloqueo de la orden y unicidad de `pago_externo_id`; un aviso idéntico no vuelve a modificar los datos. La concurrencia simultánea no está demostrada por los tests actuales.

Una firma inválida responde `401`. Los fallos internos que llegan al controlador se registran y responden `200`, fuera de la transacción del servicio para permitir su rollback. Esto confirma recepción incluso si falló el procesamiento: ese fallo no provoca un reintento por respuesta HTTP y no hay recuperación automática implementada. Peticiones rechazadas previamente por Spring, como JSON mal formado, conservan su respuesta normal.

Las órdenes vencen a las dos horas. Un cron cada dos horas, en zona `America/Argentina/Buenos_Aires`, cancela las que continúan pendientes y alcanzaron su vencimiento. La cancelación ocurre en la siguiente ejecución y requiere que la aplicación esté funcionando. El campo actual es `venceEn`, persistido como `vence_en`.

**Pendiente de consulta con el profesor:** la política comercial para pagos aprobados de órdenes ya canceladas. La propuesta está comentada y desactivada en `WebhookService`; actualmente un pago aprobado puede pasar una orden cancelada a pagada sin comparar el plazo. No hay gestión de incidencias, notificaciones comerciales ni devoluciones automáticas. Tampoco se implementaron pantallas de retorno.

## Prueba manual con Mercado Pago

Se realizó una prueba previa en Render con cuentas de prueba: el checkout devolvió la URL, se pagó con saldo del comprador de prueba y el webhook dejó un pago `APROBADO` y la orden `PAGADA`, con importe y referencia coincidentes y fecha de aprobación guardada. Esa prueba corresponde al caso exitoso de la implementación de ese momento; debe repetirse después de las modificaciones posteriores.

Pasos para repetirla:

1. Desplegar el backend en Render y cargar manualmente las variables de entorno. El access token y la clave del webhook deben corresponder a la integración utilizada.
2. Configurar los avisos de pagos en Mercado Pago hacia `https://ecommerce-aaiw.onrender.com/api/webhooks/mercadopago`, y usar esa misma URL en `MERCADOPAGO_NOTIFICATION_URL`.
3. Autenticarse en el ecommerce mediante `POST /auth/login`, con `email` y `contrasena`, y conservar el JWT devuelto.
4. Agregar un producto existente mediante `POST /api/carritos/agregar`, con el JWT y un cuerpo como `{"productoId":"<UUID del producto>","cantidad":1}`. El servicio crea el carrito activo si hace falta.
5. Enviar `POST /api/checkout` con el JWT y sin cuerpo. Conservar `ordenId` y abrir `urlPago`.
6. Pagar con la cuenta compradora de prueba, diferente de la vendedora; en la prueba previa se usó saldo de prueba.
7. Consultar `GET /api/checkout/ordenes/{ordenId}` con el JWT del propietario y comprobar `PAGADA`, total y `pagadoEn`. Verificar en los registros persistidos el pago aprobado y su referencia a la orden.

Se utilizó Render para disponer de una URL pública. Ngrok puede exponer un backend local mediante un túnel HTTPS como alternativa, pero no fue el mecanismo utilizado en esa prueba.

## Tests

Iniciar Docker Desktop y ejecutar desde la raíz:

```powershell
.\mvnw.cmd test
```

Para ejecutar solo los tests de integración de checkout:

```powershell
.\mvnw.cmd -Dtest=CheckoutIntegracionTest test
```

También se pueden ejecutar clases o métodos desde IntelliJ. El perfil `test` contiene claves ficticias; los tests de integración utilizan PostgreSQL temporal con Testcontainers. Hibernate crea y elimina ese esquema y Flyway está deshabilitado en ese perfil: estos tests no validan los scripts de migración.

Hay cobertura de precios copiados, carrito vacío, acceso a órdenes propias y ajenas, firma del webhook y respuestas del controlador. Se conservan los tests existentes del webhook aprobado y repetido. Las llamadas de pago de los tests existentes se simulan; no deben apuntar a Render, Neon ni utilizar credenciales reales.

Este README documenta el código revisado; su creación no implica una nueva ejecución de la suite ni una nueva prueba manual del deploy.
