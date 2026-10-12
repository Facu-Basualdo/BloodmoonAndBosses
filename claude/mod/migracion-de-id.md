# Migración del id (mobspoderosos → bloodmoonandbosses)

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: después de actualizar a la 0.11.0, los mobs que ya existían quedan sin nivel o se les sortea uno nuevo; un mob con nivel suma la vida dos veces después de convertirse; la config vuelve a los valores por defecto; la luna de sangre se corta a la mitad.

## Qué cambió

**Desde la 0.11.0 el id del mod es `bloodmoonandbosses`** (antes `mobspoderosos`): paquete `com.bloodmoonandbosses`, jar, config, tags y tablas de botín (`data/bloodmoonandbosses/`), attachments, ids de los modificadores y prefijo `bloodmoonandbosses$` de los mixins. **El comando es `/bloodmoon`** (`ComandoBloodmoonAndBosses.COMANDO`): corto, porque se escribe en el chat.

| Dato | Dónde | Cómo se pasa (`MigracionDeId`) |
|---|---|---|
| `config/mobspoderosos.json` | config | `migrarConfig`, lo primero de `onInitialize` → `config/bloodmoonandbosses.json` |
| `nivel`, `pendiente`, `afijo`, `invocaciones`, `jefe`, `furia` | cada mob | `migrarEntidad` en `ENTITY_LOAD` |
| `ultimo_jefe`, `ultima_estampida` | cada jugador | `migrarEntidad` en `ENTITY_LOAD` (también corre para los jugadores) |
| `luna_de_sangre`, `noche_sorteada` | el overworld | `migrarMundos` en `SERVER_STARTED` |
| Modificadores `mobspoderosos:nivel`, `afijo`, `jefe`, `furia` | atributos de cada mob | `renombrarModificadores` en `migrarEntidad` |
| Tags y tablas `mobspoderosos:*` de un datapack del dueño | datapack | **No se migran**: hay que mover el datapack a `data/bloodmoonandbosses/` |

## Los attachments viejos siguen registrados

**Cada attachment viejo está registrado para siempre** (`DE_ENTIDADES`, `DEL_MUNDO`), con el mismo codec que el nuevo. Fabric descarta al cargar un attachment cuyo id no conoce: si no estuvieran registrados, un mob de un chunk que nadie visita desde antes del cambio perdería su nivel al volver a cargarse. Por eso la migración es perezosa: **cada mob se pasa cuando se carga su chunk**, no todo el mundo al arrancar.

- **`migrarConfig` es lo primero que se llama de `MigracionDeId`**: así sus campos estáticos (los attachments viejos) se registran en `onInitialize`, antes de que cargue cualquier mundo.
- **`migrarEntidad` es el primer listener de `ENTITY_LOAD`**: `NivelDeMob.alCargar` y `JefeDeZona.alCargar` tienen que ver el nivel y la marca de jefe ya en el id nuevo. Si no, a un mob que ya tenía nivel le sortearían otro, y un jefe se quedaría sin barra.
- `Par.pasar`: si el mob ya tiene el attachment nuevo, gana el nuevo; el viejo se saca igual.

## Los modificadores

**Vanilla guarda los modificadores permanentes en el NBT con su id.** Un mob de antes trae `mobspoderosos:nivel` en la vida, el daño, la armadura… Si quedaran, seguiría igual de fuerte, pero cualquier `aplicar` posterior (una conversión, la furia) pondría los `bloodmoonandbosses:*` al lado y sumarían dos veces. `renombrarModificadores` recorre todos los atributos registrados y cambia cada modificador permanente con el namespace viejo por uno igual con el nuevo. Como se saca y se pone en el mismo momento, la vida actual no se recorta.

## Mythic Loot

Mythic Loot lee por id `bloodmoonandbosses:nivel`, `:jefe` y `:afijo` (sin depender de las clases de este mod) y detecta el mod con `isModLoaded("bloodmoonandbosses")`. Lee al morir el mob, cuando ya pasó por `ENTITY_LOAD`, así que siempre ve los ids nuevos. **Cambiar uno de esos ids o su tipo de valor rompe Mythic Loot.**

## Probado

- `unMobDelIdViejoConservaSuNivel`: un zombi guardado con los attachments y el modificador viejos, al volver a cargarse, tiene nivel 30, su afijo, la marca de jefe y 68 de vida máxima, sin nada viejo; el sorteo de la luna pasa en el overworld; la config se renombra.
- Con una copia del mundo de un servidor real (Server3, 3906 mobs con nivel): al forzar la carga de una zona, sus 385 mobs pasaron al id nuevo sin perder ninguno, sin restos del id viejo en esos chunks; los otros 3521 esperan a que se carguen.

## Ver también

- [Niveles](niveles.md)
- [Eventos y afijos](eventos.md)
- [Configuración](configuracion.md)
