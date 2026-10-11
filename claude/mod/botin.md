# Botín y experiencia

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **una granja automática de mobs con nivel suelta el triple de loot sin que nadie los toque**, **una tabla de ítems raros con un error de formato no suelta nada y no hay ningún error visible**, **`/gamerule mobDrops false` sigue soltando ítems**, **un evocador de nivel 30 suelta cuatro tótems seguros**, o **un mob con `DeathLootTable` propia suelta una vez su tabla y las tiradas extra salen de la tabla de su tipo**.

## Tiradas extra: dentro de dropAllDeathLoot, no en AFTER_DEATH

**Las tiradas extra y el botín raro se hacen en `LivingEntityMixin.mobspoderosos$tiradasExtra`, un `@WrapOperation` sobre la llamada a `dropFromLootTable(ServerLevel, DamageSource, boolean)` dentro de `LivingEntity.dropAllDeathLoot`.** No van en `ServerLivingEntityEvents.AFTER_DEATH`, aunque el plan original lo decía.

`dropAllDeathLoot` en 26.3 (verificado con `javap`):

```
boolean matoUnJugador = lastHurtByPlayerMemoryTime > 0;
if (shouldDropLoot(level)) {          // !isBaby() && gamerule MOB_DROPS
    dropFromLootTable(level, source, matoUnJugador);   // ← acá se engancha el mod
    dropCustomDeathLoot(level, source, matoUnJugador);
}
dropEquipment(level);
dropExperience(level, source.getEntity());
```

| Por qué ahí | Qué pasaría con `AFTER_DEATH` |
|---|---|
| La llamada ya pasó por `shouldDropLoot`: respeta la gamerule `mobDrops` y que las crías no suelten nada | Habría que repetir esas condiciones a mano (y `shouldDropLoot` es `protected`) |
| `matoUnJugador` es el mismo criterio que vanilla usa para el loot `killed_by_player`: un jugador lo golpeó en los últimos 100 ticks | `getLastHurtByPlayer() != null` es casi lo mismo, pero otro criterio |
| Se llama **antes** que la tirada de vanilla. `Mob.dropFromLootTable` hace `lootTable = Optional.empty()` después de usarla, así que la tabla propia del mob (`DeathLootTable` en el NBT) recién después desaparece | `getLootTable()` ya devolvería la tabla del tipo, no la propia |

**Sólo hay tiradas extra y botín raro si `matoUnJugador`.** Si no, una granja automática (mobs que caen en lava o por una caída) multiplicaría el loot sin riesgo.

`BotinPorNivel.soltarTiradasExtra` llama `tiradasExtra(nivel)` veces a `mob.dropFromLootTable(level, source, true, tabla)`, el overload público con `ResourceKey<LootTable>`. `getLootTable()` devuelve un `Optional`: un mob sin tabla no suelta nada extra.

- **Cada tirada es independiente:** `LootTable.getRandomItems` usa `getLootTableSeed()`, y con seed 0 (lo normal) toma el random del mundo. Un mob con `DeathLootTableSeed` fijo en el NBT soltaría N veces lo mismo; es aceptable.
- **El equipo puesto no se multiplica:** sale por `dropEquipment`, fuera del wrap.

### Cuántas tiradas

`tiradasExtra(nivel) = min(nivel / nivelesPorTiradaExtra, tiradasExtraMaximas)`, con `8` y `6` en `Configuracion.POR_DEFECTO`.

| Nivel | Tiradas extra | Total de la tabla |
|---|---|---|
| 1–7 | 0 | ×1 |
| 8–15 | 1 | ×2 |
| 16–23 | 2 | ×3 |
| 24–31 | 3 | ×4 |
| 32–39 | 4 | ×5 |
| 40–47 | 5 | ×6 |
| 48–50 | 6 (tope) | ×7 |

**Los dos números van juntos con `nivelMaximo` (50):** con un paso de 10, el tope de 6 nunca se alcanzaría. Con el máximo por defecto el tope no corta nada; está para cuando se sube `nivelMaximo` en la config.

### Chance de cada tirada: los tags

**Cuántas tiradas hay lo decide el nivel; si cada una sale, lo decide `BotinPorNivel.chanceTiradaExtra(mob)`.** Cada tirada se sortea por separado con `mob.getRandom()`.

| Tag (`data/mobspoderosos/tags/entity_type/`) | Mobs | Chance de cada tirada | Por qué |
|---|---|---|---|
| — | Todos los demás | 1 (segura) | |
| `tiradas_extra_por_chance` (`TIRADAS_EXTRA_POR_CHANCE`) | `evoker` | `chanceTiradaExtra.en(nivel)`: +1 % por nivel, tope 50 % | Su tabla siempre da un tótem: con tiradas seguras, uno de nivel 30 soltaría 4. Así es ~0,9 tótems extra en promedio |
| `sin_tiradas_extra` (`SIN_TIRADAS_EXTRA`) | `wither_skeleton`, `shulker` | 0 | En el Nether los esqueletos del Wither casi siempre tienen nivel alto: las calaveras serían triviales. Los caparazones son la única fuente de shulker boxes |

- **Si un mob está en los dos tags, gana `sin_tiradas_extra`** (se chequea primero).
- Los dos se pueden cambiar con un datapack (`"replace": false`). **No afectan al botín raro**, que sale de tablas del mod y no multiplica la del mob.
- **La chance es por tirada, no una sola para todas:** un evocador de nivel 50 tiene 6 tiradas al 50 %, entre 0 y 6 tótems extra (3 en promedio).

## Botín raro

**Después de las tiradas extra, `BotinPorNivel.soltarBotinRaro` sortea tres tablas propias, cada una por separado**: un mob de nivel 40 puede soltar las tres a la vez.

| `BotinPorNivel.Botin` | Tabla (`data/mobspoderosos/loot_table/`) | Desde (`nivelMinimo`) | Chance |
|---|---|---|---|
| `RARO` | `botin_por_nivel/raro.json` | 10 | 15 % |
| `EPICO` | `botin_por_nivel/epico.json` | 25 | 10 % |
| `LEGENDARIO` | `botin_por_nivel/legendario.json` | 40 | 5 % |

- Los números van en `Configuracion` (`botinRaro`, `botinEpico`, `botinLegendario`, records `Rareza`). Cada `Botin` los lee de la configuración actual, no al cargar la clase, así que `/mobspoderosos recargar` los cambia enseguida.
- `botinesRaros(nivel)` es la lista de las que tocan, y es lo que prueban los tests. Con nivel 0 (sin nivel) da vacía aunque un `nivelMinimo` mal configurado fuera 0.
- **Se tiran con `mob.dropFromLootTable(level, source, true, tabla)`**, el mismo overload que las tiradas extra: los parámetros son los de una muerte (`"type": "minecraft:entity"`) y los ítems aparecen donde murió el mob.
- **Las tablas no pueden tener tótems ni monturas.** Los tests cuentan esos ítems (evocadores, devastadores) y otro ítem de la tabla los haría fallar al azar.

### Formato de las tablas en 26.3

**Las tablas de loot de 26.3 usan `"modifier"` (un objeto o una lista) y `"condition"`**, no `"functions"` ni `"conditions"` como en 1.21. Lo de los tutoriales viejos no carga.

**Una tabla con un error no tira el servidor:** vanilla loguea el error al cargar los datos, la tabla queda como `LootTable.EMPTY` y no suelta nada. Por eso el test `botinRaro` revisa que cada tabla cargue y que siempre dé algo (ver [tests: botín](../tests/botin.md)). Para copiar el formato, sacá una tabla de vanilla del jar (`data/minecraft/loot_table/chests/`).

## Experiencia

**`LivingEntityMixin.mobspoderosos$experienciaPorNivel` es un `@ModifyReturnValue` sobre `LivingEntity.getExperienceReward(ServerLevel, Entity)`.** Ese método es `final`, así que un solo mixin cubre todas las subclases. Corre después de `EnchantmentHelper.processMobExperience`, así que multiplica el valor ya procesado.

`BotinPorNivel.experiencia(base, nivel) = round(base × (1 + nivel × experienciaPorNivel))`, con `0.1` por defecto. Si el mob no tiene nivel (`nivel == 0`) devuelve la base sin tocar.

- **No hace falta filtrar por jugador:** `dropExperience` ya sólo da experiencia si `lastHurtByPlayerMemoryTime > 0` (o si el mob es `isAlwaysExperienceDropper`) y la gamerule lo permite.
- **`Mob.getBaseExperienceReward` es al azar si el mob tiene equipo:** suma 1–3 por cada pieza con chance de soltarse ≤ 1. Por eso el test usa creepers (ver [tests: botín](../tests/botin.md)).

## Ver también

- [Niveles](niveles.md)
- [Equipo](equipo.md)
- [Configuración](configuracion.md)
- [Tests: botín](../tests/botin.md)
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)
