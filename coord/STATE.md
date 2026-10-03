# STATE — актуальный checkpoint

Обновлено: 2026-10-03 08:15 МСК.

## Подтверждено
- Чистый `build runSelfTest`: `RESULT: OK`, `EXIT_CODE=0`; `minionDeadTarget: OK`, 8 BossHook rooms generated, cavalry 4/4 at t=150. Log: `coord/x3-m3-clean-selftest-20261003-075650.log`.
- M3 повторный live visual gate: настоящая Королева естественно создала четыре ИИ-ползунка; после потери одной живой цели через 51 тик все четыре переключились на другие живые отряды и все четыре сместились (`LIVE_BROOD_RETARGET: OK`). Log: `coord/m3-closeup-visual-20261003-081004.log`.
- X3 live crypt battle: настоящий CryptLordEntity против десяти ИИ-бойцов внутри освещённой themed hall; визуально записаны фазы 2/3, sweep/slam synced actions и уклонения; `CRYPT_BATTLE_VISUAL: OK`, общий visual `RESULT: OK`. Log: `coord/x3-m3-visible-visual-20261003-080629.log`.
- Кадры: `run-visual/screenshots/19_crawler_live_brood.png`, `20_crawler_brood_target_loss.png`, `53_dt_live_crypt_phase2_a.png`, `55_dt_live_crypt_phase2_c.png`, `57_dt_live_crypt_phase3_b.png`.

## Открыто
- M3 передан на review; это автоматизированная живая проверка в отдельном QA-мире, не завершённая кампания.
- X3 остаётся doing: проверить наружные стены реальной арены и другие данжи/боссов; текущий live бой покрывает только крипту и Крипт-лорда.
- Свежий selftest всё ещё печатает `Unprimed heightmap: OCEAN_FLOOR_WG` при runtime установке зданий. Передано Claude для проверки `Prefab.groundAt`; тест формально проходит, но ошибки лога остаются.
- X7 Full/Lite обычного леса, X4, X8, P3/P4/P7 и оставшийся серверный список остаются открыты.
- Финальный jar не собирался и не утверждён.

## Следующий приоритет
1. Проверить и оформить внешний босс-барьер против реального босса (включая выход/рывок через проход), затем продвигать X3/X4 dungeon-world polish.
2. Следующий build брать только после проверки общего BUILD.lock и согласования слота; пока ожидание — продолжать независимую X7/X8 работу.
