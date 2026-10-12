# Eventos y afijos

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **el jefe no se distingue de sus escoltas**, **un jefe muerto queda para siempre en un equipo del scoreboard**, **un invocador llama refuerzos que también son campeones invocadores y la cueva se llena**, **la estampida aparece arriba de un techo de barreras o no aparece nunca en un test**, **los zombis de una estampida se queman al amanecer y el jugador se lleva el premio sin pelear**, **una luna de sangre en pleno día durante una tormenta**, **la barra de un jefe queda colgada en la pantalla después de un reinicio**, o **un campeón con afijo pierde el nombre del mod al convertirse**.

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
| Veloz | `Afijo.aplicarModificadores`, llamado desde `aplicarEstadisticas` | Modificador `bloodmoonandbosses:afijo` en `MOVEMENT_SPEED`. Desde ahí, las conversiones lo rehacen solas |
| Vampiro | `ServerLivingEntityEvents.AFTER_DAMAGE` | `source.getEntity()` y no `getDirectEntity()`: en una flecha es el esqueleto. Cura `danioRecibido × curacionVampiro`. No le toca al creeper |
| Explosivo | `AFTER_DEATH` + `Eventos.programar` | La explosión va `ticksDeMecha` después, con el sonido de la mecha. `ExplosionInteraction.NONE`: no rompe bloques |
| Invocador | `AFTER_DAMAGE` sobre la víctima | `INVOCACIONES` es persistente (si no, guardar y cargar el chunk le devolvería todas). `ULTIMA_INVOCACION` no lo es: la espera se reinicia, y no importa. Tag `no_invocan` (vex: sin evocador no desaparece nunca) |
| Blindado | `Afijo.aplicarModificadores` | `bloodmoonandbosses:afijo` en `ARMOR` (`armaduraBlindado`) y `KNOCKBACK_RESISTANCE` (`empujeBlindado`), `ADD_VALUE`. `modificar` con 0 **saca** el modificador: cada afijo pone los suyos y saca los de los otros |
| Venenoso | `AFTER_DAMAGE` sobre el atacante | `POISON` de `nivelVeneno` (1 = I, el amplificador es `nivel − 1`) por `segundosVeneno`, con el atacante como fuente. Con `getEntity()`, también un esqueleto con flechas o un creeper con su explosión |
| Congelante | `AFTER_DAMAGE` sobre el atacante | `SLOWNESS` como el veneno, más `setTicksFrozen` para la escarcha de la nieve polvo en pantalla. **Por debajo de `getTicksRequiredToFreeze()`** (140): con el máximo, vanilla lastima por frío. Fuera de la nieve baja 2 por tick, así que `segundosCongelante × 40` dura esos segundos |
| Teletransportador | `AFTER_DAMAGE` sobre la víctima | Con `chanceTeletransporte` y una espera de `segundosEntreTeletransportes` (`ULTIMO_TELETRANSPORTE`, en memoria), salta a 2–4 bloques **del que le pegó** con `lugarCerca` y lo toma de objetivo: alejarse o tirarle flechas no alcanza. Sin un atacante vivo (lava, caída) no salta |

### Partículas

**Cada `ticksEntreParticulas` (10), `Afijo.mostrarParticulas` le pone a cada campeón la partícula de su afijo** (`Afijo.particula()`): así se reconoce de lejos o en una cueva, antes de leer el nombre. Se apagan con `afijos.particulas`.

- **Se buscan alrededor de cada jugador** (`conParticulas`, 32 bloques) y no en todo el mundo: `sendParticles` sin forzar no le llega a un cliente a más de 32 bloques, así que lejos de todos nadie las vería. Es un `Set`: con dos jugadores juntos, el mob iría dos veces.
- **Los invisibles no** (una poción de invisibilidad): las partículas lo delatarían.
- Veloz va a los pies y explosivo arriba de la cabeza (humo + `SMALL_FLAME`, una mecha); el resto, alrededor del cuerpo. Son partículas de vanilla: un cliente sin el mod las ve.
- Los jefes también tienen afijo, así que también las sueltan.

## El reloj: Eventos

`Eventos.registrar()` engancha un `END_SERVER_TICK`:

| Cada | Qué |
|---|---|
| `ticksEntreParticulas` | `Afijo.mostrarParticulas` |
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
- **Vida:** un modificador `bloodmoonandbosses:jefe` con `ADD_MULTIPLIED_TOTAL` (multiplica también lo que sumó el nivel), y `setHealth(getMaxHealth())` después.
- **Aparece a la altura del jugador** (`superficie = false`): en una mina tiene que aparecer en la mina. Tipos por dimensión: tags `jefe_de_zona/overworld`, `nether` y `end`.
- **No aparece si ya hay un jefe seguido a menos de 128 bloques** del jugador.
- **El botín asegurado va en el mismo `@WrapOperation` que el botín raro** (`BotinPorNivel.soltarBotinDeJefe`), así que respeta `mobDrops` y exige que lo haya matado un jugador. Ver [botín](botin.md).
- **La barra y los mensajes usan `NivelDeMob.nombreSinVida`:** la barra ya muestra la vida, y "❤ 120/180" ahí estaría repetido.

### Contorno

**El jefe brilla con el contorno de la flecha espectral** (`setGlowingTag`), que un cliente vanilla dibuja a través de las paredes. **El color sale del equipo del scoreboard** en el que está el mob: `JefeDeZona.resaltar` lo pone en `bloodmoon.jefe` (`TeamColor.DARK_PURPLE`) o, con furia, en `bloodmoon.furia` (`RED`), los mismos colores que la barra. Los equipos se crean la primera vez y quedan en el `scoreboard.dat` del mundo.

- **En 26.3 el color de un equipo es `Optional<TeamColor>`**, no `ChatFormatting`.
- **`resaltar` corre al seguirlo y en cada `actualizarTodos`:** así sigue a la furia y a un `/bloodmoon recargar` que cambie `contorno`. Sólo hace `addPlayerToTeam` si cambia de equipo: cada alta se les manda a todos los jugadores.
- **No hace falta sacarlo del equipo al morir:** cuando un mob se va del mundo, vanilla llama a `Scoreboard.entityRemoved`, que lo saca de su equipo (lo prueba `contornoDelJefe`). Con `contorno` en `false` sí se lo saca, pero sólo de los dos equipos del mod: si un datapack lo puso en otro, no se toca.
- **Estar en el mismo equipo los hace aliados** (`isAlliedTo`): dos jefes no se pelean entre sí. Las escoltas no entran al equipo.
- El nombre del jefe también toma el color del equipo donde no tiene uno propio.
- Ver a través de las paredes no delata nada: el jefe ya se anuncia al aparecer y tiene barra.

### Furia y habilidades

Todo corre en `actualizarTodos` (cada segundo), sólo para los jefes seguidos.

| Mecánica | Cuándo | Qué hace |
|---|---|---|
| Furia (`enfurecer`) | La vida baja de `vidaFuria` × el máximo (0,5) | Attachment persistente `FURIA`; `bloodmoonandbosses:furia` con `ADD_MULTIPLIED_TOTAL` en `MOVEMENT_SPEED` y `ATTACK_DAMAGE`; `escoltasFuria` escoltas con el nivel de la zona contra su objetivo; rugido de devastador; la barra pasa a **roja** y dice "· Furia" |
| Onda de choque | Hay alguien a menos de `radioOnda` | Lastima `ATTACK_DAMAGE × danioOnda` (`mobAttack`) y empuja para afuera con `fuerzaOnda` |
| Tirón | Hay alguien entre `radioOnda` y `radioTiron` (y dentro de `radioDeBarra`) | Lo trae hasta ~3 bloques del jefe y le da lentitud II 2 segundos |

- **Cada `segundosEntreHabilidades` (10) elige una** con `elegirHabilidad`: si hay jugadores en las dos franjas, al azar; si no hay nadie a tiro, nada. **Avisa un segundo antes** (sonido, partículas y un mensaje en la barra de acción) y la hace con `Eventos.programar(20)`, con los jugadores que estén a tiro en ese momento: así se puede esquivar. Si el jefe murió o se fue en ese segundo, no pasa nada.
- **FURIA es persistente** para que un jefe que se enfureció y vuelve con su chunk no llame otra tanda de escoltas. Los modificadores son permanentes con id fijo, como los del nivel: viajan en el NBT y aplicar dos veces no apila.
- **`proximaHabilidad` vive en `Seguido`, en memoria:** después de un reinicio el jefe espera una vuelta entera antes de la primera. No vale la pena guardarlo.
- **Empujar a un jugador pide `needsSync = true`** (el `hurtMarked` de antes): el cliente es dueño de su movimiento, y sin el paquete de velocidad el empujón sólo pasa en el servidor y el jugador "rebota" a donde estaba.
- **El tirón calcula la velocidad con el roce:** en el aire la velocidad horizontal se multiplica por ~0,91 por tick, así que una velocidad `v` recorre unas `11 v`. Con `(distancia − 3) / 11`, tope 2,5, llega a unos bloques del jefe sin caerle encima.
- **Sólo cuentan los jugadores vivos, en supervivencia o aventura** (`jugadoresEntre`): un espectador o alguien en creativo no dispara ni recibe habilidades.

## Ver también

- [Niveles](niveles.md)
- [Botín y experiencia](botin.md)
- [Configuración](configuracion.md)
- [Tests: eventos](../tests/eventos.md)
