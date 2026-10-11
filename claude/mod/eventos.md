# Eventos y afijos

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **un invocador llama refuerzos que también son campeones invocadores y la cueva se llena**, **la estampida aparece arriba de un techo de barreras o no aparece nunca en un test**, **los zombis de una estampida se queman al amanecer y el jugador se lleva el premio sin pelear**, **una luna de sangre en pleno día durante una tormenta**, **la barra de un jefe queda colgada en la pantalla después de un reinicio**, o **un campeón con afijo pierde el nombre del mod al convertirse**.

Todo es del lado del servidor: barra de jefe (`ServerBossEvent`), títulos (`ClientboundSetTitleTextPacket` y compañía), sonidos y nombres. Un cliente vanilla lo ve todo.

## Los mobs de un evento: invocarConNivel

**Todo mob que trae un evento sale de `NivelDeMob.invocarConNivel(tipo, level, pos, motivo, nivel, conAfijo)`, nunca de `tipo.spawn` + `ENTITY_LOAD`.**

1. `tipo.create(level, motivo)` y `snapTo`.
2. `finalizeSpawn`: el equipo de vanilla, y `PENDIENTE` si el motivo escala.
3. `aparecer(mob, nivel, afijo)`: `NIVEL` queda puesto **antes** de entrar al mundo.
4. `addFreshEntityWithPassengers` → `alCargar` le borra `PENDIENTE` y, como ya tiene `NIVEL`, no hace nada más. Es la misma rama que usan las conversiones.

| Por qué así | Qué pasaría con `spawn` + `alCargar` |
|---|---|
| El nivel lo decide el evento (zona + 20 para el jefe, el del mob original para el compañero de la luna) | `alCargar` calcularía el de la zona |
| **No tira campeón:** un evento nunca trae campeones sin querer | Los refuerzos del invocador podrían ser invocadores, y sus refuerzos también: una cadena sin fin |
| El nivel se corta en `nivelMaximo` adentro | — |

`Afijo.alAzar` elige entre `Afijo.posibles(mob)`, y para eso necesita el mob creado: por eso el afijo se pasa como `boolean conAfijo` y no como un `Afijo`.

## Afijos

**El afijo es un attachment persistente (`Afijo.AFIJO`, codec `StringRepresentable.fromEnum`) y forma parte del nombre del mod:** `nombre(tipo, nivel, afijo)` da "Zombi [Nv 27] [Vampiro]", y `esNombreDelMod` compara con eso. Si el afijo no estuviera en esa comparación, el nombre de un campeón no se reconocería como del mod: se loguearía su muerte y las conversiones no lo rehacerían.

- **Entre corchetes y no como adjetivo**, por la concordancia: "Bruja [Nv 4] [Vampiro]".
- **`aparecer` pone el attachment antes de `aplicar`**, porque el nombre y el modificador de Veloz lo leen de ahí.
- **`alConvertir` copia el afijo antes de `aplicarEstadisticas`**: el ahogado de un zombi veloz es veloz.
- **El campeón natural lo sortea en `NivelDeMob.alCargar`** con `Afijo.paraCampeon` (`afijos.chance`, por defecto 1). `NivelDeZona.calcular(ServerLevel, …)` ya no suma el campeón: lo suma `alCargar`, porque los eventos no tiran campeón.

| Afijo | Dónde | Detalle |
|---|---|---|
| Veloz | `Afijo.aplicarModificadores`, llamado desde `aplicarEstadisticas` | Modificador `mobspoderosos:afijo` en `MOVEMENT_SPEED`. Desde ahí, las conversiones lo rehacen solas |
| Vampiro | `ServerLivingEntityEvents.AFTER_DAMAGE` | `source.getEntity()` y no `getDirectEntity()`: en una flecha es el esqueleto. Cura `danioRecibido × curacionVampiro`. No le toca al creeper |
| Explosivo | `AFTER_DEATH` + `Eventos.programar` | La explosión va `ticksDeMecha` después, con el sonido de la mecha. `ExplosionInteraction.NONE`: no rompe bloques |
| Invocador | `AFTER_DAMAGE` sobre la víctima | `INVOCACIONES` es persistente (si no, guardar y cargar el chunk le devolvería todas). `ULTIMA_INVOCACION` no lo es: la espera se reinicia, y no importa. Tag `no_invocan` (vex: sin evocador no desaparece nunca) |

## El reloj: Eventos

`Eventos.registrar()` engancha un `END_SERVER_TICK`:

| Cada | Qué |
|---|---|
| tick | Las tareas de `programar` que vencieron (la explosión, el compañero de la luna). Se corren sólo si su `ServerLevel` sigue cargado |
| segundo | `LunaDeSangre.actualizar`, `Estampida.actualizarTodas`, `JefeDeZona.actualizarTodos` |
| minuto | `sortear` de la estampida y del jefe, por jugador (las chances de la config son por minuto) |

**`SERVER_STOPPED` → `Eventos.limpiar()`:** en singleplayer el mismo proceso abre otro mundo, y lo que quedó en los mapas estáticos es del anterior.

## lugarCerca

`Eventos.lugarCerca(level, centro, min, max, angulo, tipo, superficie, random)` prueba 12 puntos de un anillo. Con `angulo`, todos dentro de un abanico de 60° (la estampida llega de un lado).

- **`superficie`**: el `MOTION_BLOCKING_NO_LEAVES` del heightmap. Si no, busca un piso a ±8 de la altura del centro (cuevas, minas).
- **Un lugar sirve si `SpawnPlacements.isSpawnPositionOk` y `noCollision(tipo.getSpawnAABB(...))`.** `isSpawnPositionOk` es la regla de vanilla: **no deja aparecer sobre barreras ni vidrio**, ni a un zombi en el agua. Por eso los tests ponen un piso de piedra (ver [tests: eventos](../tests/eventos.md)).
- Sólo chunks cargados (`isLoaded`).

## Estampida

**Vive en memoria (`ACTIVAS`, por UUID del jugador).** Si el servidor se reinicia, los mobs quedan como mobs comunes con nivel. Por eso sus modificadores (velocidad y `FOLLOW_RANGE` + 2 × `distanciaMaxima`, para que un esqueleto, que sigue a 16 bloques, no pierda al jugador) son **transitorios**: no se guardan.

- **Sorteo:** overworld, `NivelDeZona.esDeNoche`, `canSeeSky` en el jugador, nivel de la zona ≥ `nivelMinimo`, y la espera. La espera es el attachment `ULTIMA` del jugador, con `persistent` y **`copyOnDeath`**: sin `copyOnDeath`, morirse borraría la espera. Se cuenta con `getGameTime`, así que dormir no la acorta.
- **Al aire libre aparecen sobre la superficie; bajo techo, a la altura del jugador** (`superficie = canSeeSky(centro)`). Si no entra la mitad, se descartan todos y no empieza.
- **El primero es el líder:** + `nivelesDeCampeon` y con afijo.
- **Premio sólo si los jugadores mataron al menos la mitad.** Cuenta un mob como muerto si `getRemovalReason() == KILLED` **y** `getLastHurtByPlayer() != null` cuando se lo saca de la lista (un segundo después, dentro de los 100 ticks de memoria). Sin lo segundo, los zombis que se queman al amanecer darían el premio.
- **El premio sale de `BotinPorNivel.mejorBotin(nivel, RARO)` con el jugador como origen** (`jugador.dropFromLootTable(..., consumer)`). Las tablas son de tipo `entity` y no tienen condiciones sobre quién murió. Va al inventario con `placeItemBackInInventory(item, Prediction.SERVER_ONLY)`: lo que no entra cae a los pies.

## Luna de sangre

**El estado son dos attachments persistentes del `ServerLevel` del overworld:** `NOCHE_ACTIVA` (el día de la luna) y `NOCHE_SORTEADA` (el último día sorteado). Sobreviven al reinicio, y la noche se sortea una sola vez: reconectarse no da otra tirada.

- **De noche es `NivelDeZona.esDeNoche`: la hora del reloj del overworld entre 13000 y 23000.** No `isDarkOutside`, que también da `true` en una tormenta de día.
- **La fase de la luna es `level.environmentAttributes().getDimensionValue(EnvironmentAttributes.MOON_PHASE)`.** En 26.3 no hay `getMoonPhase`: la luna es un *timeline* (`data/minecraft/timeline/moon.json`, período 192000) sobre el reloj del overworld. Leerla así respeta un datapack que la cambie.
- **Los niveles extra se suman en `NivelDeZona.calcular(ServerLevel, …)`**, así que valen para todo lo que aparece en el overworld esa noche, también para los eventos.
- **El compañero se pide desde `MobMixin` sólo con `NATURAL`** (en una estructura o una patrulla, duplicar cambiaría la estructura) y se crea **un tick después** con `Eventos.programar`: en `finalizeSpawn` el mob todavía no está en el mundo ni tiene nivel. Sale con motivo `EVENT`, así no trae otro compañero.
- `/time set day` o dormir la terminan en el siguiente segundo.

## Jefe de zona

**No depende de memoria:** el attachment persistente `JEFE` lo marca, y `JefeDeZona.alCargar` (`ENTITY_LOAD`) lo vuelve a seguir con una barra nueva. `ENTITY_UNLOAD`, que también corre al morir o al desaparecer, le saca la barra a todos. Así la barra no queda colgada ni después de un reinicio ni cuando el chunk se descarga.

- **`setPersistenceRequired`:** si no, a más de 128 bloques del jugador desaparece al instante. A cambio, **se va solo** si nadie está a menos de `radioDeBarra` durante `segundosAbandonado` (`discard`). Las escoltas son mobs comunes.
- **Vida:** un modificador `mobspoderosos:jefe` con `ADD_MULTIPLIED_TOTAL` (multiplica también lo que sumó el nivel), y `setHealth(getMaxHealth())` después.
- **Aparece a la altura del jugador** (`superficie = false`): en una mina tiene que aparecer en la mina. Tipos por dimensión: tags `jefe_de_zona/overworld`, `nether` y `end`.
- **No aparece si ya hay un jefe seguido a menos de 128 bloques** del jugador.
- **El botín asegurado va en el mismo `@WrapOperation` que el botín raro** (`BotinPorNivel.soltarBotinDeJefe`), así que respeta `mobDrops` y exige que lo haya matado un jugador. Ver [botín](botin.md).
- **La barra y los mensajes usan `NivelDeMob.nombreSinVida`:** la barra ya muestra la vida, y "❤ 120/180" ahí estaría repetido.

## Ver también

- [Niveles](niveles.md)
- [Botín y experiencia](botin.md)
- [Configuración](configuracion.md)
- [Tests: eventos](../tests/eventos.md)
