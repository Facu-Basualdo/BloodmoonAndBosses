# Plan — Mod "Mobs Poderosos" (niveles por distancia, mejor loot)

## Contexto

Querés un segundo mod de Fabric, aparte de Inventario Compartido, donde los mobs hostiles aparezcan con **niveles**. Cuanto más lejos del spawn del mundo, más nivel. Un mob de más nivel es más fuerte y da **mejor loot**. Decisiones que tomaste:

- **Nivel** = distancia al spawn del mundo (con un extra en el Nether y el End).
- **Visual** = un nombre sobre la cabeza ("Zombi [Nv 7]", con color por rango). Es todo del servidor y **no hace falta mod en el cliente**.
- **Loot** = más tiradas de la tabla de vanilla + más experiencia, equipo encantado que se puede soltar, e ítems raros por rango de nivel.
- **Repo** = uno nuevo en `C:\ReposGit\MobsPoderosos`, con la misma base que este proyecto.

Toda la lógica corre en el servidor, igual que en Inventario Compartido. Los dos mods se pueden instalar juntos sin tocarse.

## Paso 0 — Crear el repo y dejar este plan ahí

1. Crear `C:\ReposGit\MobsPoderosos\` y guardar este plan en `docs/plan.md` (la convención es que los planes van en `docs/`, no en `claude/`).
2. Copiar la base de `C:\ReposGit\CompartirInventario`: `build.gradle`, `settings.gradle`, `gradle/` (incluye `gradle-daemon-jvm.properties` para Java 25), `gradlew`, `gradlew.bat`, `.gitignore`, `.gitattributes`, `LICENSE`, `.claude/commands/docs.md`, `scripts/verificar-docs.mjs`.
3. Ajustar:
   - `settings.gradle` → `rootProject.name = 'mobspoderosos'`
   - `gradle.properties` → `version=0.1.0`, `group=com.mobspoderosos` (las versiones de MC/Loader/Loom/Fabric API y `org.gradle.java.installations.paths` quedan iguales)
   - `build.gradle` → mod `"mobspoderosos"`, `modId = "mobspoderosos-test"` en `configureTests`. El run `client2` se puede quitar o dejar.
   - `CLAUDE.md` nuevo con el mismo formato y las mismas reglas de workflow (`/docs`, `runGameTest`, subir la versión, español rioplatense, prefijo `mobspoderosos$` en mixins). Copiar las notas transversales de `claude/general/` (entorno Java/Gradle, MC 26 sin ofuscar, probar con dos jugadores), que valen tal cual.
4. `git init` (sin commitear hasta que lo pidas).

## Arquitectura

Paquete `com.mobspoderosos`, mixins en `com.mobspoderosos.mixin`.

| Archivo | Responsabilidad |
|---|---|
| `MobsPoderosos.java` | Entrypoint `main`: carga la config, registra los attachments, los eventos y el comando |
| `Configuracion.java` | Record con todos los números y sus valores por defecto (desde la fase 1). En la fase 5 se suma la carga/guardado de `config/mobspoderosos.json` con Gson (viene con MC) |
| `NivelDeZona.java` | `int calcular(ResourceKey<Level> dimension, BlockPos spawn, BlockPos pos, Configuracion, RandomSource)`: la fórmula pura, sin `ServerLevel`, para poder testearla con un random con seed fijo. Un overload de conveniencia saca la dimensión y el spawn del `ServerLevel` |
| `NivelDeMob.java` | Attachments `NIVEL` y `PENDIENTE`, `aplicar(Mob, nivel)` (atributos, vida llena, nombre) y `alConvertir(viejo, nuevo)` (conversiones, ver §1) |
| `EquipoPorNivel.java` | Agrega armadura, encanta armadura y arma, y ajusta `setDropChance` |
| `BotinPorNivel.java` | Al morir: tiradas extra de vanilla + tabla de ítems raros por rango |
| `ComandoMobsPoderosos.java` | `/mobspoderosos zona` (nivel donde estás) · `/mobspoderosos invocar <entidad> <nivel>` (op, para probar) · `/mobspoderosos recargar` (op, fase 5) |
| `mixin/MobMixin.java` | `finalizeSpawn` HEAD: si el motivo escala, marca el mob con el attachment `PENDIENTE` |
| `mixin/LivingEntityMixin.java` | `getExperienceReward` RETURN: multiplica la experiencia · silencia el log "Named entity died" (ver Riesgos) |
| `mixin/CreeperAccessor.java` | `@Accessor` de `Creeper.explosionRadius` (es `private int`) para agrandar la explosión |

Recursos: `fabric.mod.json`, `mobspoderosos.mixins.json`, `data/mobspoderosos/loot_table/botin_por_nivel/{raro,epico,legendario}.json`, `data/mobspoderosos/tags/entity_type/{sin_nivel,usan_equipo,sin_tiradas_extra}.json`, `assets/mobspoderosos/icon.png`.

### 1. Cuándo se le asigna el nivel a un mob

- `Mob.finalizeSpawn(ServerLevelAccessor, DifficultyInstance, EntitySpawnReason, SpawnGroupData)` es el único lugar que conoce el **motivo** del spawn (Fabric no tiene un evento con el motivo). Un mixin en **HEAD** le pone al mob el attachment **persistente** `PENDIENTE` si el motivo escala. Tiene que ser HEAD y no TAIL: `Zombie.finalizeSpawn` llama a `super` primero y recién después le pone el equipo (verificado con `javap`).
- **Por qué un attachment persistente y no un campo `@Unique`:** los mobs de `STRUCTURE` y `CHUNK_GENERATION` se crean en un `WorldGenRegion`, y `ProtoChunk.addEntity(Entity)` los guarda como NBT (`Entity.save` → `addEntity(CompoundTag)`). Cuando el chunk pasa a estar completo, el mob se vuelve a crear desde ese NBT. Un campo transitorio se perdería ahí (evocadores y vindicadores de las mansiones, la bruja de la cabaña, los shulkers de las ciudades del End); el attachment viaja en el NBT. Verificado con `javap` en 26.3.
- `ServerEntityEvents.ENTITY_LOAD` (lifecycle-events) corre cuando el mob entra al mundo, **después** de que vanilla le puso el equipo. Ahí: si es `Enemy`, tiene `PENDIENTE` y no tiene `NIVEL`, se calcula el nivel, se aplica todo y se quita `PENDIENTE`. Por eso no hace falta engancharse en cada subclase.
- **Motivos** (`EntitySpawnReason` en 26.3):
  - **Escalan por posición:** `NATURAL`, `CHUNK_GENERATION`, `STRUCTURE`, `PATROL`, `EVENT` (raids), `REINFORCEMENT` (refuerzos de zombi), `JOCKEY` (el esqueleto que monta la araña; si no, la araña tiene nivel y el jinete no), `MOB_SUMMONED` (vexes del evocador), `TRIGGERED` (jinetes esqueleto de la trampa del rayo, lepismas de bloques infestados). Los que aparecen por otro mob salen en el mismo lugar, así que quedan prácticamente con el mismo nivel.
  - **No escalan:** `SPAWNER` y `TRIAL_SPAWNER` (si no, una granja de spawner se vuelve una fábrica de loot; se puede activar en la config), `SPAWN_ITEM_USE` (huevos; en 26.3 no existe `SPAWN_EGG`), `DISPENSER`, `COMMAND` (para eso está `/mobspoderosos invocar`), `BREEDING`, `BUCKET`, `LOAD`, `DIMENSION_TRAVEL`.
  - **`CONVERSION`:** no se calcula por posición; lo maneja el evento de conversión (abajo).
- **Conversiones (`ServerLivingEntityEvents.MOB_CONVERSION`, firma `(Mob viejo, Mob nuevo, ConversionParams)`):** vanilla copia `CUSTOM_NAME` y `customNameVisible` en `ConversionType` (verificado), pero no el nivel. Sin esto, un zombi ahogado sería un ahogado llamado "Zombi [Nv 7]" con estadísticas base, un aldeano zombi curado quedaría como aldeano llamado "Aldeano zombi [Nv 12]" para siempre, y los slimes al dividirse (que usan `convertTo(..., TRIGGERED, ...)`) tendrían hijos con el nombre del padre y sin nivel. `NivelDeMob.alConvertir`:
  - si el viejo tenía `NIVEL` y el nuevo es `Enemy` (y no está en `sin_nivel`): le copia el nivel y llama a `aplicar` (que rehace el nombre con el tipo nuevo y pisa lo que haya puesto `ENTITY_LOAD`);
  - si el nuevo no es `Enemy` (el aldeano curado): le borra el nombre y no le pone nivel.
- Los jefes (`EnderDragon`, `WitherBoss`, `ElderGuardian`, `Warden`) se excluyen con el tag `sin_nivel` desde la fase 1 (el guardián anciano aparece con `STRUCTURE`).
- El attachment `NIVEL` (`fabric-data-attachment-api-v1`, `AttachmentRegistry.create` con `Codec.INT` persistente) hace que el nivel sobreviva a guardar y cargar el chunk, y evita volver a aplicarlo en el próximo `ENTITY_LOAD`.

### 2. Fórmula del nivel (`NivelDeZona`)

```
distancia = distancia horizontal entre pos y el punto de referencia
nivel     = 1 + floor(distancia / bloquesPorNivel)   // bloquesPorNivel = 150
          + extraDimension                           // Nether +10, End +20
          + random(-1..+1)                           // variación para que no sean todos iguales
nivel     = clamp(nivel, 1, nivelMaximo)             // nivelMaximo = 50
```

Punto de referencia por dimensión:
- **Overworld:** el spawn del mundo, de `ServerLevel.getRespawnData()` (`LevelData$RespawnData.pos()`; verificado con `javap` en 26.3).
- **Nether:** la coordenada ×8 contra el spawn del overworld, para ser coherente con los portales.
- **End:** la distancia al centro `(0, 0)` del End (la del spawn del overworld no significa nada ahí). Las islas exteriores arrancan a ~1000 bloques, así que las ciudades del End quedan en nivel ~30+.

El `RandomSource` es el del mob, para que los tests puedan pasarle uno con seed fijo.

### 3. Estadísticas (`NivelDeMob.aplicar`)

Modificadores **permanentes** con un `Identifier` fijo `mobspoderosos:nivel` (`addOrReplacePermanentModifier`). Vanilla los guarda en el NBT del mob y no se apilan si se aplica dos veces:

| Atributo | Operación | Por nivel | Tope | Se llega al tope en |
|---|---|---|---|---|
| `MAX_HEALTH` | `ADD_MULTIPLIED_BASE` | +10 % | +400 % (×5) | nivel 40 |
| `ATTACK_DAMAGE` | `ADD_MULTIPLIED_BASE` | +4 % | +100 % (×2) | nivel 25 |
| `ARMOR` | `ADD_VALUE` | +0,2 | +8 | nivel 40 |
| `MOVEMENT_SPEED` | `ADD_MULTIPLIED_BASE` | +0,5 % | +20 % | nivel 40 |
| `FOLLOW_RANGE` | `ADD_MULTIPLIED_BASE` | +1 % | +50 % | nivel 50 |
| `KNOCKBACK_RESISTANCE` | `ADD_VALUE` | +0,01 | 0,5 | nivel 50 |

**Ojo con el daño:** `ADD_MULTIPLIED_BASE` multiplica el valor base **más todos los `ADD_VALUE`**, y eso incluye el modificador del arma. Un zombi con espada de hierro (+5) de nivel 25 pega `(3 + 5) × 2 = 16`, no `3 × 2 + 5`. Por eso el tope de daño es más bajo que el de vida.

**Creeper:** el daño de su explosión no usa `ATTACK_DAMAGE`, así que sin esto sólo se vuelve más resistente. Con `CreeperAccessor`: `explosionRadius` pasa de 3 a 4 desde el nivel 30 (si lo carga un rayo: de 6 a 8). Configurable (`radioCreeperExtra`, `nivelRadioCreeper`).

**Esqueletos y otros de ataque a distancia:** el daño de la flecha tampoco usa `ATTACK_DAMAGE`; escala por el encantamiento del arco (Poder), ver §4.

Después: `setHealth(getMaxHealth())` (si no, aparece con la vida base y no con la nueva). Todos los números van en `Configuracion`.

**Nombre:** `mob.getType().getDescription().copy()` + `" [Nv 7]"`, coloreado por rango (gris 1–9, verde 10–19, azul 20–29, violeta 30–39, dorado 40+). Al ser *translatable*, cada cliente lo ve en su idioma. `setCustomNameVisible(nombreSiempreVisible)`, que **por defecto es `false`**: un nombre siempre visible se dibuja a través de las paredes hasta unos 64 bloques y funciona como un "rayos X" de mobs en las cuevas. Con `false` aparece al mirar el mob.

Si un jugador le pone un name tag a un mob con nivel, el nombre se reemplaza (se pierde la etiqueta "[Nv N]") pero las estadísticas siguen. Es aceptable; queda documentado en el README.

### 4. Equipo encantado (`EquipoPorNivel`)

Sólo para mobs que ya usan equipo en vanilla (zombis, esqueletos, piglins, pillagers… con el tag `usan_equipo`):
- Por cada ranura de armadura vacía: chance `min(nivel × 1,5 %, 50 %)` de ponerle una pieza. El material depende del rango: cuero < 10, cadena/oro 10–19, hierro 20–29, diamante 30–39, netherita 40+.
- **Sin casco para los que se queman al sol** (`EntityTypeTags.BURN_IN_DAYLIGHT`, existe en 26.3): si no, los zombis y esqueletos con casco sobreviven de día, llenan el límite de mobs y el ciclo día/noche deja de importar. Configurable (`cascoParaLosQueSeQueman`, por defecto `false`). Los cascos que pone vanilla no se tocan.
- Encantar la armadura y el arma con poder `≈ nivel / 2` usando `EnchantmentHelper.enchantItem(RandomSource, ItemStack, int, RegistryAccess, Optional<HolderSet<Enchantment>>)` (firma verificada en 26.3), pasando el tag `EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT`. Así no salen encantamientos de tesoro (Reparación, maldiciones), igual que en vanilla. Con un arco, esto da Poder: es lo que hace escalar a los esqueletos.
- Chance de soltar el equipo:
  - En general: `setDropChance(slot, min(0,085 + nivel × 0,004, 0,25))`.
  - Piezas de **diamante o netherita que puso el mod**: se quedan en `0,085` (como vanilla). Si no, en nivel 40+ la netherita encantada sale gratis.
  - Vanilla sólo suelta el equipo si lo golpeó un jugador, y cuando la chance es ≤ 1 lo suelta muy dañado. Las dos cosas ya juegan a favor del balance.

### 5. Loot y experiencia (`BotinPorNivel` + mixin)

En `ServerLivingEntityEvents.AFTER_DEATH`, **sólo si lo mató un jugador** (`getLastHurtByPlayer() != null`), para que las granjas automáticas no lo aprovechen:
- **Tiradas extra:** `min(floor(nivel / 10), 3)` veces `mob.getLootTable().ifPresent(tabla -> mob.dropFromLootTable(level, source, true, tabla))`. `getLootTable()` devuelve un `Optional`: hay mobs sin tabla. El overload público con `ResourceKey<LootTable>` ya está verificado en 26.3.
  - El tope de 3 (configurable: `tiradasExtraMaximas`) evita que un nivel 50 suelte 11 veces su tabla.
  - Tag `sin_tiradas_extra`: los mobs cuya tabla tiene ítems que no deberían multiplicarse. Por defecto `evoker` (uno de nivel 30 o más soltaría 4 tótems), `wither_skeleton` (en el Nether casi siempre tienen nivel alto: las calaveras serían triviales) y `shulker` (caparazones). Se puede editar con un datapack.
- **Ítems raros:** según el rango se tira la tabla propia `botin_por_nivel/raro` (nivel ≥ 10, 15 %), `epico` (≥ 25, 10 %) y `legendario` (≥ 40, 5 %): manzanas doradas, diamantes, libros encantados, plantillas de herrería, etc. Son JSON de datapack, así que se pueden editar o pisar con un datapack sin recompilar. Estas sí aplican también a los mobs de `sin_tiradas_extra`.
- **Experiencia:** mixin en `LivingEntity.getExperienceReward(ServerLevel, Entity)` (es `final`, así que cubre todas las subclases) en RETURN: `× (1 + nivel × 0,1)` si es un `Mob` con nivel.

### 6. Balance de referencia

Números con los valores por defecto, dificultad **Normal**, sin contar encantamientos (en Difícil el daño a jugadores es ×1,5). Sirven para ajustar la config y como guía para la prueba a mano.

| Nivel (dónde) | Vida zombi/esqueleto/creeper (base 20) | Daño zombi sin arma (base 3) | Daño zombi con espada de hierro | Armadura extra | Explosión creeper | Tiradas extra | Experiencia |
|---|---|---|---|---|---|---|---|
| 1 (spawn) | 22 | 3,1 | 8,3 | +0,2 | radio 3 | 0 | ×1,1 |
| 10 (~1350 bloques) | 40 | 4,2 | 11,2 | +2 | radio 3 | 1 | ×2 |
| 25 (~3600 bloques) | 70 | 6 | 16 | +5 | radio 3 | 2 | ×3,5 |
| 40 (~5850 bloques) | 100 | 6 | 16 | +8 | radio 4 | 3 | ×5 |
| 50 (~7350 bloques) | 100 | 6 | 16 | +8 | radio 4 | 3 | ×6 |

En el Nether el +10 y la escala ×8 hacen que los niveles altos lleguen rápido: a 600 bloques del Nether (4800 en el overworld) ya es nivel ~43. Es a propósito, pero conviene confirmarlo jugando.

### 7. Config y comando

`Configuracion` es un record con valores por defecto desde la fase 1, así ningún número queda escrito a mano en el código. En la fase 5, `config/mobspoderosos.json` se crea con esos valores en el primer arranque: `bloquesPorNivel`, `nivelMaximo`, extras por dimensión, porcentaje y tope por atributo, radio del creeper, chances de armadura y de soltar equipo, `cascoParaLosQueSeQueman`, `tiradasExtraMaximas`, `escalarSpawners`, `nombreSiempreVisible`, chances de loot. `/mobspoderosos recargar` la vuelve a leer sin reiniciar el servidor (sólo afecta a los mobs que aparezcan después).

El comando `/mobspoderosos invocar zombie 30` pone un mob con ese nivel justo delante (sirve para probar sin caminar 4500 bloques). Llama a `finalizeSpawn` con `COMMAND` (para que vanilla le ponga el equipo), lo agrega al mundo y después aplica el nivel y el equipo a mano.

## Riesgos

**Ya verificados con `javap` en 26.3:**
- **Despawn:** `Mob.checkDespawn` mira `isPersistenceRequired()` (un campo que sólo se activa con un name tag o al levantar ítems), no `hasCustomName()`. Ponerle nombre por código **no** hace que el mob quede para siempre.
- **Log "Named entity {} died: {}":** existe en `LivingEntity.die` y se loguea para todo mob con nombre. Con niveles llenaría la consola, así que se corta desde la **fase 1** con un `@WrapWithCondition` sólo para mobs con `NIVEL`.
- **`ProtoChunk.addEntity(Entity)`** serializa el mob a NBT (por eso `PENDIENTE` es un attachment persistente).
- **`ConversionType`** copia `CUSTOM_NAME` y `customNameVisible` al mob nuevo. Los slimes se dividen con `convertTo(..., TRIGGERED, ...)`.
- **`Zombie.finalizeSpawn`** llama a `super` antes de ponerle el equipo.
- **Firma de `EnchantmentHelper.enchantItem`**, `EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT`, `EntityTypeTags.BURN_IN_DAYLIGHT`, `Creeper.explosionRadius` (`private int`), `ServerLivingEntityEvents.MOB_CONVERSION`.

**Falta verificar antes de escribir el código que los usa:**
- **Orden `finalizeSpawn` → `addFreshEntity`** en `NaturalSpawner`: que el equipo ya esté puesto cuando llega `ENTITY_LOAD`.
- **Orden `ENTITY_LOAD` ↔ `MOB_CONVERSION`** dentro de `Mob.convertTo`, y si `convertTo` llama a `finalizeSpawn` en cada caso. El diseño funciona en cualquier orden (`alConvertir` pisa con `addOrReplace`), pero hay que saber si un slime hijo pasa por `ENTITY_LOAD` con `PENDIENTE`.
- **Cambio de dimensión (`DIMENSION_TRAVEL`):** el mob se vuelve a crear en la otra dimensión. Confirmar que Fabric copia los attachments persistentes y que los modificadores (que van en el NBT) siguen ahí.

## Fases (cada una termina con `gradlew runGameTest` en verde y la versión subida)

1. **Base** (0.1.0): repo, `Configuracion` como record con valores por defecto, nivel por distancia (las tres dimensiones), atributos con topes, creeper, nombre, attachments `NIVEL` y `PENDIENTE`, todos los motivos de spawn, conversiones, tag `sin_nivel`, log silenciado, comando `invocar`.
2. **Loot y experiencia** (0.2.0): tiradas extra con tope, tag `sin_tiradas_extra`, mixin de experiencia, filtro por jugador.
3. **Equipo** (0.3.0): armadura por rango (sin casco para los que se queman), encantamientos con `ON_MOB_SPAWN_EQUIPMENT`, chances de soltar.
4. **Ítems raros** (0.4.0): las tres tablas de loot propias.
5. **Config + pulido** (0.5.0): config JSON, comando `recargar`, README y notas `claude/` vía `/docs`.

## Verificación

**GameTests** (`src/gametest`, con `GameTestHelper`). Acá no hay estado global compartido como en Inventario Compartido, así que pueden ser varios tests independientes:
- `NivelDeZona` (función pura, con `RandomSource` de seed fijo y variación desactivada en una de las pruebas): posiciones a 0 / 1500 / 7500 bloques dan nivel 1 / 11 / 50 (tope); el Nether usa ×8 y suma el extra; el End mide desde `(0, 0)`.
- `aplicar(zombie, 20)`: la vida máxima es la base × 3, la vida está llena, el nombre contiene "[Nv 20]", y aplicarlo dos veces no apila modificadores. `aplicar(zombie, 50)` respeta los topes (vida ×5, daño ×2).
- Spawn con `SPAWNER` → sin attachment; con `NATURAL` → con nivel; con `JOCKEY` → el jinete también tiene nivel.
- **Mob de estructura:** un mob que pasa por un `ProtoChunk` (guardarlo a NBT con `PENDIENTE` y volver a crearlo) recibe nivel al cargarse.
- **Conversiones:** zombi → ahogado conserva el nivel y el nombre dice "Ahogado"; aldeano zombi curado → aldeano sin nombre ni nivel; slime de nivel N dividido → los hijos tienen nivel N.
- **Loot:** probar `tiradasExtra(nivel)` como función (0 en nivel 9, 1 en 10, 3 en 50) en vez de comparar ítems con el mismo seed, que es difícil de controlar. Muerte a manos de un jugador de prueba (con `joinSurvivalPlayer`, como en el otro repo; **no** `makeMockServerPlayerInLevel`): un evocador de nivel 25 suelta un solo tótem (tag `sin_tiradas_extra`), y un mob con nivel da más experiencia que uno sin nivel. Muerto sin jugador → sin extras.
- Guardar y recargar el mob (`ValueOutput`/`ValueInput`) conserva el nivel y no reaplica los modificadores.
- El mob con nivel sigue teniendo `isPersistenceRequired() == false`.

**A mano** (`gradlew runServer` + `runClient`, ver la nota de probar con dos jugadores): `/mobspoderosos zona` en el spawn y a 3000 bloques, `/mobspoderosos invocar skeleton 45` y ver su nombre dorado, la armadura de netherita encantada (sin casco), el loot extra y la experiencia. Comparar los números con la tabla de §6. Ahogar un zombi con nivel y ver que el ahogado tiene el nombre correcto. También probar con un cliente **vanilla sin el mod**, para confirmar que no hace falta nada del lado del cliente.
