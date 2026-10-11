# Plan — Eventos y afijos (0.9.0)

Cuatro features nuevas. Todo sigue siendo del lado del servidor: barra de jefe, títulos, sonidos y nombres son cosas que cualquier cliente vanilla ya muestra.

## Base común

- **`NivelDeMob.invocarConNivel(tipo, level, pos, motivo, nivel, afijo)`**: crea el mob, corre `finalizeSpawn` (equipo de vanilla), le aplica nivel + equipo **antes** de `addFreshEntity`. Como ya tiene `NIVEL`, `alCargar` sólo le borra `PENDIENTE` (es la misma rama que usan las conversiones). Lo usan la estampida, las escoltas del jefe, los refuerzos del invocador y la compañía de la luna de sangre: ninguno vuelve a tirar campeón, así que no hay recursión.
- **`Eventos`**: un `END_SERVER_TICK`. Cada tick corre las tareas demoradas (la explosión del afijo explosivo, la compañía de la luna); cada segundo actualiza la luna, las estampidas y los jefes; cada minuto sortea estampidas y jefes por jugador.
- **`/mobspoderosos evento <estampida|jefe|luna_de_sangre> [jugador]`** (op) para probarlos sin esperar.
- Config: cuatro objetos nuevos en `config/mobspoderosos.json` (`afijos`, `estampida`, `lunaDeSangre`, `jefeDeZona`), cada uno con `activo` y sus números.

## Afijos de campeón

- Cada campeón (`chanceDeCampeon`) recibe un afijo al azar con `afijos.chance` (por defecto 1): **Veloz**, **Vampiro**, **Explosivo**, **Invocador**.
- Attachment persistente `AFIJO`. El nombre pasa a ser "Zombi [Nv 27] [Vampiro]": `nombre(tipo, nivel, afijo)` y `esNombreDelMod` lo incluyen, así las conversiones lo siguen reconociendo. Las conversiones copian el afijo.
- Veloz: modificador de velocidad propio (`mobspoderosos:afijo`), aplicado dentro de `aplicarEstadisticas` para que las conversiones lo rehagan.
- Vampiro: `AFTER_DAMAGE`, si el atacante (`source.getEntity()`, cubre flechas) es vampiro se cura una fracción del daño hecho. No le toca al creeper.
- Explosivo: `AFTER_DEATH` programa una explosión 1,5 s después con el sonido de mecha del creeper. `ExplosionInteraction.NONE`: no rompe bloques (si no, los campeones romperían bases).
- Invocador: `AFTER_DAMAGE` sobre el invocador, con espera entre usos y un máximo por mob (attachments persistentes), llama refuerzos de su tipo con el nivel de la zona. Tag `no_invocan` (vex).

## Estampida (horda)

- Sorteo por minuto y por jugador: overworld, de noche, al aire libre (`canSeeSky`), en supervivencia, zona ≥ `nivelMinimo`, con espera entre estampidas (attachment persistente en el jugador con el `gameTime`).
- Cuántos: `mobsBase + nivelZona / nivelesPorMobExtra`, hasta `mobsMaximos`. Tipos del tag `estampida` (zombi, esqueleto, araña). Aparecen todos del mismo lado, a 24–32 bloques, con objetivo en el jugador, +velocidad y +rango de seguimiento (modificadores transitorios). Uno es el líder: campeón con afijo.
- Barra de jefe roja con los que quedan. Termina cuando no queda ninguno: si los jugadores mataron al menos la mitad (`getLastHurtByPlayer`, para que los que se queman al amanecer no cuenten), premio: una tirada de la mejor tabla de botín raro que le corresponde al nivel, al inventario. También termina, sin premio, si el jugador muere, se desconecta o cambia de dimensión, o a los `segundosMaximos`.
- Estado en memoria: si el servidor se reinicia, quedan como mobs comunes con nivel.

## Luna de sangre

- Al empezar cada noche del overworld se sortea una vez (attachment en el `ServerLevel`): sólo con luna llena (`EnvironmentAttributes.MOON_PHASE`) y con `chance`.
- Mientras dura: los mobs que aparecen suman `nivelesExtra` (dentro de `NivelDeZona`), y cada spawn `NATURAL` trae un compañero con `chanceDeCompania`. Barra roja que se vacía hasta el amanecer, título al empezar y mensaje al terminar.
- El sorteo y el estado viven en attachments del `ServerLevel`: sobreviven a reiniciar el servidor.

## Jefe de zona

- Sorteo por minuto y por jugador, en cualquier dimensión, con espera y `nivelMinimo`. Aparece a 16–24 bloques a la altura del jugador; tipos del tag `jefe_de_zona/<overworld|nether|end>`.
- Nivel de la zona + `nivelesExtra`, vida ×`multiplicadorDeVida`, un afijo, escoltas, persistente. Attachment `JEFE`.
- Barra de jefe con su vida para los jugadores a menos de `radioDeBarra`. Si no hay ningún jugador cerca durante `segundosAbandonado`, desaparece.
- Al morir por un jugador suelta la mejor tabla de botín raro que le toca, mínimo épica (dentro del `@WrapOperation` del botín, así respeta `mobDrops`).
- Al cargarse (reinicio, chunk que vuelve) se vuelve a seguir con su barra: no depende de memoria.
