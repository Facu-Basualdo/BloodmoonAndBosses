# Tests del cliente

[← Índice Tests](INDICE.md)

`gradlew runClientGameTest` abre un cliente de verdad (con ventana), crea un mundo, corre `LunaDeSangreClienteTest` y se cierra. Tarda unos 30 segundos. **Correlo si tocás algo de `src/client`:** `runGameTest` es un servidor y no aplica los mixins de cliente.

| Test | Qué verifica |
|---|---|
| `LunaDeSangreClienteTest` | Con `/bloodmoon evento luna_de_sangre` la intensidad llega a 1 (el paquete llegó y el color entró) · al amanecer vuelve a 0 · saca tres capturas: noche común, luna de sangre mirando arriba y el horizonte |

- **Las capturas quedan en `build/run/clientGameTest/screenshots/`** (`0000_bloodmoonandbosses_noche_comun.png`…). El test no puede saber si los colores "se ven bien": se miran a ojo después de cambiarlos.
- **El mundo arranca en luna llena:** a medianoche del día 0 el sorteo natural (50 %) puede darle una luna de sangre antes de la captura de la noche común. Por eso el test va al día 1 (`time set 42000`, luna gibosa).
- El entrypoint es `fabric-client-gametest` en `src/gametest/resources/fabric.mod.json`, con `enableClientGameTests = true` en `build.gradle`. `runGameTest` no lo corre.
- `LunaDeSangre.laVe` (a quién se le avisa) se prueba en el servidor, dentro de `lunaDeSangre`: en el overworld sí, en el Nether no.

## Ver también

- [Cliente](../mod/cliente.md)
