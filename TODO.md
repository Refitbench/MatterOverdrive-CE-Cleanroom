# IDEAS / TODO
- Fix / Overhaul spacetime accelerator

- Add a way to clear/edit pattern disks.
-   Maybe recraft to clear or clear from a submenu on monitor or storage.

- Finish the Cyberware integration.

- Overhaul the Transporter.
-   Currently lacks effects that legacy had.
- Implement a Comm Badge and Teleporter Controller, the comm badge can link to the controller and allow for teleportation back home from a configurable distance.
-   The Teleporter Controller could also lock onto other things and teleport it, or increase the range with a manual lock. (Another player required).

- Implement a HoloDeck, proxy for interdim travel with approved dim list.

- Balance / Config for androids.
-   Change how long the shield last, or how long its cooldown is.
-   Maybe require artifacts for more than just the oxygen?

- Add Scanner / Analyzer chance to fail and other configs.

- Rework the Portable Decomposer?

- Self-Sealing stem bolts (Crafting ingredient?)

- Maybe change the space-time equalizer to be a bauble?

- A "Bat'leth" type weapon which is totally not a Bat'leth.

- Tricorder (Medical) and/or Hypospray.

- Finish WIP items
-   "piramid" - dead texture, looks to be a drive like object?
-   "suppy_crate" - dead texture, looks to be some supply like crate.
-   "box" - dead texture, similar to supply crate more simple looking.

- Potential for implementing a power provider and matter provider block to attach to networks.

# OPTIMIZATIONS

# DEBUG / TESTING
Test jump assist when resetting back to human or out of power.

# KNOWN ISSUES / BUGS
- When the fusion reactor is at max power and draining slightly, the 100% will flicker, solution is to switch to rounding after 1%
- Wrenching some blocks (decomposer) does not return properly.
- Blocks picked up by wrench have strange tooltips.
- As an android when in low power condition, you change back to human, the slowness is still applied without limit, reload fixes.