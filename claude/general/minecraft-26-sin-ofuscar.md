# Minecraft 26.x sin ofuscar

[← Hub](../../CLAUDE.md)

**Desde Minecraft 26.1 el jar viene sin ofuscar y Fabric usa los nombres oficiales de Mojang.** Yarn dejó de mantenerse. Casi todos los tutoriales y respuestas de foros anteriores a 2026 usan nombres de Yarn (`PlayerInventory`, `main`, `ServerPlayerEntity`…) que acá no existen.

| Yarn (viejo) | Mojang (este proyecto) |
|---|---|
| `PlayerInventory` | `Inventory` |
| `ServerPlayerEntity` | `ServerPlayer` |
| `PersistentState` | `SavedData` |
| `Identifier` (Yarn) / `ResourceLocation` (Mojang viejo) | `Identifier` |

**Tampoco confíes en lo que "sabés" de 1.21.x:** entre versiones cambian firmas. Ejemplos que afectaron a este mod:

| Lo que esperarías | Cómo es en 26.3 |
|---|---|
| `EntityType.ZOMBIE` | **`EntityTypes.ZOMBIE`**: las constantes se mudaron a otra clase; `EntityType` sigue siendo el tipo |
| `entityType.is(tag)` | No existe. `entity.is(TagKey)`, que viene de la interfaz `TypedInstance` (`builtInRegistryHolder().is()` está deprecado) |
| `MobSpawnType` / `SPAWN_EGG` | `EntitySpawnReason`, y los huevos son `SPAWN_ITEM_USE` |
| `.requires(src -> src.hasPermission(2))` | `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` |
| El spawn del mundo con `getSharedSpawnPos()` | `MinecraftServer.getRespawnData().pos()` (un `LevelData.RespawnData`) |
| `com.llamalad7.mixinextras.injector.WrapWithCondition` | Deprecado: usá el de `…injector.v2` |
| `Slime extends Mob` | `Slime extends AbstractCubeMob extends AgeableMob`, en el paquete `monster.cubemob` |

- La carga y el guardado usan `ValueInput` / `ValueOutput`, no `CompoundTag` directo.

## Verificar antes de escribir un mixin

Un mixin con un nombre o descriptor mal puesto **compila igual y explota recién al cargar la clase** en runtime. Antes de escribir uno, mirá la clase real:

```bash
J=.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/minecraft-merged-*-26.3.jar
JP="/c/Program Files/Microsoft/jdk-25.0.4.101-hotspot/bin/javap"

"$JP" -p -cp $J net.minecraft.world.entity.Mob                       # campos y firmas
"$JP" -c -p -cp $J net.minecraft.world.entity.LivingEntity \
  | awk '/public void die\(/,/^$/'                                    # bytecode de un método
```

El jar aparece después del primer `gradlew compileJava`. Con `-c` se ve el bytecode, que alcanza para saber a quién llama un método y en qué orden. Así se encontró que `Zombie.finalizeSpawn` pone el equipo **después** de `super`, y que `Mob.convertTo` dispara `MOB_CONVERSION` antes de `addFreshEntity` (ver [niveles](../mod/niveles.md)).

Para las clases de Fabric API (eventos, attachments, gametest), los jars están en `~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/`. El jar `-sources` de cada módulo trae el código de sus mixins: así se ve **dónde** dispara cada evento.

## Ver también

- [Entorno Java y Gradle](entorno-java-gradle.md)
- [Niveles](../mod/niveles.md)
