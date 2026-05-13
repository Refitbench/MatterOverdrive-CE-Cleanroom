# IDEAS / TODO
- Rework android AI?
-   "borg" might be passive unless provoked outside of its home range.
-   Currently androids without home attack on sight.
-   Buildable android companion?

- Fix / Overhaul spacetime accelerator

- Add a way to clear/edit pattern disks.
-   Maybe recraft to clear or clear from a submenu on monitor or storage.

- Finish the Cyberware integration.

- Future Atomic Science integration?

- Overhaul the Transporter.
-   Currently lacks effects that legacy had.
- Implement a Comm Badge and Teleporter Controller, the comm badge can link to the controller and allow for teleportation back home from a configurable distance.
-   The Teleporter Controller could also lock onto other things and teleport it, or increase the range with a manual lock. (Another player required).

- Implement a HoloDeck, proxy for interdim travel with approved dim list?

- Balance / Config for androids.
-   Change how long the shield last, or how long its cooldown is.
-   Maybe require artifacts for more than just the oxygen upgrade?
-   Reverse the arrows in upgrade tree, they are backwards from what one would expect.

- Rework the Portable Decomposer?

- Self-Sealing stem bolts (Crafting ingredient?)

- A "Bat'leth" type weapon which is totally not a Bat'leth.

- Tricorder (Medical) and/or Hypospray.

- Finish WIP items
-   "piramid" - just a texture, looks to be a drive like object?
-   "suppy_crate" - just a texture, looks to be some supply like crate.
-   "box" - just a texture, similar to supply crate more simple looking.
-   Star Map - Frameworks exist, but alot of missing features and no roadmap for it.
-       Androids items to be sent out for xyz reason? A use for extra parts.

- Potential for implementing a power provider and matter provider block to attach to networks.

# OPTIMIZATIONS
- Mixin temperature checks on TAN to avoid any calculations on surpress mode.
- Defer thirst updates on TAN to every 39t and calculate drift to bill RF cost.

# DEBUG / TESTING

# KNOWN ISSUES / BUGS
- Wrenching some blocks (decomposer) does not return properly.
- Blocks picked up by wrench have strange tooltips.
- As an android when in low power condition, you change back to human, the slowness is still applied without limit, reload fixes.