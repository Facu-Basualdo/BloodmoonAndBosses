# Tests de configuración

[← Índice Tests](INDICE.md)

El test `archivoDeConfiguracion` prueba `ArchivoDeConfiguracion.leer` sobre archivos en una carpeta temporal. **Nunca toca `MobsPoderosos.config`:** los tests corren en paralelo, y cambiar la configuración global haría fallar al azar a los que dependen de los valores por defecto.

## Qué cubre

| Caso | Qué verifica |
|---|---|
| Sin archivo | Devuelve un record igual a `POR_DEFECTO` y crea el archivo con `"bloquesPorNivel": 100` |
| Archivo parcial (`bloquesPorNivel`, `vida.tope` y una clave `inventada`) | Lee lo que hay; dentro de `vida`, `porNivel` queda por defecto; `nivelMaximo` queda por defecto; el archivo se reescribe con todas las claves y sin `inventada` |
| `bloquesPorNivel: 0` | Tira excepción (`validar`) y **el archivo queda como estaba** |
| JSON cortado | Tira excepción |

**No cubre** el comando `recargar` ni la carga al arrancar: son una llamada a `cargarConfiguracion()` que atrapa la excepción. Se prueba a mano (ver [probar en el juego](../general/probar-en-el-juego.md)).

## Cosas que no son obvias

- **La igualdad con `POR_DEFECTO` es un `equals` de records**, que compara también las escalas y los botines anidados. Si falla, casi siempre es una clave que Gson no supo leer o escribir.
- **El aviso de la clave `inventada` sale en el log del test** ("no existe la clave \"inventada\""): es esperado, no es un error.
- **`gradlew runGameTest` también crea `build/run/gameTest/config/mobspoderosos.json`** al arrancar el servidor de tests, y el mod no pisa un archivo que ya existe. **Por eso `runGameTest` borra esa carpeta antes de arrancar** (un `doFirst` en `build.gradle`): si no, después de cambiar `POR_DEFECTO` los tests corrían con los valores de la corrida anterior.
- `lanza(() -> …)` es un helper del test con una interfaz que permite `IOException`: `Runnable` no la deja tirar.

## Ver también

- [Configuración](../mod/configuracion.md)
- [Tests: niveles](niveles.md)
