# Cliente: el cielo de la luna de sangre

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **un cliente vanilla no puede entrar al servidor**, **el cielo queda rojo después de la luna (o en otro servidor)**, o **el servidor dedicado no arranca por una clase de `net.minecraft.client`**.

Con el mod también en el cliente, la luna de sangre se ve: la luna roja y 1,5 veces más grande, y el cielo, la niebla, las nubes y la luz del cielo corridos hacia el rojo. **Es opcional:** sin el mod en el cliente todo lo demás anda igual (título, barra, niveles).

| Pieza | Dónde | Qué hace |
|---|---|---|
| `PaqueteLunaDeSangre` | `src/main` | `CustomPacketPayload` con un `boolean`, registrado en `PayloadTypeRegistry.clientboundPlay()` desde `onInitialize` (lo necesitan los dos lados) |
| `LunaDeSangre.avisarClientes` | `src/main` | Cada segundo, desde `actualizar`: a cada jugador con `laVe` distinto de lo último que se le avisó (`AVISADOS`), si `ServerPlayNetworking.canSend`, le manda el paquete |
| `BloodmoonAndBossesCliente` | `src/client` | Entrypoint `client`: recibe el paquete, el tick del cliente y `DISCONNECT` (vuelve a cero) |
| `LunaDeSangreCliente` | `src/client` | La intensidad (0 a 1, entra y sale en 60 ticks, interpolada con el tick parcial) y los colores objetivo con su fuerza |
| `EnvironmentAttributeProbeMixin` | `src/client` | `@ModifyReturnValue` en `EnvironmentAttributeProbe.getValue(atributo, parcial)`: `SKY_COLOR`, `FOG_COLOR`, `CLOUD_COLOR` (un `Vector4fc`) y `SKY_LIGHT_COLOR` |
| `SkyRendererMixin` | `src/client` | Dos `@ModifyArg` en `renderMoon`: el tamaño de `applyCelestialBodyTransform` (índice 2) y el color de `DynamicGpuData.writeTransform(Matrix4f, Vector4f)` |

- **El cliente no sabe qué noche es de sangre:** se sortea en el servidor. Por eso el paquete, y no algo que el cliente calcule.
- **`canSend` antes de mandar:** a un cliente vanilla nunca le llega, y registrar el tipo de paquete no lo afecta. Comparar con `AVISADOS` cada segundo (y no sólo en `empezar`/`terminar`) cubre al que se conecta, al que vuelve del Nether y a un `/bloodmoon recargar` que apague la luna. `JOIN` lo saca de `AVISADOS`: un cliente que se reconecta arranca sin luna.
- **`laVe` es `activa(level)` del nivel del jugador**, que ya exige el overworld: en el Nether el paquete dice `false`.
- **Todo lo que dibuja la cámara pasa por `EnvironmentAttributeProbe.getValue`** (`SkyRenderer.extractRenderState`, `AtmosphericFogEnvironment`, `LightmapRenderStateExtractor`): un solo mixin tiñe cielo, niebla, nubes y luz. Se devuelve un vector nuevo: el de vanilla puede ser una constante compartida. El mixin mira `level.isClientSide()`: en singleplayer el servidor integrado corre en el mismo proceso.
- **La luna se pinta multiplicando su color** (`Vector4f(1, 1, 1, alfa)`, recién creado en `renderMoon`): verde y azul bajan, la transparencia (lluvia, día) no se toca.
- **`splitEnvironmentSourceSets`:** el código de cliente está en `src/client`, así `src/main` no compila si usa una clase de `net.minecraft.client` por error. Lo de Jade sigue en `src/main` (no usa clases de cliente; ver [jade](jade.md)).
- Los colores no son números de balance: son constantes de `LunaDeSangreCliente`, no van en `Configuracion` (que es del servidor).

## Ver también

- [Eventos y afijos](eventos.md): cuándo hay luna de sangre.
- [Tests: cliente](../tests/cliente.md)
