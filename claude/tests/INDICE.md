# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

| Nota | De qué trata |
|---|---|
| [Niveles](niveles.md) | Tests independientes (no hay estado global) · corren a ~11 millones de bloques: con `NATURAL` todo sale nivel máximo, así que las estadísticas se prueban con `COMMAND` + `aplicar` · spawneá con `EntityType.spawn(level, pos, motivo)` para pasar por `finalizeSpawn` · para simular el `ProtoChunk`, sacá `"UUID"` del NBT antes de recrear · conversiones con `convertTo` directo · el log dice 7 tests aunque los nuestros sean 6 |
