# LOCKS
(пусто)

tools/modelgen/units.py | codex | X2
tools/modelgen/run.py | codex | X2
src/main/java/com/alkimor/regnum/client/model/ | codex | X2 generated
src/main/resources/assets/regnum/textures/entity/ | codex | X2 generated
src/main/java/com/alkimor/regnum/client/render/UnitRenderer.java | codex | X2
src/main/java/com/alkimor/regnum/client/screen/ScienceScreen.java | codex | X1 visual verification
src/main/java/com/alkimor/regnum/client/ClientSelfTest.java | codex | X1 visual verification
src/main/java/com/alkimor/regnum/client/render/RegnumHumanoidModel.java | codex | X2
src/main/java/com/alkimor/regnum/client/render/UnitItemLayer.java | codex | X2
src/main/java/com/alkimor/regnum/client/ClientVisualTest.java | codex | X2 visual regressions
src/main/java/com/alkimor/regnum/client/screen/QuestJournalScreen.java | codex | P3 client quest journal
src/main/java/com/alkimor/regnum/client/render/QuestTrackerOverlay.java | codex | P3 client quest HUD
src/main/java/com/alkimor/regnum/client/render/SoldierPoseAnimator.java | codex | P3 procedural soldier poses
src/main/java/com/alkimor/regnum/client/render/RegnumHumanoidModel.java | codex | P3 animation integration
src/main/resources/data/regnum/worldgen/template_pool/ | codex | X3 jigsaw resource reference audit/fix
tools/validate_worldgen_refs.py | codex | X3 jigsaw resource reference validator
src/main/resources/data/regnum/worldgen/structure_set/dt/ | codex | X3 missing structure placements audit/fix
src/main/resources/data/regnum/worldgen/structure/dt/nether_keep.json | codex | X3 restore valid Nether Keep jigsaw root
tools/generate_nether_keep.py | codex | X3 create unique Nether Keep template
src/main/resources/data/regnum/structure/dt/nether_keep/ | codex | X3 custom Nether Keep NBT
src/main/resources/data/regnum/structure/dt/illager_hideout/ | codex | X3 repair missing loot table refs
src/main/resources/data/regnum/tags/worldgen/structure/dt/ | codex | X3 remove unresolved structure refs
src/main/resources/data/regnum/worldgen/configured_feature/dt/azure_bluet_patch.json | codex | X7 subtle overworld wildflower patch
src/main/resources/data/regnum/worldgen/placed_feature/dt/azure_bluet_patch.json | codex | X7 subtle overworld wildflower patch
src/main/resources/data/regnum/neoforge/biome_modifier/azure_bluet_patch.json | codex | X7 add flower patch to selected vanilla biomes
coord/DECISION_anim.md | codex | P3 decide animation library/dependency strategy
src/main/java/com/alkimor/regnum/core/RegnumClientConfig.java | codex | P4 client performance telemetry setting
src/main/java/com/alkimor/regnum/client/render/ClientPerformanceOverlay.java | codex | P4 client FPS/frame-time measurement
src/main/java/com/alkimor/regnum/client/render/GlowLayer.java | codex | P4 skip optional emissive pass in Lite
tools/gen_textures.py | codex | Mine crawler chitin-plate item texture generator
src/main/resources/assets/regnum/textures/item/chitin_plate.png | codex | Mine crawler chitin-plate icon

src/main/java/com/alkimor/regnum/client/render/BossRenderer.java | codex | B1 bigger bosses
src/main/java/com/alkimor/regnum/client/render/BossVisualScale.java | codex | B1 shared multiplier
src/main/java/com/alkimor/regnum/client/render/RegionBossRenderers.java | codex | B1 bigger bosses
src/main/java/com/alkimor/regnum/client/render/MineCreatureRenderers.java | codex | B1 bigger queen
src/main/java/com/alkimor/regnum/client/ClientVisualTest.java | codex | B1 bigger boss frames

src/main/java/com/alkimor/regnum/client/model/CrawlerQueenModel.java | codex | X8 synchronized attack poses

src/main/java/com/alkimor/regnum/client/BattleShowcase.java | codex | B2 real battle demo
tools/Show-Regnum.ps1 | codex | B2 battle launcher
ПОСМОТРЕТЬ_БОЙ.bat | codex | B2 battle launcher
coord/C10_DODGE_ACCEPTANCE.md | codex | C10 demo acceptance criteria
src/main/java/com/alkimor/regnum/client/BattleDodgeTelemetry.java | codex | C10 live movement verification
src/main/java/com/alkimor/regnum/client/render/ChitinGait.java | codex | X8 ground-aligned gait
src/main/java/com/alkimor/regnum/client/model/CrawlerModel.java | codex | X8 brood gait wrapper
tools/modelgen/engine.py | codex | X8 chitin material
tools/modelgen/mine_creatures.py | codex | X8 tapered carapace
tools/mc_convert/dt_import.py | codex | P7 missing import support
src/main/resources/data/regnum/enchantment/dt/ | codex | P7 restore imported definitions
src/main/resources/data/regnum/item_modifier/dt/ | codex | P7 restore loot reference
tools/validate_import_support.py | codex | P7 required references validation
tools/encode_battle_capture.py | codex | B2 actual frame sequence
src/main/java/com/alkimor/regnum/client/render/AdaptedModelAnimator.java | codex | X8 LLibrary keyframe adaptation
src/main/java/com/alkimor/regnum/worldgen/ImportedAnchorProcessor.java | codex | P7 transformed structure decoration anchors
tools/repair_imported_anchors.py | codex | P7 scoped processor wiring
src/main/resources/data/regnum/worldgen/processor_list/dt/ | codex | P7 imported anchor wiring
src/main/resources/data/minecraft/tags/enchantment/exclusive_set/repair.json | codex | P7 imported repair exclusivity support
src/main/java/com/alkimor/regnum/client/render/CryptLordAnimatedModel.java | codex | X8 visible boss attacks
src/main/java/com/alkimor/regnum/client/render/CryptLordRenderer.java | codex | X8 animated model
src/main/java/com/alkimor/regnum/animation/BossAnimationState.java | codex | shared visual state contract
src/main/java/com/alkimor/regnum/client/render/RegionalHumanoidAnimatedModel.java | codex | X8 regional casts
src/main/java/com/alkimor/regnum/client/render/ForgemasterAnimatedModel.java | codex | X8 hammer animation
.gitignore | codex | bootstrap shared local Git repository
