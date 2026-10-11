# Tests de niveles

[← Índice Tests](INDICE.md)

`MobsPoderososGameTest` tiene **varios tests independientes**, cada uno con `helper.succeed()` al final. A diferencia de Inventario Compartido, acá no hay estado global del servidor: cada test usa sus propios mobs y pueden correr en paralelo.

`gradlew runGameTest` cuenta un test más que los de `MobsPoderososGameTest`: el que sobra es de Fabric. Los tests de loot y experiencia están en [tests: botín](botin.md), los de equipo en [tests: equipo](equipo.md) y los del archivo de configuración en [tests: configuración](configuracion.md).

## Qué cubre

| Test | Qué verifica |
|---|---|
| `nivelPorDistancia` | `NivelDeZona.calcular` como función pura: spawn → 1, 1500 bloques → 11, 7500 → 50 (tope), la distancia se mide desde el spawn y no desde `(0, 0)`, Nether ×8 + 10, End desde `(0, 0)` + 20, y con variación 1 queda en ±1 |
| `aplicarNivel` | Vida ×3 en nivel 20 y vida llena, aplicar dos veces deja **un** modificador, nombre "[Nv 20]" no visible siempre, `isPersistenceRequired()` sigue en `false`, topes en nivel 50 (vida ×5, daño +100 %, armadura +8), un name tag hace que `esNombreDelMod` dé `false` |
| `creeper` | Radio 3 en nivel 29 y 4 en nivel 30 |
| `motivosDeSpawn` | `NATURAL` y `JOCKEY` escalan · `SPAWNER`, `SPAWN_ITEM_USE` y el guardián anciano con `STRUCTURE` (tag `sin_nivel`) no · un aldeano con `NATURAL` queda sin nivel **y sin `PENDIENTE`** |
| `mobDeEstructura` | Un zombi con `finalizeSpawn(STRUCTURE)` → NBT → recreado y agregado al mundo recibe nivel. Recargar uno con nivel 33 conserva el nivel y no duplica modificadores |
| `conversiones` | Zombi → ahogado conserva nivel y su nombre pasa a ser el de ahogado · con name tag conserva "Pepe" y el nivel · aldeano zombi curado queda sin nivel ni nombre · un slime de nivel 5 dividido tiene hijos de nivel 5 |

**No cubre** el log silenciado de `die` (habría que capturar el logger) ni los spawns reales de `NaturalSpawner` y de estructuras: eso se prueba a mano (ver [probar en el juego](../general/probar-en-el-juego.md)).

## Cosas que no son obvias

- **Los tests corren a millones de bloques de `(0, 0)`, pero el spawn del mundo de tests (`getRespawnData().pos()`) queda al lado:** un mob con `NATURAL` sale con nivel 1 o 2, no con el máximo. No asumas ningún nivel para un spawn real: leelo con `NivelDeMob.nivel` y calculá lo esperado a partir de él. Por eso los tests de estadísticas usan `COMMAND` (que no escala) y después `NivelDeMob.aplicar` con un nivel fijo.
- **Spawneá con `tipo.spawn(level, pos, motivo)`**, no con `helper.spawn`: el de `EntityType` pasa por `finalizeSpawn` con el motivo que le des, que es lo que se está probando.
- **Las constantes son `EntityTypes.ZOMBIE`, no `EntityType.ZOMBIE`** (ver [minecraft-26-sin-ofuscar](../general/minecraft-26-sin-ofuscar.md)).
- **Para simular el `ProtoChunk`** (`recrear`): `saveWithoutId` a un `TagValueOutput`, **sacar `"UUID"`** (con el mismo UUID el mundo rechaza la copia), `EntityType.create(level, LOAD)`, `load`, `snapTo` y `addFreshEntity`. Ese último dispara `ENTITY_LOAD` en el momento.
- **Las conversiones se llaman con `mob.convertTo(tipo, ConversionParams.single(mob, true, true), m -> {})`** directo, sin ahogar al zombi: el hook de Fabric está adentro de `convertTo`, así que es el mismo camino.
- **Para dividir un slime:** `setSize(2, true)`, `setHealth(0)` y `remove(Entity.RemovalReason.KILLED)`. `AbstractCubeMob.remove` sólo divide si `isDeadOrDying()`.

## Ver también

- [Niveles](../mod/niveles.md)
- [Tests: botín](botin.md)
- [Tests: equipo](equipo.md)
