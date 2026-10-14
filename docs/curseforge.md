# Bloodmoon & Bosses

Hostile mobs get **levels from 1 to 100**: the farther from spawn and the older the world, the more health, damage and armor they have, and the better their loot. Some spawn as **champions** with an affix, there are **stampedes**, **blood moons** and **zone bosses**. The level and health show in the name: **"Zombie [Lv 7] ❤ 34/40"**.

## Levels

| Level (where) | Zombie health (base 20) | Damage (base 3) | Extra armor | Loot | Experience |
|---|---|---|---|---|---|
| 1 (at spawn) | 21.6 | 3.1 | +0.1 | normal | ×1.05 |
| 20 (~1,900 blocks) | 52 | 5.4 | +2.4 | ×2 | ×2 |
| 50 (~4,900 blocks) | 100 | 9 | +6 | ×4 | ×3.5 |
| 80 (~7,900 blocks) | 148 | 12.6 | +9.6 | ×6 | ×5 |
| 100 (max, ~9,900 blocks) | 180 | 15 | +12 | ×7 | ×6 |

- **+1 level every 100 blocks** from world spawn. The **Nether** counts each block as 8 and adds 20 levels; the **End** adds 40.
- **Over time:** every 5 in-game days, newly spawned mobs get +1 level, up to +20 at day 100.
- **The name color shows the tier:** gray, green, blue, purple, gold.
- **Gear:** zombies, skeletons and piglins can spawn with extra armor (leather → chain or gold → iron → diamond → netherite as the level rises), enchanted for their level. Mobs that burn in daylight don't get helmets, so the sun still works.
- **Loot:** every 16 levels the mob drops its vanilla loot one more time (up to 6 extra rolls), **only if you killed it**: farms are unaffected. From level 20, 50 and 80 they can also drop items from rare, epic and legendary tables (golden apples, diamonds, enchanted books, netherite scrap, upgrade templates…).
- **Spawner mobs have no level**, so mob farms keep working as in vanilla. Bosses (dragon, wither, elder guardian, warden) are unchanged.

## Champions and affixes

One in 20 mobs is a **champion**: 10 levels above its zone and with an **affix** in its name ("Zombie [Lv 27] [Vampire]"). Each affix has its own particles, so you can spot it from afar.

| Affix | What it does |
|---|---|
| **Swift** | Runs 30% faster |
| **Vampire** | Heals half the damage it deals (arrows too) |
| **Explosive** | Explodes like a creeper a second and a half after dying (no block damage) |
| **Summoner** | Calls two reinforcements of its kind when hurt, up to 3 times |
| **Armored** | +6 armor and half knockback |
| **Poisonous** | Its hits give Poison II for 5 seconds |
| **Freezing** | Its hits give Slowness II for 3 seconds |
| **Teleporter** | When hit, half the time it appears right next to you |

## Events

- **Stampede:** at night, outdoors, in a zone of level 10 or more, a horn sounds and 6 to 16 mobs charge at you from the same side, led by a champion. **Kill at least half to get a rare loot roll** (epic from level 50, legendary from 80) straight into your inventory.
- **Blood moon:** on full moon nights there's a 50% chance the moon turns to blood. That night, overworld mobs spawn with **+10 levels** and half of them come with a companion. Sleeping skips it.
- **Zone boss:** in any dimension, in zones of level 15 or more, a boss can appear nearby: 20 levels above the zone, **triple health**, an affix, 3 escorts, a boss bar and a purple glow you can see through walls. At half health it **enrages** (faster, stronger, more escorts), and every 10 seconds it uses an ability (a shockwave that knocks you back, or a pull that drags ranged fighters in). **It always drops a rare loot roll, epic or better.**

## With the mod on the client (optional)

- During a blood moon the moon turns red and bigger, and the sky, fog and clouds turn red.
- Zone bosses glow purple (pulsing red when enraged) and champions take the color of their affix.
- With **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)**, you see mobs' level and health from up to 48 blocks away.

None of this is required: without the mod on the client everything works the same, using vanilla boss bars, titles and sounds.

## Commands

Commands are in Spanish. All but `zona` are for operators:

| Command | What it does |
|---|---|
| `/bloodmoon zona` | Shows the mob level in your zone (anyone can use it) |
| `/bloodmoon invocar <entity> <level> [affix]` | Spawns a mob with that level |
| `/bloodmoon evento estampida \| jefe \| luna_de_sangre` | Starts a stampede, a zone boss or a blood moon |
| `/bloodmoon recargar` | Reloads the config |

## Configuration

Almost everything is configurable in `config/bloodmoonandbosses.json`: blocks per level, max level, how each stat scales, champion and affix chances, every event, loot tables… It reloads with `/bloodmoon recargar`, no restart needed. With **[Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)** you can also edit it in-game, with a name and a tooltip for every option. Loot tables and the mob lists for events and bosses are tags you can change with a datapack.

## Installation

All the logic runs on the server: **players don't need to install anything**. For singleplayer or LAN, install it on the client that opens the world.

Requires **Minecraft 26.3**, **Fabric Loader 0.19.5+**, **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)** and **Java 25**.

## Works well with

- **Hero's Forge**: the defaults are balanced against its stats. With the expected gear and points for each zone, a zombie takes 5 or 6 hits anywhere in the world. Without it, level 100 zones are for the few.
- **Mythic Loot**: leveled mobs drop weapons and armor with affixes based on their level, and zone bosses always drop an epic or better item, sometimes with an affix of their species.
- **Soul Link**: no conflicts.

License: MIT. Feel free to use it in modpacks.
