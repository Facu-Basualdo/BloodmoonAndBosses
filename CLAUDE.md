# Bloodmoon & Bosses — mod de Minecraft

**El id es `bloodmoonandbosses` desde la 0.11.0 (antes `mobspoderosos`)**, y el comando es `/bloodmoon`. Lo guardado con el id viejo (el nivel de cada mob, la config, la luna) se pasa solo: ver [migración del id](claude/mod/migracion-de-id.md). No lo vuelvas a cambiar sin una migración. Además, **Mythic Loot lee por id `bloodmoonandbosses:nivel`, `bloodmoonandbosses:jefe` y `bloodmoonandbosses:afijo`** (sin depender de las clases de este mod): cambiarles el id o el tipo de valor lo rompe.

Mod de Fabric para **Minecraft Java 26.3** que hace aparecer a los mobs hostiles con **niveles**: cuanto más lejos del spawn del mundo y más días tiene el mundo, más vida, daño y armadura tienen, y mejor loot dan; además, algunos aparecen como campeones con niveles de más. El nivel y la vida se ven en el nombre ("Zombi [Nv 7] ❤ 34/40"). Java 25, Gradle 9.7 + Fabric Loom 1.18, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3. Toda la lógica corre en el servidor: los jugadores no necesitan instalar nada.

## Cómo obtener contexto

1. Ubicá el **área** en la tabla de abajo.
2. Abrí su `INDICE.md` y leé las descripciones: muchas veces la respuesta ya está ahí.
3. Abrí **sólo** la nota que corresponde. No leas un área entera.

Si no sabés en qué nota está algo, buscá por contenido antes de abrir archivos:

```bash
grep -ril "<palabra clave>" claude/
```

El mismo tema en distintas áreas usa el mismo nombre de archivo (`claude/mod/niveles.md` ↔ `claude/tests/niveles.md`), así que el grep devuelve el clúster completo.

## Áreas

| Área | Carpeta | Stack | Índice |
|---|---|---|---|
| Mod | `src/main/` | Java 25 · Fabric API · Mixins (SpongePowered + MixinExtras) · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
| Tests | `src/gametest/` | Fabric GameTest API · servidor sin ventana | [claude/tests/INDICE.md](claude/tests/INDICE.md) |

## Notas transversales

| Nota | De qué trata |
|---|---|
| [Entorno Java y Gradle](claude/general/entorno-java-gradle.md) | `~/.gradle/gradle.properties` fuerza JDK 17 (React Native) y gana sobre el proyecto · Java 25 se fija con `gradle/gradle-daemon-jvm.properties` + `org.gradle.java.installations.paths` · sin eso Loom falla con "requires JVM runtime version 25" |
| [Minecraft 26.x sin ofuscar](claude/general/minecraft-26-sin-ofuscar.md) | Desde 26.1 se usan los nombres de Mojang (Yarn está muerto: los tutoriales viejos tienen otros nombres) · antes de escribir un mixin, verificá firmas con `javap` sobre el jar de `.gradle/loom-cache` · las constantes de tipos de entidad están en `EntityTypes`, no en `EntityType` |
| [Probar en el juego](claude/general/probar-en-el-juego.md) | `runServer` necesita `eula=true` y `online-mode=false` en `run/` · `/bloodmoon invocar <entidad> <nivel>` evita caminar miles de bloques · probá también con un cliente vanilla: el mod no tiene que hacer falta del lado del cliente |

## Features que cruzan áreas

| Feature | Mod | Tests |
|---|---|---|
| Idioma: por jugador y traducible con respaldo; nombres reconocidos en cualquier idioma | [idioma](claude/mod/idioma.md) | [niveles](claude/tests/niveles.md) |
| Niveles (cuándo se asignan, fórmula, tiempo y campeones, estadísticas, nombre y vida en el nombre, conversiones) | [niveles](claude/mod/niveles.md) | [niveles](claude/tests/niveles.md) |
| Botín y experiencia (tiradas extra, tags `sin_tiradas_extra` y `tiradas_extra_por_chance`, botín raro, experiencia por nivel) | [botin](claude/mod/botin.md) | [botin](claude/tests/botin.md) |
| Equipo (armadura por rango, encantamientos, chance de soltar, tags `usan_equipo` y `equipo_valioso`) | [equipo](claude/mod/equipo.md) | [equipo](claude/tests/equipo.md) |
| Configuración (`config/bloodmoonandbosses.json`, `/bloodmoon recargar`) | [configuracion](claude/mod/configuracion.md) | [configuracion](claude/tests/configuracion.md) |
| Migración del id `mobspoderosos` → `bloodmoonandbosses` (0.11.0) | [migracion-de-id](claude/mod/migracion-de-id.md) | [niveles](claude/tests/niveles.md) |
| Eventos y afijos (8 afijos de campeón, estampida, luna de sangre, jefe de zona con furia y habilidades, `/bloodmoon evento`) | [eventos](claude/mod/eventos.md) | [eventos](claude/tests/eventos.md) |
| Jade (opcional en el cliente: los mobs se ven hasta 48 bloques) | [jade](claude/mod/jade.md) | — (sólo cliente: se prueba a mano) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`**: es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compila no alcanza, los mixins recién se aplican en runtime.
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`**: minor (`0.1.0` → `0.2.0`) para una feature nueva, patch (`0.1.0` → `0.1.1`) para un arreglo.
- **No commitees ni pushees salvo que te lo pidan.**
- Mensajes, comentarios y docs en **español rioplatense** (vos), como el resto del proyecto.

## Convenciones de código

- Paquete `com.bloodmoonandbosses`; mixins en `com.bloodmoonandbosses.mixin`, registrados en `src/main/resources/bloodmoonandbosses.mixins.json`.
- **Métodos y accessors agregados por mixins llevan prefijo `bloodmoonandbosses$`** para no chocar con otros mods.
- **Ningún número de balance escrito a mano en el código:** van en `Configuracion`.
- Indentación con tabs, imports agrupados: java/terceros → `net.fabricmc` → `net.minecraft`.
- Comentarios sólo para el porqué (qué rompería vanilla si no estuviera), no para parafrasear.

## Qué NO va en el grafo

Planes, investigaciones, auditorías y borradores van en `docs/` y se leen a demanda; `claude/` es sólo contexto permanente. El plan del mod por fases está en [docs/plan.md](docs/plan.md).
