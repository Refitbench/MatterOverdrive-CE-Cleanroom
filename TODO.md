# IDEAS / TODO
- Finish the Tritanium Create New and replace the old create in worldgen and registration.
- Finish or Remove/Disable Dimensional Pylon (Dimensional Generator).
- Implement a Comm Badge and Teleporter Controller, the comm badge can link to the controller and allow for teleportation back home from a configurable distance.
-   The Teleporter Controller could also lock onto other things and teleport it, or increase the range with a manual lock. (Another player required).
- Implement a HoloDeck, proxy for interdim travel with approved dim list.
- Balance / Config for android shields.
-   Change how long the shield last, or how long its cooldown is.
- Implement a "infinite" replication request.

- Add Scanner / Analyzer chance to fail. 
- Rework the Portable Decomposer.


# OPTIMIZATIONS
- Cables seem to be doing connection scans, we should only update this on a side change, cache!

# DEBUG / TESTING
- Test matter and upgrades updating due to #346b739
-   Applies to Replicator, Decomposer, and SpaceTimeAccelerator.

# KNOWN ISSUES / BUGS
- When the fusion reactor is at max power and draining slightly, the 100% will flicker, solution is to switch to rounding after 1%
