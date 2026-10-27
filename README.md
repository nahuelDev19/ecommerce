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

## Probar la API desplegada en Render

El backend está desplegado en Render y se puede probar desde **Postman**, utilizando esta URL base:

```text
https://ecommerce-aaiw.onrender.com
```

El servicio utiliza una instancia gratuita que se suspende después de un período de inactividad. Por eso, **la primera solicitud puede demorar varios minutos** mientras se reactiva e inicia la aplicación. Durante ese tiempo, esperar la respuesta antes de continuar.

Una vez que la aplicación está activa, las siguientes solicitudes se procesan con normalidad. Si transcurre otro período de inactividad, el servicio puede volver a suspenderse y la siguiente solicitud tendrá nuevamente esa demora.

La API no incluye una interfaz web de registro o inicio de sesión. Para seguir las pruebas de este apartado, usar Postman: abrir las direcciones directamente en el navegador no permite enviar los métodos, cuerpos JSON y tokens necesarios. No hace falta acceder a una cuenta de Render ni configurar un entorno local para probar la API publicada.

### 1. Registrar un usuario

Crear una solicitud con el método **POST**:

```text
https://ecommerce-aaiw.onrender.com/auth/register
```

En **Authorization**, seleccionar **No Auth**. En **Body → raw → JSON**, enviar:

```json
{
  "email": "tu-correo@example.com",
  "contrasena": "Prueba12345",
  "nombreUsuario": "Usuario de prueba"
}
```

Reemplazar los datos por los del usuario de prueba. La contraseña debe tener al menos ocho caracteres, una mayúscula, una minúscula y un número.

Si el registro es exitoso, la respuesta devuelve un token para autenticar las siguientes solicitudes.

### 2. Iniciar sesión

Si el usuario ya está registrado, enviar **POST** a:

```text
https://ecommerce-aaiw.onrender.com/auth/login
```

Seleccionar **Authorization → No Auth** y enviar en **Body → raw → JSON**:

```json
{
  "email": "tu-correo@example.com",
  "contrasena": "Prueba12345"
}
```

Conservar el token devuelto. En las siguientes solicitudes, seleccionar **Authorization → Bearer Token** y pegar únicamente el valor del token, sin agregar la palabra `Bearer`.

Usar el token del mismo usuario durante toda la prueba del carrito y del checkout. Si vence, iniciar sesión nuevamente.

### 3. Consultar productos

Enviar **GET**, con el Bearer Token y sin cuerpo:

```text
https://ecommerce-aaiw.onrender.com/api/productos/filtrados
```

La respuesta contiene los productos paginados. Copiar el ID de un producto para agregarlo al carrito.

### 4. Agregar un producto al carrito

Enviar **POST**, con el Bearer Token:

```text
https://ecommerce-aaiw.onrender.com/api/carritos/agregar
```

En **Body → raw → JSON**, enviar:

```json
{
  "productoId": "UUID del producto",
  "cantidad": 1
}
```

Reemplazar `UUID del producto` por un ID obtenido en la consulta anterior. Si el usuario no tiene un carrito activo, el servicio lo crea automáticamente.

Para consultar el carrito activo, enviar **GET**, con el mismo token y sin cuerpo:

```text
https://ecommerce-aaiw.onrender.com/api/carritos/mi-carrito
```

### 5. Iniciar el checkout

Con un carrito que contenga productos, enviar **POST**:

```text
https://ecommerce-aaiw.onrender.com/api/checkout
```

Usar el mismo Bearer Token y seleccionar **Body → none**. El servidor identifica al usuario mediante el token y calcula el total; no se envían el ID del carrito ni el importe.

La respuesta incluye:

```json
{
  "ordenId": "UUID de la orden",
  "urlPago": "URL de pago de Mercado Pago"
}
```

Conservar `ordenId` y abrir `urlPago` en el navegador para continuar en Mercado Pago. Abrir esa URL no significa que la orden esté pagada.

Para completar el pago de prueba, seguir las indicaciones del apartado [Configuración y prueba manual con Mercado Pago](#configuración-y-prueba-manual-con-mercado-pago) y utilizar la cuenta compradora de prueba correspondiente.

### 6. Consultar la orden

Enviar **GET**, con el token del usuario que inició el checkout y sin cuerpo:

```text
https://ecommerce-aaiw.onrender.com/api/checkout/ordenes/{ordenId}
```

Reemplazar `{ordenId}` por el valor devuelto al iniciar el checkout.

Cuando el pago sea aprobado y el webhook se procese correctamente, la orden debería figurar como `PAGADA`. La respuesta también permite consultar los ítems, los importes y las fechas.

Para listar las órdenes del usuario, enviar **GET**, con el mismo token y sin cuerpo, a:

```text
https://ecommerce-aaiw.onrender.com/api/checkout/ordenes
```

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

## Configuración y prueba manual con Mercado Pago

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
