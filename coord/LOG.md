# LOG — последние checkpoint-и

Подробная история сохранена в Git; здесь оставлять не больше десяти актуальных коротких итогов.

- 2026-10-03: M5 Arena3 на чистой карте построила один помост, Queen побеждена, NORTH выиграл с 6 выжившими; повторный старт N/R отдельно не проверен.
- 2026-10-03: P4 Full/Lite FPS сравнение оказалось недействительным: HUD показывал Lite при Full-конфиге.
- 2026-10-03: Единственный FAIL объединённого selftest был cavalry 0/4 в грязном мире с 40 лошадьми; чистый прогон Claude дал 4/4 на t=150.
- 2026-10-03: X7 clean-world build+selftest с четырьмя цветочными/подлесочными слоями прошёл RESULT OK; reference validator тоже OK.
- 2026-10-03: Первый естественный forest visual gate оказался слишком редким; свежие чанки дали 3 allium, 5 azure. Причина: `WORLD_SURFACE_WG` поднимал исходную высоту patch до кроны.
- 2026-10-03: X7 исправил flora heightmap на `MOTION_BLOCKING_NO_LEAVES`; свежий forest дал 56 fern, 16 oxeye, 13 allium, 9 azure, `RESULT: OK`.
- 2026-10-03: Meadow и forest Full/Lite выдавали одинаковые счетчики; meadow кадр был чёрным. Высоту камеры и задержку захвата увеличили, повторная компиляция ожидает build lock.
- 2026-10-03: Тестовая громкость run-visual выставлена в 0; пользовательские настройки не менялись.
- 2026-10-03: По X3 замечен старый пустой кадр `50_dt_undead_crypt`; сценарий не сообщал, если структура не нашлась, нужен fresh-world диагностический прогон.
[2026-10-03 05:33 МСК] X3 BossHook theme pass compile + fresh server selftest RESULT OK; minionretarget OK; crypt generated. Visual theme/boss shot remains pending. Fresh selftest exposed runtime WORLD_SURFACE_WG warnings in BossHook.site and OCEAN_FLOOR_WG warnings needing attribution/owner handoff. Gradle project cache was cold; current run downloaded 786 MiB and completed in ~9m.
[2026-10-03 06:04 МСК] X3 fresh dedicated gate OK; safe BossHook heightmap removes WORLD_SURFACE_WG warnings and places 8 halls. C11 selftest OK; visual altar matching fixed. Visual RESULT OK but screenshot QA rejected due stale saved boss/entities in reused quickplay world; archived world, next run requires a fresh clean save. Latest perf gate 240 combat units 49.9ms average / 76.2ms worst; record for weak-PC optimization.

### 2026-10-03 06:13 МСК — X3 fresh save
Fresh clean visual save; `runVisualTest` exit 0, `RESULT: OK`, correct crypt/altar match and real boss spawn. Manual QA: camera 50 black, chamber still visually too empty/box-like; X3 remains doing. BUILD.lock released, then observed Claude chronicle busy.

### 2026-10-03 06:47 МСК — X3 relief/camera pass
`build runSelfTest`: EXIT 0, RESULT OK, eight BossHook halls; 240-unit average 49.9 ms/worst 68.0 ms. Fresh clean filtered visual run EXIT 0/RESULT OK. Camera 50 moved below ceiling, carried effects cleared, crypt hall received bone accents, cross ribs, altar portal, and more lamps. New shots are readable; X3 remains open until full imported dungeon/real combat QA. OCEAN_FLOOR_WG runtime errors persist in the server-owned Prefab path and were reported to Claude.
[2026-10-03 07:19 МСК] M3 live Queen QA: build+selftest OK; fresh visual repro found 4/4 crawlers still targeting dead Soldier after 61 ticks, navDone=true. Exact logs and fix request sent to Claude in CHAT #29; BUILD.lock free.
- 2026-10-03 08:11: clean selftest + live X3 Crypt Lord (10 AI soldiers, phases 2/3, synced sweep/slam) and M3 Queen brood retarget/movement passed; outdoor arena containment remains open.
