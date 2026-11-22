# Bloodmoon & Bosses — mod de Minecraft

**El id es `bloodmoonandbosses` desde la 0.11.0 (antes `mobspoderosos`)**, y el comando es `/bloodmoon`. Lo guardado con el id viejo (el nivel de cada mob, la config, la luna) se pasa solo: ver [migración del id](claude/mod/migracion-de-id.md). No lo vuelvas a cambiar sin una migración. Además, **Mythic Loot lee por id `bloodmoonandbosses:nivel`, `bloodmoonandbosses:jefe` y `bloodmoonandbosses:afijo`** (sin depender de las clases de este mod): cambiarles el id o el tipo de valor lo rompe.

Mod de Fabric para **Minecraft Java 1.21.11** (backport de la rama `main`, que es 26.3) que hace aparecer a los mobs hostiles con **niveles**: cuanto más lejos del spawn del mundo y más días tiene el mundo, más vida, daño y armadura tienen, y mejor loot dan; además, algunos aparecen como campeones con niveles de más. El nivel y la vida se ven en el nombre ("Zombi [Nv 7] ❤ 34/40"). Java 21 (Gradle corre con el 25), Gradle 9.7 + Fabric Loom Remap 1.18.4 con los mappings oficiales de Mojang, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11. Toda la lógica corre en el servidor: los jugadores no necesitan instalar nada. Con el mod también en el cliente, la luna de sangre se ve en el cielo y jefes y campeones se tiñen, y con Mod Menu la config se edita desde el juego (`src/client`, opcional).

## Diferencias con 26.3 (rama `main`)

Para portar un cambio de `main`, aplicalo, compilá y corré `runGameTest`. Lo que cambia en esta rama:

- **Build:** `net.fabricmc.fabric-loom-remap` con `mappings loom.officialMojangMappings()` (1.21.11 viene ofuscado), dependencias con `modImplementation`/`modCompileOnly`, Java 21 y `compatibilityLevel` `JAVA_21` en los mixins. Gradle sigue corriendo con el JDK 25.
- **Renombres de 26.x:** `ContainerInput` → `ClickType`, `PayloadTypeRegistry.playS2C()/playC2S()`, `EntityType` en vez de `EntityTypes`, `Minecraft.screen`/`setScreen` en vez de `gui`, `GuiGraphics`/`render…` en vez de `GuiGraphicsExtractor`/`extract…`, `KeyBindingHelper`, `InputConstants.Type.KEYSYM`, `Items.GRAY_STAINED_GLASS_PANE` y similares.
- **SavedData:** el id es un `String` que se usa de nombre de archivo (`data/<mod>_<nombre>.dat`) y el almacenamiento es el del overworld. La migración de datos de mundo del id viejo se sacó: ningún mundo de 1.21.x se guardó con él.
- **Gametests de cliente:** la API de 1.21.11 no espera paquetes (`waitForClientboundPackets`): se esperan unos ticks.
- **Tags de entidad:** `mob.getType().is(tag)` en vez de `mob.is(tag)`. `ServerBossEvent` sin UUID, `PlayerTeam.setColor(ChatFormatting)`, hora con `getDayTime()`.
- **Cliente:** la luna se agranda en el `Matrix4fStack.scale(20, 1, 20)` y se tiñe en `DynamicUniforms.writeTransform`; el tinte de mobs va en `extractRenderState`. Jade 21 sin `ConfigIcon`.

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
| Mod | `src/main/` y `src/client/` (sólo cliente) | Java 25 · Fabric API · Mixins (SpongePowered + MixinExtras) · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
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
| Cliente opcional: cielo de la luna de sangre (paquete del servidor, mixins de cielo y luna) y tinte de jefes y campeones (attachments sincronizados) | [cliente](claude/mod/cliente.md) | [cliente](claude/tests/cliente.md) |
| Jade (opcional en el cliente: los mobs se ven hasta 48 bloques) | [jade](claude/mod/jade.md) | — (sólo cliente: se prueba a mano) |
| Pantalla de configuración con Mod Menu, opcional (0.19.0) | [pantalla-de-configuracion](claude/mod/pantalla-de-configuracion.md) | [pantalla-de-configuracion](claude/tests/pantalla-de-configuracion.md) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`**: es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compila no alcanza, los mixins recién se aplican en runtime. Si tocaste `src/client`, también `gradlew runClientGameTest` (abre una ventana unos 30 segundos).
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`**: minor (`0.1.0` → `0.2.0`) para una feature nueva, patch (`0.1.0` → `0.1.1`) para un arreglo.
- **Con cada versión nueva, agregá su sección `## <versión>` en `CHANGELOG.md`**, en inglés y pensada para quien juega: es el changelog que `gradlew publishMods` sube a CurseForge, y sin ella la publicación falla. Subir es sólo `gradlew publishMods` con el token de CurseForge en `~/.gradle/gradle.properties` (`curseforgeToken`) o en `CURSEFORGE_TOKEN`; sin token corre en seco y no sube nada. Publicá sólo si te lo piden.
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
