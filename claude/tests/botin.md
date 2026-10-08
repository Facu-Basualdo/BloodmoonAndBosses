# Tests de botín

[← Índice Tests](INDICE.md)

Tres tests de `MobsPoderososGameTest` cubren el loot extra y la experiencia. El loot es al azar, así que **los tests usan mobs cuya tabla es determinista** en vez de comparar ítems con un seed fijo.

## Qué cubre

| Test | Qué verifica |
|---|---|
| `tiradasExtra` | `BotinPorNivel.tiradasExtra` como función pura: 0 en nivel 7, 1 en 8, 5 en 47, 6 en 48 y 6 (tope) en 200 · `chanceTiradaExtra`: 1 para un zombi, 0 para un esqueleto del Wither (`sin_tiradas_extra`), 0,1 para un evocador de nivel 10 y 0,5 (tope) en nivel 50 |
| `botinAlMorir` | Un devastador muerto por un jugador suelta 4 monturas en nivel 30 (1 + 3) y 7 en nivel 50 (1 + 6) · muerto con `genericKill` suelta 1 · 20 evocadores de nivel 50 sueltan **más de 20 y menos de 140** tótems: las tiradas extra salen, pero no todas |
| `experiencia` | Un creeper sin nivel da 5; uno de nivel 20 da 15 (5 × 3) |

**No cubre** que la gamerule `mobDrops` y las crías corten las tiradas extra: sale de que el wrap está dentro de `shouldDropLoot` (ver [botín](../mod/botin.md)).

## Cosas que no son obvias

- **El devastador siempre suelta una montura** (`entities/ravager`: un pool, `count: 1`, sin condiciones). Cada tirada extra es exactamente una montura más, así que contarlas mide las tiradas. El evocador siempre suelta un tótem (las esmeraldas son al azar, por eso se cuentan sólo los tótems).
- **Las tiradas por chance se prueban con márgenes, no con un seed:** el random es el del mob (`mob.getRandom()`), que no se puede fijar desde el test. Con 20 evocadores de nivel 50 hay 120 tiradas al 50 %: que salgan todas (140 tótems) o ninguna (20) tiene probabilidad 2⁻¹²⁰, así que el test no es flaky. Los valores exactos de la chance se prueban aparte con `chanceTiradaExtra`.
- **Para que cuente como muerte por jugador, matalo con `hurtServer(level, damageSources().playerAttack(jugador), …)`.** `hurtServer` le pone `lastHurtByPlayer` y `dropAllDeathLoot` ve `matoUnJugador = true`. El jugador sale de `joinSurvivalPlayer` (copiado de Inventario Compartido): **no** uses `makeMockServerPlayerInLevel`, que está deprecado. Hay que sacarlo con `playerList.remove` al terminar.
- **Los mobs mueren todos en `DENTRO`:** `matarYContar` borra los `ItemEntity` que cuenta, porque si no el siguiente mob cuenta también los del anterior.
- **La experiencia se prueba con creepers** porque no tienen equipo: `Mob.getBaseExperienceReward` suma 1–3 al azar por cada pieza de equipo, y un zombi puede aparecer con armadura. Se llama `getExperienceReward(level, null)` directo, sin matar al mob.

## Ver también

- [Botín y experiencia](../mod/botin.md)
- [Tests: niveles](niveles.md)
