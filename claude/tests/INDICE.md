# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

| Nota | De qué trata |
|---|---|
| [Niveles](niveles.md) | Tests independientes (no hay estado global) · corren a ~11 millones de bloques: con `NATURAL` todo sale nivel máximo, así que las estadísticas se prueban con `COMMAND` + `aplicar` · spawneá con `EntityType.spawn(level, pos, motivo)` para pasar por `finalizeSpawn` · para simular el `ProtoChunk`, sacá `"UUID"` del NBT antes de recrear · conversiones con `convertTo` directo · el log cuenta un test más que los nuestros (es de Fabric) |
| [Botín](botin.md) | El loot es al azar: se prueba con el devastador (siempre una montura) y el evocador (siempre un tótem) · la chance del evocador se prueba con márgenes sobre 20 mobs (el random del mob no se puede fijar) · matá con `playerAttack(jugador)` para que cuente como muerte por jugador · el jugador sale de `joinSurvivalPlayer`, no de `makeMockServerPlayerInLevel` · borrá los ítems contados: todos mueren en el mismo lugar · la experiencia con creepers, que no tienen equipo al azar |
