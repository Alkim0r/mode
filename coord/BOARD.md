# BOARD (обновлять статус при смене)
| id | владелец | статус | задача | файлы |
|----|----------|--------|--------|-------|
| C1 | claude | done | Эффекты INFIRMARY (лечение/эпидемия) и SMITHY (броня/оружие) | kingdom/ |
| C2 | claude | todo | Ресурсы/переработка/склад-инвентарь (соль, олово, селитра, сера, лён), рецепты | kingdom/, tools/gen_data.py |
| C3 | claude | done | Эпидемия: глубокая переработка | kingdom/ |
| C4 | claude | done | Дипломатия: casus belli, посланники, дань | kingdom/Realms |
| C5 | claude | done | Огнестрел: мушкетёр/бомбарда, гейт по науке | kingdom/SoldierType, Combat |
| C6 | claude | done | Soft-интеграция Create/Aeronautics (AIR ветка) | kingdom/ |
| X1 | codex | review | Экран науки (дерево технологий) + экран командиров | client/screen |
| X2 | codex | doing | Модели/текстуры 14 ролей уже заведены; проверить читаемость мушкетёра/бомбардира и добавить выстрельную позу после серверного сигнала анимации | tools/modelgen/units.py, client/model, client/render |
| X3 | codex | doing | Static candidate: собственный Nether Keep NBT, placements для 11 структур, исправлены loot/NBT/tag refs; ждёт сборку/selftest и генерацию в мире | data/regnum/worldgen/template_pool, structure_set, structure tags/NBT |
| X4 | codex | todo | Новые боссы (модели+поведение), аванпосты на независимых деревнях | entity, structures |
| X5 | codex | todo | Обновить КАК_ИГРАТЬ.md, AGENTS.md под новые системы | docs |
Полный список пожеланий пользователя: Project «mode» -> regnum/roadmap-phase2.md

| X6 | claude | doing | CityScreen/HUD: запасы, эпидемия, кузница, ресурсы апгрейда (после моего payload) | client/screen |
| X7 | codex | doing | Исправлена схема rare azure_bluet random_patch; build + server selftest OK, клиентский тест OK. Остаются визуальная плотность/проверка структур и дальнейший world polish | resources, worldgen |
| C7 | claude | done | Payload запасов/эпидемии для CityScreen | core/network |

| C8 | claude | done | Босс-тактика (BossTactics: строй героев/щитов/стрелков/копий/конницы множит урон; /regnum warboss), разведчик SCOUT, клич военачальника, организованные армии ИИ | dungeon/boss, kingdom |

| C9 | claude | done | Аванпосты по назначению, просьбы деревень, выход командира | kingdom/ |
| X5 | claude | doing | КАК_ИГРАТЬ.md готов; AGENTS.md обновить | docs |
| P3 | codex | doing | Фаза 3: журнал/HUD, процедурные боевые позы и letterbox-титр катсцен; текущий overlay собирается, визуальная проверка ещё ожидается | client/screen, client/render |
| P4 | codex | doing | FPS/p95 HUD сбрасывает окно при переключении Full/Lite; Lite убирает лишний glow/decor/nature pack; uncached compile в изолированной копии OK, clientTest завис до TitleScreen, повторить после профиля | client, core/RegnumClientConfig |
| X8 | codex | doing | LLibrary keyframes: Queen/brood + новые CryptLord/Mire/Scarab/Forge модели. Build EXIT0, visual OK19:07, dedicated OK19:11. Реальный CryptLord: client/server impact18/26 совпали, 150 кадров1280x720; дальнейшая художественная QA и dodge-позы открыты | client/model, client/render, animation/BossAnimationState |
| Q1 | claude | doing | Квесты/сюжет (story/*), реализация идей IDEAS 1-9,12,15 | story, kingdom |

| P5 | codex | done | Журнал: прямые действия, живое обновление, полный текст с прокруткой; проверка кадров natural pack | client/screen/QuestJournalScreen, client/render/QuestTrackerOverlay, resourcepacks/natural, tools/validate_pack_animations.py |
| P6 | codex | done | Сброс процедурной позы без накопления наклонов; кэш обхода модели/HUD вместо выделений каждый кадр | client/render/RegnumHumanoidModel, client/model/Crawler*Model, QuestTrackerOverlay |

| P7 | codex | doing | Восстановлены13 зачарований/3 модификатора через importer --support; validator127 refs OK; runtime TagLoader/loot warnings исчезли, visual OK16:31. Остаются BlockAttachedEntity/огнестрел и server gate | tools/mc_convert/dt_import.py, data/enchantment, item_modifier, modelgen |

| B1 | codex | done | Все 5 боссов увеличены в 1.5 раза, физические размеры согласованы и применены Claude без двойного SCALE; isolated build/visual/selftest OK (15:22), 14 кадров осмотрены. Узкие коридоры остаются предметом игровой QA | client/render, ClientVisualTest |

| B2 | codex | doing | 10 бойцов против всех5: N/R/F, камера, реальные стены и DASH-WALLS OK. Исправлен premature win: живой QueenDown578/13brood → win889/0brood/3army; отряд добивает выводок. Build EXIT0, dedicated OK18:28; живая C10 оценка других боссов продолжается | client/BattleShowcase, tools/Show-Regnum.ps1 |

| C10 | claude | review (v3 собран, ждёт живую проверку Кодекса) | Новый запрос владельца: бойцы реагируют на видимые предупреждения атак и пытаются выйти из опасной зоны с учётом роли, реакции и навигации; контракт в INBOX 15:40 | kingdom/ai, SoldierEntity, dungeon/boss/Telegraph |
| C11 | claude | todo | Подтверждённый пользователем idle brood: summoned minions не должны бросать цель на свету, переискать бойцов/игроков после смерти target; запрос в INBOX | mine/CrawlerEntity, CrawlerQueenEntity, core/SelfTest |

| G1 | codex | doing | Локальный Git transition: baseline main + отдельный worktree для Lite Codex | .git, coord/GIT_WORKFLOW.md, client/ClientSetup |
