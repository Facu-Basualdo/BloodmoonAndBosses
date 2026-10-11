# Niveles

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **un zombi aparece con nivel pero sin la armadura que le tocaba**, **los evocadores de las mansiones salen sin nivel**, **un zombi ahogado se llama "Zombi [Nv 7]" y tiene la vida de un ahogado común**, **un aldeano curado queda para siempre con el nombre "Aldeano zombi [Nv 12]"**, o **la consola se llena con "Named entity … died"**.

## Se marca en finalizeSpawn y se aplica en ENTITY_LOAD

**El nivel se decide en dos momentos distintos, y no se puede juntar en uno.**

| Momento | Clase | Qué hace |
|---|---|---|
| `Mob.finalizeSpawn` **HEAD** | `MobMixin` | Si el `EntitySpawnReason` escala (`NivelDeMob.escala`), le pone al mob el attachment `PENDIENTE` |
| `ServerEntityEvents.ENTITY_LOAD` | `NivelDeMob.alCargar` | Saca `PENDIENTE`. Si no tiene `NIVEL` y `puedeTenerNivel`, calcula el nivel por posición y llama a `aparecer` (`aplicar` + `EquipoPorNivel.equipar`) |

- **`finalizeSpawn` es el único lugar que conoce el motivo del spawn.** Fabric no tiene un evento con el motivo, y `ENTITY_LOAD` no lo recibe.
- **El nivel no se puede aplicar en `finalizeSpawn`:** cada subclase pone su equipo en su propio `finalizeSpawn`, y `Zombie` lo hace **después** de llamar a `super` (verificado con `javap`). Un HEAD o un TAIL en `Mob.finalizeSpawn` correría antes de que el zombi tenga espada. Cuando llega `ENTITY_LOAD`, el equipo ya está puesto, y el mod lo completa y lo encanta ahí (ver [equipo](equipo.md)).
- **`PENDIENTE` es un attachment persistente, no un campo `@Unique`.** Los mobs de `STRUCTURE` y `CHUNK_GENERATION` se crean en un `WorldGenRegion`: `ProtoChunk.addEntity(Entity)` los guarda como NBT y, cuando el chunk está completo, el mob se recrea desde ese NBT. Un campo transitorio se perdería ahí (evocadores y vindicadores de las mansiones, la bruja de la cabaña, los shulkers del End). El attachment viaja en el NBT y `ENTITY_LOAD` lo encuentra al cargar el chunk.
- **`alCargar` saca `PENDIENTE` aunque el mob no escale** (un aldeano con `NATURAL`): si no, quedaría para siempre en su NBT.

Los attachments se registran en el inicializador estático de `NivelDeMob`, y `MobsPoderosos.onInitialize` llama a `NivelDeMob.inicializar()` (vacío) para cargar la clase. **Tienen que estar registrados antes de que se cargue el primer chunk:** si no, Fabric no sabe deserializar los que ya estaban guardados.

## Qué motivos escalan

| Motivo | ¿Escala? | Por qué |
|---|---|---|
| `NATURAL`, `CHUNK_GENERATION`, `STRUCTURE`, `PATROL`, `EVENT` (raids) | Sí | Son los spawns "del mundo" |
| `REINFORCEMENT`, `JOCKEY`, `MOB_SUMMONED`, `TRIGGERED` | Sí | Aparecen por otro mob, en el mismo lugar. Sin `JOCKEY`, la araña tendría nivel y el esqueleto que la monta no |
| `SPAWNER`, `TRIAL_SPAWNER` | Sólo con `escalarSpawners` (`false` por defecto) | Una granja de spawner sería una fábrica de loot |
| `SPAWN_ITEM_USE` (huevos), `DISPENSER`, `COMMAND`, `BREEDING`, `BUCKET`, `LOAD`, `DIMENSION_TRAVEL` | No | Para probar está `/mobspoderosos invocar` |
| `CONVERSION` | No por posición | Lo maneja `alConvertir` (abajo) |

**`puedeTenerNivel` = `instanceof Enemy` y no está en el tag `mobspoderosos:sin_nivel`** (dragón, wither, guardián anciano, warden). El guardián anciano aparece con `STRUCTURE`, así que sin el tag tendría nivel. El tag se chequea con `mob.is(TagKey)`: `EntityType` no tiene `is(tag)` en 26.3.

## Fórmula

`NivelDeZona.calcular(dimension, spawn, pos, config, random)` es una función pura, sin `ServerLevel`, para poder testearla con un `RandomSource` de seed fijo. El overload con `ServerLevel` saca el spawn de `MinecraftServer.getRespawnData().pos()`.

```
nivel = 1 + floor(distanciaHorizontal / bloquesPorNivel) + extraDimensión ± variación   →   clamp(1, nivelMaximo)
```

| Dimensión | Distancia desde | Extra |
|---|---|---|
| Overworld (y cualquier dimensión de otro mod) | El spawn del mundo | 0 |
| Nether | El spawn del mundo, con la posición **×8** (así cruzar un portal no cambia el nivel de golpe) | `extraNether` (20) |
| End | `(0, 0)`: el spawn del overworld no significa nada ahí | `extraEnd` (40) |

Con `bloquesPorNivel` 100 y `nivelMaximo` 100, el tope se alcanza a ~9900 bloques del spawn.

`/mobspoderosos zona` usa `config.conVariacion(0)`: muestra el nivel de la zona, no una tirada. Suma el del tiempo (es el mismo para todos) y sólo menciona la chance de campeón.

### Tiempo y campeones

**El overload con `ServerLevel` suma dos cosas a la zona, y vuelve a cortar en `nivelMaximo`:**

```
nivel = clamp(zona + porTiempo(días) + (campeón ? nivelesDeCampeon : 0), 1, nivelMaximo)
```

| Qué | Cómo | Por defecto |
|---|---|---|
| `NivelDeZona.porTiempo(dias, config)` | `min(dias / diasPorNivelExtra, nivelesPorTiempoMaximos)`; con `diasPorNivelExtra` 0, nada | +1 cada 5 días, tope +20 |
| `NivelDeZona.esCampeon(config, random)` | Una tirada con el random del mob | 5 %, +10 niveles |

- **Los días salen de `level.getOverworldClockTime() / SharedConstants.TICKS_PER_GAME_DAY`**, el mismo reloj que usa vanilla en `ServerLevel.getCurrentDifficultyAt` (verificado con `javap`). En 26.3 `Level` ya no tiene `getDayTime`: los relojes son `WorldClocks`. Es global, así que el Nether y el End usan los días del overworld. Dormir lo adelanta y `/time set` lo puede atrasar; `getGameTime` no se usa porque no cuenta las noches dormidas.
- **Las dos funciones son puras y aparte de `calcular(dimension, …)`**, que sigue siendo sólo la zona: así los tests de distancia no dependen del reloj del mundo de prueba ni de la tirada de campeón.
- **El nivel se fija al aparecer:** que pasen días no cambia a los mobs que ya están, igual que la config.
- **El campeón no tiene marca ni attachment propio:** es un mob de más nivel. Si tuviera un nombre distinto, `esNombreDelMod` (que compara con `nombre(tipo, nivel)`) dejaría de reconocerlo y las conversiones lo perderían.
- El tiempo también cuenta para el balance con Estadísticas RPG: a los 100 días, al lado del spawn aparecen mobs de nivel 21.

## Estadísticas

**Todos los modificadores usan el mismo `Identifier` (`mobspoderosos:nivel`) y `addOrReplacePermanentModifier`.** Aplicar dos veces reemplaza, no apila; y como son permanentes, vanilla los guarda en el NBT del mob. Al recargar, `alCargar` no los vuelve a aplicar porque ya tiene `NIVEL`.

| Atributo | Operación | Por nivel / tope (`Configuracion`) |
|---|---|---|
| `MAX_HEALTH` | `ADD_MULTIPLIED_BASE` | +8 % / +800 % |
| `ATTACK_DAMAGE` | `ADD_MULTIPLIED_BASE` | +4 % / +400 % |
| `ARMOR` | `ADD_VALUE` | +0,12 / +12 |
| `MOVEMENT_SPEED` | `ADD_MULTIPLIED_BASE` | +0,2 % / +20 % |
| `FOLLOW_RANGE` | `ADD_MULTIPLIED_BASE` | +0,5 % / +50 % |
| `KNOCKBACK_RESISTANCE` | `ADD_VALUE` | +0,005 / 0,5 |

**Todas las escalas llegan a su tope recién en el nivel 100, y están balanceadas contra Estadísticas RPG** (el mod hermano en el que el jugador compra vida, daño y armadura con experiencia, hasta 100 puntos por estadística). Con los topes antes de tiempo (el daño se quedaba quieto desde el nivel 25), alejarse dejaba de ser más peligroso y el jugador con puntos se volvía casi inmortal. El objetivo: con el equipo y los puntos que se esperan en cada zona, un zombi se mata en 5 o 6 golpes en todo el mundo y lo que aguanta el jugador baja de a poco hacia el 100. **Si se cambia una escala, hay que mirar los números del otro mod**, y al revés.

- **`ADD_MULTIPLIED_BASE` multiplica la base más todos los `ADD_VALUE`, incluido el del arma.** Un zombi con espada de hierro de nivel 25 pega `(3 + 5) × 2`, no `3 × 2 + 5`. Por eso el daño topea más bajo que la vida.
- `getAttribute` devuelve `null` para atributos que el mob no tiene (el creeper no tiene `ATTACK_DAMAGE`): `modificar` lo saltea.
- **`setHealth(getMaxHealth())` al final.** Sin eso, el mob aparece con la vida base aunque el máximo haya subido.
- **Creeper:** su explosión no usa `ATTACK_DAMAGE`. Desde `nivelRadioCreeper` (60), `CreeperAccessor` pone `explosionRadius` en 3 + `radioCreeperExtra` (4). Vanilla lo duplica si está cargado y lo guarda en el NBT como `ExplosionRadius`.
- El daño de las flechas tampoco usa `ATTACK_DAMAGE`: los esqueletos escalan por el encantamiento del arco (ver [equipo](equipo.md)).

## Nombre

`NivelDeMob.nombre(tipo, nivel)` = `tipo.getDescription().copy().append(" [Nv N]")`, con color por **quintos de `nivelMaximo`** (`nivel × 5 / nivelMaximo`: gris, verde, azul, violeta, dorado; con 100, dorado desde el 80). Es relativo para que, si se cambia `nivelMaximo`, los colores sigan repartidos en todo el rango y no queden todos dorados. La descripción es *translatable*: cada cliente la ve en su idioma, sin mod.

- **`nombreSiempreVisible` es `false` por defecto.** Un nombre siempre visible se dibuja a través de las paredes y funciona como un rayos X de mobs en las cuevas. Con `false` aparece al mirar el mob.
- **Ponerle nombre por código no evita el despawn.** `Mob.checkDespawn` mira `isPersistenceRequired()` (un campo que activa el name tag de vanilla), no `hasCustomName()`.
- **`esNombreDelMod(mob)`** compara el nombre actual con `nombre(tipo, nivel)`. Si un jugador le puso un name tag, da `false`: el nombre es del jugador y el mod no lo toca.

## Conversiones

**`ServerLivingEntityEvents.MOB_CONVERSION` dispara dentro de `Mob.convertTo`, justo antes de `addFreshEntity`** (es un `@ModifyArg` de Fabric sobre esa llamada). Así que `alConvertir` corre **antes** que el `ENTITY_LOAD` del mob nuevo, y cuando `alCargar` lo ve ya tiene `NIVEL` y no recalcula.

`ConversionType` copia `CUSTOM_NAME` al mob nuevo, pero no el nivel ni los modificadores (el mob nuevo se crea con `EntityType.create`, sin `finalizeSpawn`). `alConvertir(viejo, nuevo)`:

| Caso | Qué pasa |
|---|---|
| El viejo no tenía `NIVEL` | Nada (un slime de spawner se divide en hijos sin nivel) |
| El nuevo `puedeTenerNivel` (zombi → ahogado, slime → slimes chicos) | Copia el nivel y las estadísticas. Si el nombre era del mod, lo rehace con el tipo nuevo |
| El nuevo no es `Enemy` (aldeano zombi curado) | Sin nivel. Si el nombre era del mod, se borra |
| El jugador le había puesto un name tag | Su nombre se conserva; el nivel pasa igual |

Los slimes se dividen con `convertTo(…, TRIGGERED, …)` desde `AbstractCubeMob.remove`, así que pasan por el mismo camino.

## Log de muerte silenciado

**`LivingEntity.die` loguea `"Named entity {} died: {}"` para todo mob con nombre.** Con niveles, sería una línea en la consola por cada mob muerto. `LivingEntityMixin` envuelve ese `Logger.info` con un `@WrapWithCondition` (el de `mixinextras.injector.v2`) y lo saltea **sólo si `esNombreDelMod`**: los mobs con name tag de un jugador se siguen logueando como en vanilla.

## Ver también

- [Tests: niveles](../tests/niveles.md)
- [Botín y experiencia](botin.md)
- [Equipo](equipo.md)
- [Configuración](configuracion.md)
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
