# Índice — Mod (`src/main`)

Java 25 · Fabric API 0.162.0+26.3 · Mixins sobre Minecraft 26.3 (nombres de Mojang).

[← Hub](../../CLAUDE.md)

## Protocolo antes de tocar un mixin

1. Leé [niveles](niveles.md): el reparto entre `finalizeSpawn` y `ENTITY_LOAD` condiciona todo lo demás.
2. Verificá la firma real del método con `javap` (ver [minecraft-26-sin-ofuscar](../general/minecraft-26-sin-ofuscar.md)) en vez de confiar en tutoriales o en memoria.
3. Corré `gradlew runGameTest`.

## Notas

| Nota | De qué trata |
|---|---|
| [Niveles](niveles.md) | `MobMixin` marca `PENDIENTE` en `finalizeSpawn` HEAD y `ENTITY_LOAD` aplica el nivel: `Zombie` pone el equipo **después** de `super`, así que antes no está · `PENDIENTE` es un attachment persistente porque los mobs de estructura pasan por NBT en el `ProtoChunk` · `SPAWNER` y huevos no escalan; `JOCKEY`/`TRIGGERED`/`MOB_SUMMONED` sí · el End mide desde `(0, 0)` y el Nether ×8 · un `Identifier` fijo por modificador: aplicar dos veces no apila · `ADD_MULTIPLIED_BASE` también multiplica el arma · `setHealth(getMaxHealth())` o aparece con la vida base · `MOB_CONVERSION` corre antes de `addFreshEntity` y le gana a `ENTITY_LOAD` · el nombre no evita el despawn · el log "Named entity died" se corta sólo si el nombre es del mod |
| [Equipo](equipo.md) | Se pone sólo al aparecer (`NivelDeMob.aparecer`), nunca en conversiones: no es idempotente · armadura sólo para `usan_equipo` (los que la dibujan) · sin casco para `BURN_IN_DAYLIGHT` o sobreviven de día · se encanta todo lo que tenga el mob con `ON_MOB_SPAWN_EQUIPMENT`, salvo lo ya encantado (`selectEnchantment` no mira lo que tiene) · la chance de soltar nunca baja, no toca la 0 (espada del vex) ni `equipo_valioso` (diamante, netherita, tridente) |
| [Configuración](configuracion.md) | `config/mobspoderosos.json` se mezcla sobre `POR_DEFECTO` antes de `fromJson`: con records, Gson deja en 0 lo que falta · se reescribe completo, pero sólo si validó: un archivo con error queda intacto y sigue la config anterior · clave desconocida = `warn` · al recargar, lo que se aplica al aparecer no cambia; loot y experiencia sí · un campo nuevo va también en `conVariacion` y quizás en `validar` |
| [Botín y experiencia](botin.md) | Las tiradas extra son un `@WrapOperation` dentro de `dropAllDeathLoot`, no `AFTER_DEATH`: así respetan `mobDrops` y crías, y la `DeathLootTable` propia todavía no se borró · sólo si `matoUnJugador` (el criterio de vanilla), para que las granjas no las aprovechen · una tirada cada 16 niveles, tope de 6 (se llega en el nivel 96) · en el tag `tiradas_extra_por_chance` (evocador) cada tirada sale con +0,5 % por nivel, tope 50 % · `sin_tiradas_extra` (esqueleto del Wither, shulker) no tiene y le gana al otro tag · la experiencia es un `@ModifyReturnValue` en `getExperienceReward`, que es `final` · vanilla ya da experiencia sólo si mató un jugador · botín raro: tres tablas propias sorteadas por separado, desde su `nivelMinimo` · en 26.3 las tablas usan `"modifier"`/`"condition"`, y una mal escrita queda vacía sin error |
