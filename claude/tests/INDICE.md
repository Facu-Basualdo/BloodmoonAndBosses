# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

| Nota | De qué trata |
|---|---|
| [Niveles](niveles.md) | Tests independientes (no hay estado global) · el spawn del mundo de tests está al lado de los tests: con `NATURAL` sale nivel 1–2, así que las estadísticas se prueban con `COMMAND` + `aplicar` · spawneá con `EntityType.spawn(level, pos, motivo)` para pasar por `finalizeSpawn` · para simular el `ProtoChunk`, sacá `"UUID"` del NBT antes de recrear · conversiones con `convertTo` directo · `vidaEnElNombre` prueba que el nombre con la vida sigue siendo del mod después de pasar por NBT · el log cuenta un test más que los nuestros (es de Fabric) |
| [Equipo](equipo.md) | Armadura al azar: se prueba con 20 mobs y con lo que nunca puede pasar (casco en un zombi, armadura en un vindicador) · sacale la armadura de vanilla antes (`sinArmadura`) · el camino real con un esqueleto (siempre arco a 0,085), no con el vex (su espada está en 0) · el spawn de tests está al lado: leé el nivel, no lo asumas |
| [Configuración](configuracion.md) | `ArchivoDeConfiguracion.leer` sobre una carpeta temporal, nunca la config global (los tests corren en paralelo) · un archivo inválido no se pisa · el `warn` de la clave `inventada` es esperado |
| [Eventos](eventos.md) | El piso de la estructura es de barreras y `isSpawnPositionOk` las rechaza: `ponerPiso` o `lugarCerca` no encuentra lugar · la estampida al aire libre usa el heightmap: `skyAccess = true` o cae sobre el techo de barreras · `assertValueEqual` es (real, esperado) · `maxTicks` por defecto 20 (la mecha dura 30) · la luna se activa y se apaga en el mismo método: el overworld es compartido · premio con `remove(KILLED)` en el mismo tick |
| [Cliente](cliente.md) | `gradlew runClientGameTest` abre un cliente con ventana: `runGameTest` no aplica los mixins de cliente · el aspecto con vindicadores (los zombis se queman de día) · capturas en `build/run/clientGameTest/screenshots` para mirar los colores · el mundo arranca en luna llena: el test va al día 1 |
| [Botín](botin.md) | El loot es al azar: se prueba con el devastador (siempre una montura) y el evocador (siempre un tótem) · la chance del evocador se prueba con márgenes sobre 20 mobs (el random del mob no se puede fijar) · matá con `playerAttack(jugador)` para que cuente como muerte por jugador · el jugador sale de `joinSurvivalPlayer`, no de `makeMockServerPlayerInLevel` · borrá los ítems contados: todos mueren en el mismo lugar · la experiencia con creepers, que no tienen equipo al azar · el botín raro con devastadores (sólo dan monturas) y cada tabla se tira a mano: una mal escrita queda vacía sin error |
