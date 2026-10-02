# Regnum: Королевства и Легенды

Мод для Minecraft **1.21.1 / NeoForge 21.1.x**.

## Сборка
Нужна Java 17+ (Gradle сам скачает JDK 21 для компиляции).

```
gradlew.bat build        (Windows)
./gradlew build          (Linux/macOS)
```
Готовый мод: `build/libs/regnum-0.1.0.jar` → положить в папку `mods`.

Запуск тестового клиента: `gradlew runClient`, сервера: `gradlew runServer`.

## Структура
- `core/` — каркас: реестры, конфиг, сеть, команды
- `survival/` — навыки, травмы, медицина, собирательство, лут
- `crafting/` — ремёсла (кузнечное дело, закалка)
- `kingdom/` — города, постройки, армия, приказы, набеги
- `dungeon/` — склепы, босс Морграт
- `wanderers/` — странники с характером
- `client/` — рендеры и экраны
- `tools/` — генераторы текстур и JSON (`python3 tools/gen_textures.py`, `python3 tools/gen_data.py`)
