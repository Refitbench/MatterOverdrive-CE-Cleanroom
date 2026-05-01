# Changelog
## [1.0.6] - 2026-04-30
### Fixed
- Tritanium crates not dropping items when destroyed. Now saves on wrench pickup, otherwise drops them.
- Tritanium crates now accept dye through oreDict entries, modded dye should now work.
- Tritanium crates preserve dye color when picked up with a wrench.
- Added missing shift tooltip to tritanium crate.

## [1.0.5] - 2026-04-30
## Fixed
- Fixed FML version check (For real this time I swears).
## Changed
- Updated CE URL's to Refitted URL.

## [1.0.4] - 2026-04-30
### Fixed
- Fixed entity registration for mod integration (DeepMobEvolution). Moved from preInit to postInit.

## [1.0.3] - 2026-04-28
### Fixed
- Fixed natural and player spawned androids never attacking.
- Fixed some anomaly render compatability with shaders.

### Changed
- Switched from github based update URL to a curseforge related API, get latest upload.

## [1.0.2] - 2026-04-27
### Fixed
- Corrected the project and update URLs.
- An attempt of semi-automated FML changelog / update json generation.

### Changed
- Made baubles a soft dependancy. SpaceTimeEqualizer default chestpiece with optional bauble.

## [1.0.1] - 2026-03-31
- First build under Refitbench.

### Added
- This is a default template changelog that follows the [KeepAChangelog Convention](https://keepachangelog.com/en/1.1.0/)