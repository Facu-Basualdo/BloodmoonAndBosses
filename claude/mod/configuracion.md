# Configuración

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **un admin escribe mal un número y el servidor arranca con mobs sin nivel (o divide por cero)**, **una versión nueva del mod suma un valor y el archivo viejo lo deja en 0**, **un typo en una clave se ignora en silencio**, o **después de `/bloodmoon recargar` alguien espera que los mobs que ya estaban cambien de vida**.

## Dónde está y cuándo se lee

**`config/bloodmoonandbosses.json`** (`ArchivoDeConfiguracion.ruta()`, con `FabricLoader.getConfigDir()`). Se lee:

| Cuándo | Quién | Si falla |
|---|---|---|
| Al arrancar, **primero** en `BloodmoonAndBosses.onInitialize` | `cargarConfiguracion()` | Queda `Configuracion.POR_DEFECTO` y el error va a la consola |
| `/bloodmoon recargar` (op) | `cargarConfiguracion()` | Queda la configuración anterior; el error va al chat de quien lo corrió y a la consola |

`BloodmoonAndBosses.config` es un campo `volatile` que se reemplaza entero: nadie ve una configuración a medio cargar.

## Lectura: por defecto + archivo, y se reescribe

`ArchivoDeConfiguracion.leer(ruta)`:

1. Serializa `POR_DEFECTO` con Gson a un `JsonObject` (Gson ≥ 2.10 sabe serializar records).
2. Si el archivo existe, lo **mezcla encima** (`mezclar`), recursivo en los objetos: `{"vida": {"tope": 2.0}}` cambia sólo el tope y deja `porNivel` por defecto.
3. `GSON.fromJson(completo, Configuracion.class)` y `config.validar()`.
4. **Recién ahí** reescribe el archivo completo.

**Por qué mezclar y no `fromJson` directo:** con records, Gson deja en 0 o `null` lo que falta en el JSON. Un archivo de una versión anterior no tendría `nivelesPorTiradaExtra`, que quedaría en 0, y `tiradasExtra` dividiría por cero.

- **Se reescribe para que aparezcan las claves nuevas** de una versión nueva del mod. El costo es que se pierde el formato del admin y las claves desconocidas.
- **Una clave que no existe se ignora con un `warn`** que dice cuál es (con prefijo para las anidadas, `vida.tpoe`). Sin el aviso, un typo no tendría ningún efecto y nadie se daría cuenta.
- **Si algo falla (JSON roto, tipo equivocado, `validar`), se tira la excepción antes de escribir: el archivo del admin queda como estaba** para que lo corrija.

## Validación

**`Configuracion.validar()` rechaza lo que rompería el mod**, no lo que sería raro de balance:

| Regla | Qué rompería |
|---|---|
| `bloquesPorNivel ≥ 1`, `nivelesPorMaterialDeArmadura ≥ 1`, `nivelesPorTiradaExtra ≥ 1` | División por cero |
| `nivelMaximo ≥ 1` | `Mth.clamp(nivel, 1, 0)` da 0, y el resto del código lee el nivel 0 como "sin nivel" (`NivelDeMob.nivel`, `experiencia`) |
| `variacion ≥ 0`, `tiradasExtraMaximas ≥ 0` | Nada se rompe (el código los trata como 0), pero un negativo es seguro un error del admin |
| Ninguna escala ni botín `null` | `NullPointerException` en pleno juego (pasa si el JSON pone `"vida": null`) |
| `nivelMinimo ≥ 1` en cada botín | Con 0, los mobs sin nivel también tirarían las tablas raras |

## Qué cambia al recargar

**Lo que se calcula al aparecer no cambia para los mobs que ya están; lo que se calcula al morir cambia enseguida.**

| Se usa al aparecer (queda en el mob) | Se usa al morir (vale enseguida) |
|---|---|
| Nivel y fórmula (`bloquesPorNivel`, extras, `variacion`, `escalarSpawners`) | `nivelesPorTiradaExtra`, `tiradasExtraMaximas`, `chanceTiradaExtra` |
| Estadísticas (son modificadores permanentes en el NBT) | `botinRaro`, `botinEpico`, `botinLegendario` |
| Nombre y `nombreSiempreVisible` | `experienciaPorNivel` |
| | `vidaEnElNombre`, al próximo cambio de vida de cada mob |
| Radio del creeper, armadura, encantamientos, chances de soltar el equipo | |

El mensaje de éxito de `recargar` lo dice, para que nadie lo reporte como bug.

## Agregar un valor nuevo

1. Componente nuevo en el record `Configuracion` y su valor en `POR_DEFECTO`, con un comentario si el número tiene un porqué.
2. **Sumarlo también en `conVariacion` y `conExtras`**, que llaman al constructor canónico con todos los campos: si no, no compila. Un valor de un evento va dentro de su record (`Afijos`, `Estampida`, `LunaDeSangre`, `JefeDeZona`), que el JSON escribe como un objeto anidado y `mezclar` completa clave por clave.
3. Si un valor imposible rompe algo, la regla en `validar()`.
4. La fila en la tabla de configuración del `README.md`.

El nombre del componente es la clave del JSON: **renombrarlo rompe los archivos que ya existen** (el valor viejo se ignora con un aviso y vuelve al por defecto).

## Ver también

- [Niveles](niveles.md)
- [Equipo](equipo.md)
- [Botín y experiencia](botin.md)
- [Eventos y afijos](eventos.md)
- [Tests: configuración](../tests/configuracion.md)
- [Pantalla de configuración](pantalla-de-configuracion.md): editar este archivo desde Mod Menu.
