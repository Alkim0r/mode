# Regnum — контекст для Codex (копия из Project «mode», 02.10.2026)

Мод Minecraft 1.21.1 NeoForge 21.1.x, mod_id `regnum`, пакет `com.alkimor.regnum`. UI только на русском (en_us = копия ru_ru). Мод приватный, для друзей; планка «10 из 10». Авторские права не важны: можно брать текстуры/модели/схемы из других модов (файлы модов в `_refs`; из облака Mojang/Modrinth/CurseForge недоступны — скачивает пользователь).

## Правила
- Источник истины — эта папка. Синхронизацию из облака Claude делает через `_sync.zip` + `_apply_sync.py`; свои файлы не затирать — см. coord/PROTOCOL.md и LOCKS.md.
- Генерируемое руками не править: `client/model/*`, `kingdom/CultureWeapons.java`, ресурсы из `tools/*.py` — менять генератор (`gen_data.py`, `gen_textures.py`, `modelgen/run.py`, `gen_armor.py`, `gen_kit.py`, `gen_weapons.py`).
- Сборка: `build-watch.bat` реагирует только на `_rebuild.flag`; вместе кладут `_selftest.flag` / `_visualtest.flag` / `_clienttest.flag`. Итог — build.log: `EXIT_CODE=0`, `[REGNUM-SELFTEST] RESULT: OK`. Визуальный тест открывает окно Minecraft — только по согласованию с пользователем (при закрытой игре `python3 _reset_visual.py`).
- Данные данжей `data/regnum/**/dt` и схемы `structure/<культура>` только на ПК; НИКОГДА не удалять целиком `worldgen`/`tags`.

## Постройки и данжи
- Конвертер `tools/mc_convert` (MineColonies .blueprint → structure NBT). Схемы: `data/regnum/structure/<культура>/{craftsmanship,education,fundamentals,military,mystic,decorations,walls}`.
- `kingdom/Prefab`, `WallKit`, `Capital`, `WallJob`, `CityBuildings` (тип здания → схема).
- Данжи Dungeons and Taverns 4.4.4 (именно MC 1.21.1) импортированы как `regnum:dt/*`, `tools/gen_dt_sets.py`; `dungeon/BossHook` вырезает залу босса в центре bounding box и ставит алтарь+сундук (может быть грубо — искать пустое место внутри структуры).
- Наши простые данжи (crypt, sunken_shrine, forge_fortress, sand_tomb, bandit_camps) старые — задача X3: переделать/заменить.

## Уже есть
Навыки/травмы/медицина, горн, города/экономика/набеги, армия (11 типов солдат, строй, мораль, командиры), 4 босса, торговля, династия, культуры (6), оружие/броня культур, территория, стены, соседние королевства (дипломатия, осада, караваны, союзы), события, деревни-вассалы, наука (25 технологий), шпионаж, кампания (`/regnum quest`).

## Клиент ↔ сервер
Сетевые пакеты в `core/network/*` (например `CityInfoPayload` с `int[] byType`). Для экрана науки/командиров (X1) нужны новые payload — согласовать с Claude через INBOX (общие файлы KingdomModule/RegnumCommands/network — через LOCKS.md).

## Уроки
Длинные команды (E2BIG) — писать файлы; device_bash ~180 с; скриншоты >900 px не стейджатся; selftest max-tick-time=-1; для далёких чанков в визуальных тестах ждать ≥500 тиков.
