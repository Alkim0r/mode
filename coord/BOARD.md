# BOARD — только актуальная работа

Статусы: `todo`, `doing`, `review`. Завершённые задачи удаляются отсюда после попадания результата в Git/STATE. История прежней доски доступна до коммита `017f32b`.

| id | владелец | статус | задача | область/критерий |
|---|---|---|---|---|
| C2 | claude | todo | Новые ресурсы, переработка и склад-инвентарь | `kingdom/`, `tools/gen_data.py` |
| C11 | claude | todo | Выводок Королевы не теряет цели на свету и переходит к новой цели | `mine/`, `core/SelfTest.java` |
| Q1 | claude | doing | Сюжет и оставшиеся серверные идеи | `story/`, `kingdom/` |
| M1 | claude | review | Optional bridge внешних боссов | runtime external boss + multiplayer/unloaded-world cases |
| X1 | codex | review | Экран науки и экран командиров | финальная визуальная проверка |
| X2 | codex | doing | Читаемость ролей и боевые позы 14 типов бойцов | `tools/modelgen/units.py`, `client/model`, `client/render` |
| X3 | codex | doing | Nether Keep и импортированные jigsaw-структуры | свежая генерация в мире + server gate |
| X4 | codex | todo | Новые боссы и аванпосты независимых деревень | модели, поведение, структуры |
| X6 | claude | doing | CityScreen/HUD: запасы, эпидемия, кузница, upgrade costs | `client/screen` по готовому payload |
| X7 | codex | doing | World polish после исправления azure_bluet patch | плотность и живая проверка структур |
| P3 | codex | doing | Журнал/HUD, процедурные позы, cutscene letterbox | оставшаяся визуальная приёмка |
| P4 | codex | doing | Lite и FPS/p95 HUD для слабых ПК | повтор client test; проверить переключение Full/Lite |
| P7 | codex | doing | Импортированные enchantments/modifiers и anchors | fresh dedicated gate, BlockAttachedEntity/firearm cases |
| X8 | codex | doing | Анимации Королевы, выводка и 4 региональных боссов | художественная QA; server/client impact уже сверены |
| B2 | codex | doing | Живой бой 10 бойцов против 5 боссов | закончить оценку остальных боссов без demo-артефактов |
| C10 | claude→codex | review | Бойцы уходят из видимых зон атак | сервер v3 собран; требуется живая визуальная приёмка |
| L1 | codex-lite | review | Lite-ветка SiegeTowerRenderer | commit `d6758f9`; visual test не пройден |
| M3 | codex | doing | QA Королевы и только её выводка | 1–2 воспроизводимых дефекта, кадр и лог |

## Недавние подтверждённые результаты

- `I1`: создан `coord/CODEX_PROJECT_INDEX.md`, commit `017f32b`.
- `A1`: очищен и нормализован обязательный агентский контекст; история осталась в Git.
- Общий animated-boss snapshot: build/visual/dedicated OK 19:07–19:11 МСК.
- Dodge pose snapshot: visual/dedicated OK 19:19–19:21 МСК.
- Серверная сериализация `VisualAction`: selftest OK 19:39 МСК.
