# Regnum — справка-индекс проекта для агента

Актуальность среза: 2026-10-02. Это навигационная карта по исходникам, а не замена `AGENTS.md`, `coord/BOARD.md` или игровому руководству. Перед любой работой сначала читать `AGENTS.md`, `coord/PROTOCOL.md`, `coord/BOARD.md`, свой INBOX и `coord/LOCKS.md`.

## 1. Что это за проект

`Regnum: Королевства и Легенды` — крупный мод для Minecraft 1.21.1 на NeoForge 21.1.x (`mod_id = regnum`, Java-пакет `com.alkimor.regnum`). Его основной игровой цикл соединяет:

- развитие героя: атрибуты, навыки, фокус, перки, классы, черты, травмы и потребности;
- основание и развитие городов, экономика, склады, наука, дипломатия и династия;
- управление армией, ролями бойцов, строями, командирами и осадами;
- сложные сражения с телеграфируемыми атаками боссов и участием отрядов;
- процедурные и импортированные структуры, региональные данжи и сюжетные цепочки;
- клиентские экраны, HUD, собственные модели, процедурные и keyframe-анимации;
- высокий базовый уровень сложности при наличии клиентского Lite-режима.

Пользовательский текст должен быть только на русском. `en_us.json` сохраняется копией `ru_ru.json` ради Minecraft fallback.

## 2. Техническая основа и точки входа

| Назначение | Точка входа | Что важно |
|---|---|---|
| Главный класс мода | `src/main/java/com/alkimor/regnum/Regnum.java` | Регистрирует конфиги, 12 модулей, общие реестры, сеть, команды, SaveGuard и self-test. Порядок модулей значим. |
| Контракт модуля | `core/RegnumModule.java` | Каждый модуль имеет `id()`, русское `title()` и `init(IEventBus)`. |
| Общие реестры | `core/ModRegistries.java` | Blocks, items, entities, effects, attachments, components, structures, block entities и creative tab. |
| Общий конфиг | `core/RegnumConfig.java` | Survival, kingdom и difficulty: XP, травмы, города, набеги, scarcity, smart mobs, boss scaling, army LOD. |
| Клиентский конфиг | `core/RegnumClientConfig.java` | Lite, декор моделей, glow, performance overlay, дистанция детализации NPC. |
| Сеть | `core/network/RegnumNetwork.java` | Версия протокола `2`; UI snapshots идут S→C, действия экранов C→S. Обработчики всегда ставятся через `enqueueWork`. |
| Командный корень | `core/command/RegnumCommands.java` | Корень `/regnum`; многие подсистемы отдельно дописывают свои ветки через события регистрации команд. |
| Клиентская регистрация | `client/ClientSetup.java` | Layer definitions, entity renderers и встроенный natural resource pack (отключается Lite-режимом). |
| Серверный автотест | `core/SelfTest.java` | Интеграционные проверки в отдельном dedicated-server запуске. |
| Клиентские тесты | `client/ClientSelfTest.java`, `client/ClientVisualTest.java` | Runtime-проверки клиента и постановочные скриншоты/визуальные assertions. |

Сборка задаётся `build.gradle` и `gradle.properties`: Minecraft 1.21.1, NeoForge 21.1.252. Стандартный артефакт — `build/libs/regnum-0.1.0.jar`, но финальный jar нельзя считать релизом без одобрения владельца.

## 3. Карта модулей

Порядок подключения в `Regnum.MODULES`:

1. `survival/` — состояние героя, прогрессия, травмы, нужды, медицина, сезонность и классовые наборы.
2. `crafting/` — кузнечное ремесло, закалка, мастерская и свитки мастерства.
3. `kingdom/` — главное доменное ядро: города, армия, территория, экономика, дипломатия и кампания.
4. `dungeon/DungeonModule` — крипта, Морграт, алтари и базовые dungeon pieces.
5. `dungeon/RegionsModule` — региональные структуры и боссы: болото, кузня, пустынная гробница.
6. `wanderers/` — странники, характеры, диалоги и карты целей.
7. `trade/` — рынки, торговцы, караваны, цены и торговое состояние мира.
8. `dynasty/` — династия игрока и наследование.
9. `combat/` — умные мобы, парирование и дефицит добычи.
10. `armor/` — культурные комплекты брони.
11. `story/` — квесты, репутация, катсцены, ночная армия и optional external bosses.
12. `mine/` — шахтные ползуны, Королева ползунов и хоррор-события.

Пакеты по масштабу на дату среза: `client` ~135 Java-файлов, `kingdom` ~77, `survival` ~44, `dungeon` ~26, `core` ~23. Большой размер `client/model` в основном объясняется сгенерированными моделями культурных юнитов.

## 4. Доменная модель и состояние

### Герой

- `survival/SurvivorData.java` — сериализуемое состояние игрока.
- `survival/Attr.java`, `Skill.java`, `Perk.java`, `Trait.java`, `CharClass.java` — статические оси прогрессии.
- `survival/Skills.java`, `PlayerStats.java`, `PerkEvents.java`, `HeroActions.java` — расчёты, события и действия UI.
- `SurvivalModule.SURVIVOR` — NeoForge attachment, сериализуется и копируется после смерти.
- `survival/kit/` — предметы и сущности классовых наборов.

### Королевство и мир

- `kingdom/KingdomData.java` — центральное SavedData мира с городами/королевствами.
- `kingdom/City.java` — агрегат города; `KingdomManager.java` — тики экономики и набегов.
- `kingdom/KingdomEvents.java` — основные игровые события вокруг городов и армии.
- `kingdom/Realms.java`, `Diplomacy.java`, `Territory.java` — межгосударственные отношения и защита владений.
- `kingdom/Science.java`, `Commissions.java`, `Contracts.java`, `DayClock.java` — отдельные SavedData-подсистемы.
- `trade/TradeData.java` — SavedData рынков.
- `dynasty/DynastyModule.DYNASTY` — attachment династии игрока.
- `dungeon/BossHook.Done` — SavedData для уже обработанных boss-hook структур.

Ключевое правило территории реализуется в `kingdom/Territory.java`: чужак без объявленной войны не должен ломать или ставить блоки во владениях на любой высоте. Военная ветка связана с прочностью стен и осадой.

## 5. Армия, ИИ и бой

| Задача | Основные файлы |
|---|---|
| Типы и культуры бойцов | `kingdom/SoldierType.java`, `Culture.java`, `CultureWeapons.java` |
| Сущность бойца и синхронизированное состояние | `kingdom/SoldierEntity.java` |
| Приказы и построения | `kingdom/Order.java`, `Formation.java`, `ArmyCommands.java`, `FieldCommand.java` |
| AI goals | `kingdom/ai/FoeScanGoal.java`, `SoldierMoveGoal.java`, `SoldierRangedGoal.java`, `SoldierCavalryGoal.java`, `SoldierDodgeGoal.java`, `RealmBreachGoal.java` |
| Осадная техника | `CatapultEntity.java`, `SiegeTowerEntity.java`, `SiegeBoulderEntity.java`, `Walls.java` |
| Общая тактика мобов | `combat/Tactics.java`, `combat/PlayerParry.java` |
| Боссы против армий | `dungeon/boss/BossRules.java`, `BossTactics.java`, `Telegraph.java` |
| Визуальный контракт атак | `dungeon/boss/VisualActor.java`, `VisualAction.java`, `animation/BossAnimationState.java` |
| Клиентские позы | `client/render/SoldierPoseAnimator.java`, `RegnumHumanoidModel.java`, animated model wrappers |

Для поведения армии действует продуктовая планка: ИИ должен отвечать на высоту, ямы, столбы, подкопы и укрепления, а не только идти к ближайшей цели. При изменении AI проверять одновременно навигацию, роль бойца, стоимость поиска целей и поведение в плотной толпе.

## 6. Клиент, UI и сеть

Клиент не должен напрямую владеть серверным состоянием. Типовой поток:

`SavedData/attachment/entity на сервере → *InfoPayload/*SnapshotPayload → ClientAccess/StoryClient → Screen/HUD → *ActionPayload → серверный handler`.

Основные пары:

- герой: `SkillSyncPayload` → `CreationScreen`/`JournalScreen`; действия через `HeroActionPayload`;
- город: `CityInfoPayload` → `CityScreen`; действия через `CityActionPayload`;
- армия: `ArmyInfoPayload` → `CommandScreen`; действия через `ArmyOrderPayload`;
- наука: `ScienceInfoPayload` → `ScienceScreen`; действия через `ScienceActionPayload`;
- торговля: `TradeInfoPayload` → `TradeScreen`; действия через `TradeActionPayload`;
- командиры: `CommandersPayload` → `CommandersScreen`;
- сюжет: `QuestSnapshotPayload`/`CutscenePayload` → `StoryClient` → `QuestJournalScreen`, `QuestTrackerOverlay`, `CutsceneLetterboxOverlay`.

Рендеры регистрируются в `ClientSetup`. Модели культурных юнитов выбираются через `client/model/ModelIndex.java`, а общий рендер бойца — `client/render/UnitRenderer.java`. Для производительности клиентский код должен учитывать `RegnumClientConfig.LITE_MODE`, LOD, отключаемый glow и отсутствие выделений в hot render path.

## 7. Данжи, мир и ресурсы

Есть два слоя world content:

- Java-процедуры: `dungeon/*Structure.java`, `*Piece.java`, `ProceduralPiece.java`, `worldgen/ImportedAnchorProcessor.java`;
- data-driven ресурсы: `src/main/resources/data/regnum/worldgen/` и NBT в `data/regnum/structure/`.

В `data/regnum/structure/` лежат культурные структуры (`clans`, `empire`, `north`, `steppe`, `sultanate`, `west`) и импортированный набор `dt`. Для jigsaw важна согласованность цепочки:

`structure JSON → structure_set → template_pool → NBT element → processor_list/tags/loot tables`.

Связанные валидаторы:

- `tools/validate_worldgen_refs.py` — ссылки worldgen/jigsaw;
- `tools/validate_import_support.py` — обязательные ресурсы импортированного набора;
- `tools/repair_imported_anchors.py` — scoped wiring processor-ов;
- `tools/generate_nether_keep.py` — собственный Nether Keep;
- `tools/mc_convert/` — конвертация и импорт сторонних структур.

Клиентские assets находятся в `assets/regnum/`: blockstates, item/block models, языки и текстуры. Встроенный верхнеприоритетный pack природы — `resources/resourcepacks/natural`; в Lite он не подключается.

## 8. Генерируемые файлы: где источник истины

Не править вручную:

- `src/main/java/com/alkimor/regnum/client/model/*`;
- `kingdom/CultureWeapons.java`;
- генерируемые модели, текстуры, язык, рецепты и loot tables.

Менять генератор и запускать из корня:

- `python3 tools/gen_data.py` — block/item models, язык, recipes, loot; в конце вызывает `gen_weapons.py` и `gen_kit.py`;
- `python3 tools/gen_textures.py` — растровые текстуры;
- `python3 tools/modelgen/run.py` — entity Java models и PNG;
- `tools/modelgen/units.py`, `bosses.py`, `mine_creatures.py`, `engine.py` — исходные описания геометрии/материалов.

После генерации проверять весь diff: один генератор способен затронуть десятки файлов и обе локали.

## 9. Сборка и проверка

### Основной контур

- `build.bat` — разовая Gradle build с записью в `build.log`.
- `build-watch.bat` — следит за `_rebuild.flag`.
- `_selftest.flag` — dedicated server self-test.
- `_clienttest.flag` — клиентский smoke/self-test.
- `_visualtest.flag` — визуальный тест и скриншоты в `run-visual/screenshots`.
- `_reset_visual.py` — сброс визуального мира перед тестом при закрытой игре.

Успех подтверждают конкретные маркеры: `EXIT_CODE=0`, `[REGNUM-SELFTEST] RESULT: OK`, `[REGNUM-VISUAL] RESULT: OK`. Не считать отсутствие явной ошибки успехом.

Перед Gradle/игровым запуском соблюдать `coord/BUILD.lock`. Визуальный тест открывает Minecraft и требует согласования с пользователем. Для документационных изменений компиляция обычно не нужна; проверяются diff, ссылки и чистота Git.

### Быстрый поиск ошибок

Не читать огромный `build.log` целиком. Сначала искать `error:`, `Exception`, `RESULT`, `EXIT_CODE`, затем открывать узкий контекст.

## 10. Координация и границы владения

- `coord/BOARD.md` — статус задач и владелец.
- `coord/INBOX_codex.md`, `coord/INBOX_claude.md` — обмен сообщениями; прочитанное помечается.
- `coord/LOCKS.md` — файловые блокировки; перед правкой поставить, после законченного шага снять.
- `coord/BUILD.lock` — единоличное владение сборкой/раном.
- `coord/STATE.md` — короткая сводка, но она может отставать от BOARD и последних INBOX-сообщений.
- `coord/TASKS_codex_phase3.md`, `coord/IDEAS_codex.md` — визуальный план и идеи владельца.
- `coord/LOG.md` — краткий итог каждого законченного шага.

Полосы ответственности по протоколу:

- Claude: серверная логика королевств/армии/науки/шпионажа/событий и `core/SelfTest.java`;
- Codex: клиент, UI, render/model/texture generators, worldgen/structures, CAMP-данжи, баланс-таблицы и `КАК_ИГРАТЬ.md`;
- общие файлы (`KingdomModule`, `RegnumCommands`, language через generator, `CityInfoPayload`) — только с lock и сообщением второй стороне.

## 11. Маршруты «если меняешь X»

| Изменение | Начать с | Затем проверить |
|---|---|---|
| Новый модуль | `RegnumModule`, `Regnum.MODULES` | конфиг, реестры, dedicated-server classloading |
| Новый предмет/блок | соответствующий `*Module`, `ModRegistries` | generator, model, texture, lang, recipe, loot |
| Новая сущность | модуль-регистратор | attributes, renderer, layer/model, spawn egg, lang, dedicated test |
| Новая система героя | `SurvivorData` | codec/migration, sync payload, UI, death copy |
| Новая система королевства | `KingdomData` или отдельный SavedData | dirty marking, load/save, day clock, команды/self-test |
| Изменение UI | соответствующий screen + payload | scale/resolution, live refresh, Russian overflow, client test |
| Новая атака босса | entity + `Telegraph`/`VisualAction` | impact timing, army dodge, animation reset, sound/particles |
| Новая роль бойца | `SoldierType` + AI | culture model generator, held item, pose, ranged/melee signal, LOD |
| Новая структура | structure/structure_set/template_pool/NBT | tags, loot, processors, biome modifiers, validators, live generation |
| Оптимизация | hot path и конфиг Lite/LOD | 50/100/200/400 entities, allocations, visual parity |
| Новая команда | `/regnum` branch | permissions, Russian feedback, multiplayer authority, self-test |

## 12. Инварианты и частые ловушки

1. Не загружать client-only классы на dedicated server; границу держит `ClientAccess` и ленивые сетевые callback-и.
2. После изменения enum, ordinal или packed int-array одновременно менять codec, producer, consumer и тест границ.
3. Любое SavedData-изменение требует `setDirty()` и обратной совместимости существующих миров.
4. Не путать визуальный масштаб модели с физическим hitbox; не применять scale дважды.
5. Boss telegraph, server impact и client animation должны иметь одну временную шкалу.
6. Jigsaw JSON может быть синтаксически валиден, но ссылаться на отсутствующий NBT/tag/loot table — запускать валидаторы.
7. Генератор важнее результата: ручная правка generated-файла будет затёрта.
8. `coord/STATE.md` — удобный кеш, но источник текущей занятости — BOARD + LOCKS + последние INBOX-записи.
9. Не удалять старые миры, логи или captures без согласования владельца.
10. Перед коммитом отделять чужие параллельные изменения; коммитить только файлы своей задачи.

## 13. Быстрый старт следующей сессии

1. `git status --short` и узкий `git diff`.
2. Прочитать `coord/BOARD.md`, `coord/INBOX_codex.md` (хвост), `coord/LOCKS.md`, `coord/BUILD.lock`.
3. Найти задачу и её владельца; поставить lock только на нужные файлы.
4. Открыть модульную точку входа, состояние/codec, событие/handler и клиентский consumer — не изучать подсистему по одному файлу.
5. После изменения выполнить минимальную релевантную проверку, снять lock, обновить BOARD/LOG/INBOX и сделать отдельный коммит.

## 14. Документы по назначению

- `КАК_ИГРАТЬ.md` — пользовательская механика и команды.
- `ПЛАН_МОДОВ_И_ИНТЕГРАЦИИ.md` — моды, зависимости и интеграционный план.
- `coord/context-handoff.md`, `coord/roadmap-phase2.md` — исторический handoff и roadmap.
- `coord/TASKS_codex_phase3.md` — текущая клиентско-визуальная фаза.
- `coord/IDEAS_codex.md` — идеи владельца; намерение — реализовать все, но приоритет задают BOARD и прямой запрос.
- `coord/GIT_WORKFLOW.md` — локальный Git/worktree workflow.
- `THIRD_PARTY_NOTICES.md` — происхождение и лицензии стороннего контента.

Этот индекс следует обновлять при изменении точек входа, формата сохранений/сети, генераторов, тестового контура или границ ответственности.
