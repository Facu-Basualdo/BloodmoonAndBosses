# Tests de equipo

[← Índice Tests](INDICE.md)

Tres tests de `MobsPoderososGameTest` cubren `EquipoPorNivel`. **La armadura es al azar, así que se prueba con muchos mobs y con lo que nunca puede pasar**, no comparando ítems con un seed.

## Qué cubre

| Test | Qué verifica |
|---|---|
| `materialDeArmadura` | `EquipoPorNivel.material` como función: cuero en 9, cadena u oro en 15 (20 tiradas, nunca otra cosa), hierro en 20, diamante en 39, netherita en 40 y en 200 (tope) |
| `armaduraPorNivel` | 20 zombis de nivel 50: **ninguno** con casco (se queman al sol), y alguna pieza en las otras ranuras, de netherita y con chance de soltar 0,085 · 20 piglins de nivel 50: alguno con casco · un vindicador equipado 10 veces sigue sin armadura (no está en `usan_equipo`) |
| `encantamientosYChanceDeSoltar` | Nivel 1 no encanta (poder 0) · nivel 40 sí · chance de soltar el arma: 0,245 en 40, 0,25 (tope) en 50 · espada de diamante (`equipo_valioso`) queda en 0,085 · `setGuaranteedDrop` no baja · un arma con Filo 1 sigue con un solo encantamiento · la espada del vex sigue en 0 · un esqueleto con `NATURAL` sale con la chance de su nivel (camino real por `ENTITY_LOAD`) |

**No cubre** que se vea la armadura en el cliente ni el balance de pelear contra un mob de nivel 40 con netherita: eso es a mano (ver [probar en el juego](../general/probar-en-el-juego.md)).

## Cosas que no son obvias

- **Sacale la armadura de vanilla antes de equipar (`sinArmadura`).** `finalizeSpawn` con `COMMAND` también le da armadura al azar según la dificultad, y el mod no toca las ranuras ocupadas: sin limpiar, un casco de vanilla haría fallar "sin casco".
- **`conArma` pone el arma a mano y la chance en 0,085.** Así el punto de partida es conocido; el arma que vanilla le da al zombi es al azar (a veces ninguna).
- **Los márgenes:** 20 zombis × 3 ranuras al 50 % sin ninguna pieza tiene probabilidad 2⁻⁶⁰, y 20 piglins sin casco 2⁻²⁰. No son flaky.
- **El camino real (`ENTITY_LOAD`) se prueba con un esqueleto, no con un vex.** El esqueleto siempre lleva arco con la chance de vanilla (0,085), así que después de aparecer tiene que tener `min(0,085 + 0,004 × nivel, 0,25)`. El vex sirvió para encontrar el bug de la chance 0 (vanilla nunca suelta su espada y el mod se la subía), y ahora prueba esa regla.
- **El nivel de un spawn real es bajo** porque el spawn del mundo de tests está al lado (ver [tests: niveles](niveles.md)): el test lo lee con `NivelDeMob.nivel` y calcula lo esperado desde ahí.
- Para encantar a mano: `registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS)` y `stack.enchant(holder, nivel)`.

## Ver también

- [Equipo](../mod/equipo.md)
- [Tests: niveles](niveles.md)
- [Tests: botín](botin.md)
