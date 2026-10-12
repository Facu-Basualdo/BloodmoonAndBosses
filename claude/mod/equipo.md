# Equipo

[← Índice Mod](INDICE.md)

Síntomas que esto evita: **los vex empiezan a soltar su espada de hierro**, **los zombis con casco sobreviven de día y llenan el límite de mobs**, **un pillager tiene armadura que nadie ve**, **una espada encantada por vanilla sale con dos encantamientos incompatibles**, o **la netherita encantada sale gratis matando mobs de nivel 80**.

## Cuándo se pone

**`EquipoPorNivel.equipar(mob, nivel)` corre sólo cuando el mob aparece**, desde `NivelDeMob.aparecer` (que es `aplicar` + `equipar`):

| Camino | ¿Equipo? | Por qué |
|---|---|---|
| `NivelDeMob.alCargar` (`ENTITY_LOAD` con `PENDIENTE`) | Sí | Vanilla ya le puso su equipo en `finalizeSpawn`: el mod completa y encanta sobre eso (ver [niveles](niveles.md)) |
| `/bloodmoon invocar` | Sí | Pasa por `finalizeSpawn` con `COMMAND` y después por `aparecer` |
| `NivelDeMob.alConvertir` | **No** | `ConversionType` copia el equipo del mob viejo al nuevo: equiparlo de nuevo le sumaría piezas y encantamientos en cada conversión |
| `NivelDeMob.aplicar` a secas | No | Lo usan los tests de estadísticas y las conversiones |

**No es idempotente:** cada llamada sortea de nuevo las ranuras vacías. Por eso no puede ir en `aplicar`, que sí se llama más de una vez.

## Armadura: sólo para usan_equipo

**La armadura nueva va sólo a los mobs del tag `bloodmoonandbosses:usan_equipo`** (`EquipoPorNivel.USAN_EQUIPO`): zombis, husks, ahogados, aldeanos zombi, piglins zombificados, esqueletos, strays, bogged, parched, esqueletos del Wither, piglins y piglins brutos. Son los que **dibujan** la armadura: los illagers y el vex no tienen capa de armadura en el modelo, así que se la pondría invisible (y igual protegería).

- Por cada ranura de armadura **vacía**, chance `chanceArmaduraExtra.en(nivel)` (0,75 % por nivel, tope 50 %, desde el nivel 67). Lo que puso vanilla no se toca.
- El material sale de `EquipoPorNivel.material(nivel, random)`: un rango cada `nivelesPorMaterialDeArmadura` (20) niveles, con tope en el último.

| Rango | Material |
|---|---|
| 0 (1–19) | cuero |
| 1 (20–39) | cadena **u** oro, uno por mob (no piezas mezcladas) |
| 2 (40–59) | hierro |
| 3 (60–79) | diamante |
| 4+ (80+) | netherita |

- **Sin casco para `EntityTypeTags.BURN_IN_DAYLIGHT`** salvo que `cascoParaLosQueSeQueman` sea `true`. Vanilla protege del sol a quien tiene algo en la cabeza: con casco, zombis y esqueletos sobreviven de día, ocupan el límite de mobs y el ciclo día/noche deja de importar. El casco que vanilla puso por su cuenta se queda.

## Encantamientos

**Se encanta todo el equipo que tenga el mob** (manos y armadura, `CON_EQUIPO`), esté o no en `usan_equipo`: la ballesta del pillager, el hacha del vindicador o la espada del vex también escalan. Es la única forma de que escale el daño a distancia: las flechas no usan `ATTACK_DAMAGE`, y con un arco esto da Poder.

- Poder `(int) poderDeEncantamiento.en(nivel)` (0,3 por nivel, tope 30 en el nivel 100). **Con poder 0 (nivel 1) no se encanta nada.**
- `EnchantmentHelper.enchantItem(random, item, poder, registryAccess, Optional<HolderSet>)` con **`EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT`**, el mismo tag que usa vanilla: sin Reparación, sin maldiciones ni otros de tesoro.
- **Lo ya encantado (por vanilla) se saltea.** `enchantItem` llama a `selectEnchantment`, que no mira lo que el ítem ya tiene: sobre una espada con Filo podría sumar Golpeo, que son incompatibles.
- `enchantItem` encanta el `ItemStack` en el lugar. Después se llama a `setItemSlot` con el mismo stack, como hace vanilla en `enchantSpawnedEquipment`. `onEquipItem` no suena porque el stack viejo y el nuevo son el mismo.

## Chance de soltar el equipo

**`subirChancesDeSoltar` suma `chanceSoltarEquipo.porNivel × nivel` a la chance que ya tenía la ranura, con tope `chanceSoltarEquipo.tope`** (`Escala.sobre`). Con los valores por defecto: 0,085 de vanilla + 0,2 % por nivel, tope 0,25 (desde el nivel 83).

| Caso | Qué pasa | Por qué |
|---|---|---|
| Chance actual **0** | No se toca | Vanilla la pone en 0 a propósito: el vex nunca suelta su espada. Subirla regalaba espadas de hierro (lo encontró un gametest) |
| Chance actual ≥ tope (2.0 de `setGuaranteedDrop`, lo que el mob levantó) | No se toca | `nueva > actual` es falso: la chance nunca baja |
| Ítem en el tag `bloodmoonandbosses:equipo_valioso` | No se toca (queda en 0,085) | Diamante, netherita y el tridente. Si no, en nivel 80+ la armadura de netherita encantada sale en uno de cada cuatro mobs |
| El resto | Sube | |

- Las chances se leen con `mob.getDropChances().byEquipment(slot)` y se escriben con `setDropChance`. En 26.3 viven en el record `DropChances`, que vanilla guarda en el NBT: sobreviven al recargar el chunk.
- **Vanilla sólo suelta el equipo si lo golpeó un jugador**, y con chance ≤ 1 lo suelta dañado. Las dos cosas ayudan al balance sin que el mod haga nada.
- Más equipo también da más experiencia: `Mob.getBaseExperienceReward` suma 1–3 por cada pieza con chance de soltarse ≤ 1.

## Ver también

- [Niveles](niveles.md)
- [Botín y experiencia](botin.md)
- [Configuración](configuracion.md)
- [Tests: equipo](../tests/equipo.md)
