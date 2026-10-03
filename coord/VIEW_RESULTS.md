# Как посмотреть текущую работу Regnum

`C:\Users\Pavel\Desktop\for_mode\regnum\ПОСМОТРЕТЬ_БОЙ.bat` запускает живой бой десяти стандартных бойцов против каждого из пяти боссов по очереди на отдельной площадке с настоящими стенами. Камера наблюдателя: WASD и мышь, Space вверх, Shift вниз. N — следующий босс, R — повтор текущего, Esc — пауза. Режим не изменяет обычные HP/урон и работает только в собственном демонстрационном мире. До завершения повторной проверки арена считается рабочим тестом.

Это рабочая демонстрация, не финальный релиз и не обещание завершённой красоты мира.

Двойной щелчок по `C:\Users\Pavel\Desktop\for_mode\regnum\ПОСМОТРЕТЬ_МОД.bat` запускает Minecraft с Regnum из отдельной проверочной копии и сразу открывает демонстрационный мир. Дождитесь окончания текущего автотеста. Обычная игра затем остаётся открытой до вашего выхода. При запуске через терминал путь тот же; команду выполняет `tools/Show-Regnum.ps1`.

В мире можно летать в творческом режиме. Около точки появления — площадка с новыми бойцами и шахтными существами; это стенд для моделей, не итоговый ландшафт кампании.

- `/regnum story gui` — настоящий сюжетный журнал и текущие задания.
- `/regnum science gui` — наука (доступность зависит от города).
- `/regnum commander gui` — военачальники.
- `/regnum mine pack` и `/regnum mine queen` — проверка шахтных существ; нужны права оператора.

Свежие снимки интерфейсов: `C:\Users\Pavel\Desktop\for_mode\regnum-codex-build\run-clienttest\screenshots`.
Снимки юнитов и боссов: `C:\Users\Pavel\Desktop\for_mode\regnum-codex-build\run-visual\screenshots`.

Интерфейсные тесты используют искусственные задания для проверки длинных текстов. В демонстрационном мире журнал получает реальные задания с сервера.

Демонстрация запускает базовый Regnum; внешние шейдеры и финальное сравнение Full/Lite ещё не проверены. Научные и сюжетные автотесты проходят, но обнаруженные ошибки импортированных тегов и предупреждения прикреплённых сущностей ещё требуют исправлений.

## Камера боя
В обновлённом стенде `F` переключает слежение за боссом и свободную камеру. `N` — следующий босс, `R` — повтор. Захват движения для QA включает слежение по умолчанию; обычный запуск сохраняет свободную камеру. Кадры выводка для QA начинаются после его реального появления. Статичные модельные снимки с NoAI не являются проверкой поведения.

## X3: свежая проверка арены Морграта (2026-10-03, 06:12 МСК)

Свежий тестовый прогон завершился `BUILD SUCCESSFUL`, `[REGNUM-VISUAL] RESULT: OK` и подтвердил, что в сцене появляется настоящий `CryptLordEntity`. PNG лежат в текущей рабочей папке проекта:

- `run-visual/screenshots/50_dt_undead_crypt.png` — сейчас чёрный кадр, не использовать как пример качества.
- `run-visual/screenshots/51_dt_arena_undead_crypt.png` — чистый зал; визуально пока слишком пустой и прямоугольный.
- `run-visual/screenshots/52_dt_boss_undead_crypt.png` — Морграт виден в зале с босс-баром.

Эти скриншоты — диагностический стенд, не готовый данж и не подтверждение финального качества. X3 остаётся открытым: нужно исправить ракурс кадра 50, сделать зал выразительнее и снова вручную проверить свежие PNG. Чтобы посмотреть кадры в Проводнике, откройте `C:\Users\Pavel\Desktop\for_mode\regnum\run-visual\screenshots`.

## X3 — свежий стенд арены (2026-10-03 06:47 МСК)

Текущий проверенный прогон: `build runSelfTest` и чистый `runVisualTest` завершились с exit 0 и `RESULT: OK`. В тестовой крипте показан настоящий `CryptLordEntity`. Последняя правка добавила костяную отделку, стеновые опоры, рёбра потолка, рамку за алтарём и четыре дополнительных фонаря; кадр 50 перенесён внутрь комнаты и больше не чёрный.

- `C:\Users\Pavel\Desktop\for_mode\regnum\run-visual\screenshots\50_dt_undead_crypt.png` — общий вид зала.
- `C:\Users\Pavel\Desktop\for_mode\regnum\run-visual\screenshots\51_dt_arena_undead_crypt.png` — арена до появления босса.
- `C:\Users\Pavel\Desktop\for_mode\regnum\run-visual\screenshots\52_dt_boss_undead_crypt.png` — Морграт с босс-баром.

Это всё ещё диагностическая арена, не готовый imported dungeon и не видеозапись боя. Визуальная приёмка реальной драки и boss-animation остаётся отдельной задачей B2; X3 не закрыт одним автоматическим маркером.

## M3 live brood repro — 2026-10-03 07:17 МСК
- Real Queen naturally spawned 4 live AI crawlers. After original Soldier target died, all 4 still held its dead UUID at t+61; navDone=true, movement 0.49–0.58 blocks, zero switched to two reachable living Soldiers. M3 gate failed and request #29 was sent to Claude. Full log coord/m3-live-queen-20261003-0717.log; frames `run-visual/screenshots/19_crawler_live_brood.png` and `run-visual/screenshots/20_crawler_brood_target_loss.png`. This test arena is diagnostic, not final art or imported dungeon acceptance.

## X3 + M3 live QA — 2026-10-03 08:00–08:11 МСК
- Fresh dedicated selftest `RESULT: OK`; included C11 `minionDeadTarget: OK`, cavalry 4/4 at t=150, and 8 BossHook rooms. See `coord/x3-m3-clean-selftest-20261003-075650.log`.
- Fresh crypt hall camera is inside the walls, not underground behind the perimeter. Screenshots `53_dt_live_crypt_phase2_a.png` through `59_dt_live_crypt_fight_summary.png` show the real Crypt Lord fighting ten live AI soldiers. Captured synced Sweep and Slam clips across phases 2–3; dodge count observed in live samples. `CRYPT_BATTLE_VISUAL: OK`.
- Real Queen naturally spawned four AI brood crawlers. Test removes one selected live target after holding the Queen; in 51 ticks all four acquired the other live soldiers and all four moved. `LIVE_BROOD_RETARGET: OK`; `coord/m3-closeup-visual-20261003-081004.log`.
- M3 remains a review item; QA scene is diagnostic, not the campaign dungeon or final visual acceptance. X3 remains open for surface wall containment and non-crypt routes.
