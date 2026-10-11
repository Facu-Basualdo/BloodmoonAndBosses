# Tests de eventos y afijos

[← Índice Tests](INDICE.md)

## Qué cubre

| Test | Qué verifica |
|---|---|
| `afijos` | Nombre "[Nv 20] [Veloz]" que sigue siendo del mod · modificador `mobspoderosos:afijo` de +0,3 · zombi → ahogado conserva el afijo, el nombre y el modificador · un creeper no puede ser vampiro, un vex (`no_invocan`) no puede ser invocador · el vampiro se cura al pegarle a un aldeano y uno sin afijo no |
| `afijoInvocador` | Al lastimarlo aparecen `refuerzosInvocador` zombis con nivel, sin `PENDIENTE` y sin afijo · un segundo golpe dentro de la espera no invoca · `INVOCACIONES` queda en 1 |
| `afijoExplosivo` | Al morir no explota enseguida · `ticksDeMecha` + 2 después, el aldeano de al lado perdió vida |
| `invocarConNivel` | Nivel dado (37), sin `PENDIENTE` aunque `EVENT` escale, en el mundo · 500 se corta en 100 · con `conAfijo` tiene afijo · un aldeano da `null` |
| `estampida` | `cuantos` y su tope · empieza, no puede haber dos contra el mismo jugador, el primero tiene afijo, todos van contra el jugador con nivel y modificador · muertos por el jugador → termina con premio en el inventario · descartados → termina sin premio · muertos sin jugador (como al amanecer) → sin premio |
| `jefeDeZona` | `JEFE`, persistente, con afijo, contra el jugador, vida llena · vida = la de uno común del mismo nivel × `multiplicadorDeVida` · barra con el jugador cerca · al descartarlo se deja de seguir y la barra se vacía |
| `botinDeJefe` | `mejorBotin` en los cuatro casos · 5 devastadores jefe de nivel 19 muertos por un jugador sueltan algo que no es montura |
| `lunaDeSangre` | `esDeNoche` en los bordes (12999, 13000, 22999, 23000) · `nivelesExtra` con y sin `NOCHE_ACTIVA` · 20 zombis `NATURAL` con luna traen al menos un compañero |
| `archivoDeConfiguracion` | (en [configuración](configuracion.md)) los eventos aparecen en el archivo y `estampida.distanciaMaxima` menor que la mínima se rechaza |

**No cubre** los sorteos (`sortear`: noche, cielo, espera y chance), la barra de la luna, el abandono del jefe ni los títulos y sonidos: dependen del reloj del mundo y de la config global. Se prueban a mano con `/mobspoderosos evento …` (ver [probar en el juego](../general/probar-en-el-juego.md)).

## Cosas que no son obvias

- **El piso de la estructura de test es de barreras, y `SpawnPlacements.isSpawnPositionOk` no deja aparecer mobs sobre una barrera.** Sin `ponerPiso(helper)` (piedra en la capa 0 de la estructura de 8 × 8), `lugarCerca` no encuentra lugar: no hay refuerzos, ni compañeros, ni estampida. Los mobs que se spawnean directo con `tipo.spawn` no pasan por esa regla, por eso los tests viejos no lo necesitaban.
- **La estructura también tiene un techo de barreras (`skyAccess = false`).** La estampida usa el heightmap si el jugador ve el cielo, y caería arriba del techo: su test va con `@GameTest(skyAccess = true)`.
- **Los eventos buscan lugar alrededor:** el jugador va a `CENTRO` (4, 1, 4), no a `DENTRO` (2, 1, 2), que está contra una esquina. La config del test pone las distancias en 2–4 para que entren en la estructura.
- **`assertValueEqual` es (real, esperado):** el mensaje dice "Expected *nombre* to be *segundo*: was *primero*". Los tests viejos lo usan al revés y no importa mientras den igual; al leer un fallo, invertí los números.
- **`@GameTest` vence a los 20 ticks por defecto.** La mecha del explosivo dura 30: `maxTicks = 60`.
- **Para el premio de la estampida, `hurtServer` + `remove(KILLED)`** en vez de esperar la animación de muerte. Si no, el tick del evento (cada segundo) podría dar el premio antes de que el test vacíe el inventario. Todo pasa en el mismo tick, así el jugador no llega a levantar el loot de los mobs.
- **La luna se activa y se apaga dentro del mismo método:** los tests corren en paralelo y comparten el overworld. Si `NOCHE_ACTIVA` quedara puesta, los mobs `NATURAL` de los otros tests tendrían +10 niveles. Los compañeros se cuentan con `runAfterDelay(3)`, porque se crean un tick después.
- **El jefe es persistente:** descartalo al final del test, que la estructura no lo borra.

## Ver también

- [Eventos y afijos](../mod/eventos.md)
- [Tests: niveles](niveles.md)
- [Tests: botín](botin.md)
