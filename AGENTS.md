# Regnum: Королевства и Легенды — контекст для агентов (Codex / Claude)

Мод Minecraft **1.21.1, NeoForge 21.1.x**. mod_id `regnum`, пакет `com.alkimor.regnum`. Весь текст в игре — только на русском (en_us.json = копия ru_ru.json).

## Правила совместной работы
- Эта папка — источник истины. Перед началом: посмотреть, что изменилось (`git status`/`git diff`, если есть git). После шага — коммит с понятным описанием.
- Один агент — одна подсистема за раз; не править одновременно одни и те же файлы.
- Сгенерированное руками не править: `src/main/java/.../client/model/*`, `kingdom/CultureWeapons.java`, текстуры/модели/язык из `tools/*.py`. Меняйте генератор и перезапускайте его из корня:
  - `python3 tools/gen_data.py` (модели блоков/предметов, язык, рецепты, лут; в конце вызывает gen_weapons и gen_kit)
  - `python3 tools/gen_textures.py`, `python3 tools/modelgen/run.py` (модели сущностей → Java + PNG)
- Ничего не удалять без согласования с владельцем.

## Сборка и тесты
- `build.bat` — разовая сборка; `build-watch.bat` — автосборка по файлу `_rebuild.flag`.
- Вместе с `_rebuild.flag` можно положить `_selftest.flag` (серверный автотест), `_visualtest.flag` (клиент со скриншотами в `run-visual/screenshots`), `_clienttest.flag`.
- Лог: `build.log`. Успех: `EXIT_CODE=0`, `[REGNUM-SELFTEST] RESULT: OK`, `[REGNUM-VISUAL] RESULT: OK`.
- Перед визуальным тестом при закрытой игре: `python3 _reset_visual.py`.

## Архитектура
- `Regnum.java` подключает модули (`RegnumModule`): survival, crafting, kingdom, dungeon, regions, wanderers, trade, dynasty, combat. Реестры — `core/ModRegistries`, конфиги — `core/RegnumConfig` (common) и `core/RegnumClientConfig`, пакеты — `core/network`.
- Герой (Bannerlord+Zomboid): `survival/` — Attr(6), Skill(18, 0–100, фокус и предел обучения), Perk(144, выбор 1 из 2 на 25/50/75/100), Trait(29), CharClass(6), SurvivorData, Skills, PlayerStats, PerkEvents, HeroActions; инструменты классов — `survival/kit`.
- Королевство: `kingdom/` — City/KingdomData/KingdomManager (экономика, набеги), SoldierEntity (приказы, строи, приёмы), Culture (6 культур), оружие культур.
- Бой и сложность: `combat/Tactics` (умные враги), `combat/ScarcityModifier`; боссы — `dungeon/boss` (Telegraph — атаки по зонам с подготовкой, BossRules — масштаб и правила против армий).
- Данжи: `dungeon/*Piece` (процедурные структуры), `dungeon/BossHook` (залы боссов в импортированных данжах). Сюжет: `story/` (Cutscene, BossIntros, Quests).

## Решения владельца
- Защита территории королевств жёсткая: без объявленной войны чужак не ломает и не ставит блоки во владениях (от бедрока до неба). Во время войны — прочность стен, осада.
- ИИ армии должен отвечать на любую позицию игрока (лучники, сапёры, лестницы, поджог); абуз ямами, столбами и подкопом недопустим.
- Цель — высокая сложность: умные мобы, тяжёлые боссы, дефицит, долгая прокачка; нужна оптимизация и облегчённый режим для слабых ПК.

## Состояние (02.10.2026)
Готово на сервере: шпионаж, деревни и аванпосты, наука (25 технологий), склад, эпидемии, дипломатия, огнестрел (14 типов бойцов), Compat (Create/Aeronautics), BossTactics, Contracts, Fortune, Legends, FieldCommand (выход командира), Council (роли), BattleReport, `story/*` (серверные катсцены боссов, 35 квестов, 4 фракции — `/regnum story …`). Игровое руководство — `КАК_ИГРАТЬ.md`.
Работа Codex — `coord/TASKS_codex_phase3.md` (анимации ModelPart/процедурные позы, экраны, звук, данжи/боссы, мир). Идеи — `coord/IDEAS_codex.md` (владелец хочет реализовать все).
Финальный jar — только после одобрения владельца.

## Связь агентов
Протокол обмена между Claude и Codex — coord/PROTOCOL.md (BOARD, INBOX, LOCKS, BUILD.lock). В начале каждого шага читать coord/BOARD.md и свой INBOX.
