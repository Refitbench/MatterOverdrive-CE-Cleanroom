![](BannerBlinks.gif)

![Discord](https://img.shields.io/discord/703124643149643818?logo=discord&link=https%3A%2F%2Fdiscord.gg%2FsgQxDJdrnY) 

# Matter Overdrive - Maintained by Refitbench

## About
Matter Overdrive is a Minecraft mod inspired by the popular Sci-fi TV series Star Trek. It dwells in the concept of replicating and transforming one type matter into another.
Although it may seem overpowered, Matter Overdrive takes a more realistic approach and requires the player to build a complex system before even the simplest replication can be achieved.

## Refit Differences
* Optimized every machine block, remove redundant code, cache states for huge idle overhead gains.
* Optimized Matter Pipes, instead of checking every tick for neighbors, we cache it.
* Removed Matter Pipe internal buffers, transfers are direct.
* Vastly improved and optimized the Anomaly, cache states and queue lookups (configurable).
* Gravitational Stabilizer now requires power (Optional) and accepts upgrades and outputs stats via redstone.
* Fusion Reactor now shows valid anomaly positions instead of just center ghost block.
* Removed items and server logic for incomplete features. (StarMap / Grav Generator).
* Completed the WIP Tritanium Crate, its now a single block and accepts dye.
* Lots of config options for balance and tinkering by users and modpacks.
* Register items properly, so they show up in the same place, as it should have been.
* Better organize items and blocks to make a bit more logical sense.
* Implemented HEI's collapsible group support.
* Pattern Monitor allows managing tasks from networked replicators.
* Replication requests can be infinite (-1), configurable by server host.
* Fixed a fair amount of bugs and exploits. (Hopefully didn't add any new ones.. probably did, report them!).
* New Replication effects and sounds.
* New Recharge Station effects and sounds.
* New Inscriber effects and sounds.
* Improved Android performance, cache unlocks and power states.
* Space Time Equalizer now has (optional) bauble support.

## Switching from MO:CE or MO:Legacy to MO:Refit
If you are coming from MO:CE and have no current setups built, switch is fairly simple, you will just see some incomplete items removed from the save. If you DO have setups, the following is important.
* Always back up your save!
* If you have fusion reactors with stablizers, they will by default require power now, if left as is, your stablizers will stop working!
* Some machines may require picking up and replacing.
* Old tritanium crates should get converted to the new tile, check them after upgrade, report any issues.

## Mod-Links
* [Discord](https://discord.gg/sgQxDJdrnY)

## Features
* [Matter Scanner](https://mo.simeonradivoev.com/items/matter_scanner/), for scanning matter patterns for replication.
* [Replicator](https://mo.simeonradivoev.com/items/replicator/), for transforming materials.
* [Decomposer](https://mo.simeonradivoev.com/items/decomposer/), for breaking down materials to basic form.
* [Transporter](https://mo.simeonradivoev.com/items/transporter/), for beaming up.
* [Phaser](https://mo.simeonradivoev.com/items/phaser/), to set on stun.
* [Fusion Reactors](https://mo.simeonradivoev.com/fusion-reactor/) and [Gravitational Anomaly](https://mo.simeonradivoev.com/items/gravitational_anomaly/), for that sweet energy.
* Complex Networking for replication control.
* Star Maps, with Galaxies, Stars, and Planets.
* [Androids](https://mo.simeonradivoev.com/android-guide/), become an Android and learn powerful RPG like abilities, such as Teleportation and Forcefields.


![Matter Overdrive Blocks and Items](https://mo.simeonradivoev.com/wp-content/uploads/2015/05/main_screenshot.png)

## Issues:
https://github.com/Refitbench/MatterOverdrive-CE-Cleanroom/issues
