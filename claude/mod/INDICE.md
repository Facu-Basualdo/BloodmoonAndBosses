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
