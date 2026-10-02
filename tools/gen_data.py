"""Генератор JSON-ресурсов Regnum: модели, blockstates, рецепты, лут, worldgen, теги, локализация.
Запуск: python3 tools/gen_data.py (из корня проекта)
"""
import json
import os

A = "src/main/resources/assets/regnum"
D = "src/main/resources/data"


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


# ------------------------------------------------------------------ блоки
CUBE_BLOCKS = ["town_hall", "barracks", "market", "watchtower", "master_forge", "training_ground", "builder_hut",
               "library", "university", "infirmary", "smithy", "warehouse", "stable"]
for b in CUBE_BLOCKS:
    w(f"{A}/models/block/{b}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"side": f"regnum:block/{b}_side", "top": f"regnum:block/{b}_top",
                     "bottom": "minecraft:block/spruce_planks" if b != "master_forge" else "minecraft:block/stone_bricks"}})
    w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"regnum:block/{b}"}}})
    w(f"{A}/models/item/{b}.json", {"parent": f"regnum:block/{b}"})

w(f"{A}/models/block/crypt_altar.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "side": "regnum:block/crypt_altar_side", "top": "regnum:block/crypt_altar_top", "bottom": "minecraft:block/polished_blackstone"}})
w(f"{A}/models/block/crypt_altar_used.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "side": "regnum:block/crypt_altar_side", "top": "regnum:block/crypt_altar_top_used", "bottom": "minecraft:block/polished_blackstone"}})
w(f"{A}/blockstates/crypt_altar.json", {"variants": {
    "enabled=false": {"model": "regnum:block/crypt_altar"}, "enabled=true": {"model": "regnum:block/crypt_altar_used"}}})
w(f"{A}/models/item/crypt_altar.json", {"parent": "regnum:block/crypt_altar"})
for alt, bottom in (("mire_altar", "minecraft:block/mud_bricks"), ("forge_altar", "minecraft:block/polished_blackstone"), ("sun_altar", "minecraft:block/sandstone_top")):
    w(f"{A}/models/block/{alt}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": f"regnum:block/{alt}_side", "top": f"regnum:block/{alt}_top", "bottom": bottom}})
    w(f"{A}/models/block/{alt}_used.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": f"regnum:block/{alt}_side", "top": f"regnum:block/{alt}_top_used", "bottom": bottom}})
    w(f"{A}/blockstates/{alt}.json", {"variants": {"enabled=false": {"model": f"regnum:block/{alt}"}, "enabled=true": {"model": f"regnum:block/{alt}_used"}}})
    w(f"{A}/models/item/{alt}.json", {"parent": f"regnum:block/{alt}"})
w(f"{A}/models/block/ember_core.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "regnum:block/ember_core"}})
w(f"{A}/blockstates/ember_core.json", {"variants": {"": {"model": "regnum:block/ember_core"}}})
w(f"{A}/models/item/ember_core.json", {"parent": "regnum:block/ember_core"})

# ------------------------------------------------------------------ предметы
FLAT = ["bandage", "splint", "herbal_decoction", "medkit", "healing_herb", "survivor_journal",
        "mastery_scroll", "crypt_heart", "trick_elixir", "life_root",
        "rot_root", "star_iron", "sun_amber", "antidote", "sun_amulet",
        "salt", "spices", "silk", "furs", "wine", "iron_goods", "herb_bundle", "amber"]
for i in FLAT:
    w(f"{A}/models/item/{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"regnum:item/{i}"}})
for i in ["commander_baton", "morgrath_blade", "mire_staff", "star_blade"]:
    w(f"{A}/models/item/{i}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"regnum:item/{i}"}})
for egg in ["crypt_lord_spawn_egg", "bandit_spawn_egg", "wanderer_spawn_egg", "mire_mother_spawn_egg", "forgemaster_spawn_egg", "scarab_queen_spawn_egg"]:
    w(f"{A}/models/item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})

# ------------------------------------------------------------------ локализация (только русский; en_us — копия, чтобы текст был русским при любом языке клиента)
LANG = {
    "itemGroup.regnum": "Regnum: Королевства и Легенды",
    # блоки
    "block.regnum.town_hall": "Ратуша",
    "block.regnum.barracks": "Казарма",
    "block.regnum.market": "Рынок",
    "block.regnum.watchtower": "Сторожевая башня",
    "block.regnum.master_forge": "Горн мастера",
    "block.regnum.training_ground": "Учебный плац",
    "block.regnum.builder_hut": "Мастерская строителя",
    "block.regnum.library": "Библиотека",
    "block.regnum.university": "Университет",
    "block.regnum.infirmary": "Лазарет",
    "block.regnum.smithy": "Кузница",
    "block.regnum.warehouse": "Склад",
    "block.regnum.stable": "Конюшня",
    "block.regnum.crypt_altar": "Алтарь склепа",
    # предметы
    "item.regnum.commander_baton": "Жезл командира",
    "item.regnum.bandage": "Бинт",
    "item.regnum.splint": "Шина",
    "item.regnum.herbal_decoction": "Целебный отвар",
    "item.regnum.medkit": "Аптечка",
    "item.regnum.healing_herb": "Целебные травы",
    "item.regnum.survivor_journal": "Дневник выжившего",
    "item.regnum.mastery_scroll": "Свиток мастерства",
    "item.regnum.crypt_heart": "Сердце склепа",
    "item.regnum.morgrath_blade": "Клинок Морграта",
    "item.regnum.trick_elixir": "Чудо-эликсир",
    "item.regnum.life_root": "Корень жизни",
    "item.regnum.salt": "Ящик соли",
    "item.regnum.spices": "Мешок пряностей",
    "item.regnum.silk": "Тюк шёлка",
    "item.regnum.furs": "Связка мехов",
    "item.regnum.wine": "Бочонок вина",
    "item.regnum.iron_goods": "Ящик железных изделий",
    "item.regnum.herb_bundle": "Лекарственный сбор",
    "item.regnum.amber": "Ларец янтаря",
    "structure.regnum.bandit_camp": "Лагерь разбойников",
    "item.regnum.crypt_lord_spawn_egg": "Яйцо призыва: Морграт",
    "item.regnum.bandit_spawn_egg": "Яйцо призыва: Разбойник",
    "item.regnum.wanderer_spawn_egg": "Яйцо призыва: Странник",
    # сущности
    "entity.regnum.crypt_lord": "Морграт, Костяной Владыка",
    "entity.regnum.soldier": "Солдат",
    "entity.regnum.bandit": "Разбойник",
    "entity.regnum.catapult": "Катапульта",
    "entity.regnum.siege_boulder": "Каменное ядро",
    "entity.regnum.siege_tower": "Осадная башня",
    "entity.regnum.caravan": "Торговец",
    "entity.regnum.wanderer": "Странник",
    # эффекты
    "effect.regnum.bleeding": "Кровотечение",
    "effect.regnum.fracture": "Перелом",
    "effect.regnum.infection": "Инфекция",
    # смерть
    "death.attack.regnum.bleeding": "%1$s истёк кровью",
    "death.attack.regnum.bleeding.player": "%1$s истёк кровью, спасаясь от %2$s",
    # структура
    "structure.regnum.crypt": "Склеп",
    "block.regnum.mire_altar": "Алтарь Топи",
    "block.regnum.forge_altar": "Алтарь Горна",
    "block.regnum.sun_altar": "Солнечный алтарь",
    "block.regnum.ember_core": "Угольное сердце",
    "item.regnum.rot_root": "Гнилой корень",
    "item.regnum.star_iron": "Звёздное железо",
    "item.regnum.sun_amber": "Солнечный янтарь",
    "item.regnum.antidote": "Противоядие",
    "item.regnum.mire_staff": "Посох Топи",
    "item.regnum.star_blade": "Звёздный клинок",
    "item.regnum.sun_amulet": "Амулет солнца",
    "item.regnum.mire_mother_spawn_egg": "Яйцо призыва: Матушка Топь",
    "item.regnum.forgemaster_spawn_egg": "Яйцо призыва: Горновой",
    "item.regnum.scarab_queen_spawn_egg": "Яйцо призыва: Сехмет-ра",
    "entity.regnum.mire_mother": "Матушка Топь",
    "entity.regnum.forgemaster": "Горновой, огненный кузнец",
    "entity.regnum.scarab_queen": "Сехмет-ра, царица-скарабей",
    "structure.regnum.sunken_shrine": "Затопленное капище",
    "structure.regnum.forge_fortress": "Гномья кузня-крепость",
    "structure.regnum.sand_tomb": "Песчаная гробница",
}
w(f"{A}/lang/ru_ru.json", LANG)
w(f"{A}/lang/en_us.json", LANG)

# ------------------------------------------------------------------ рецепты
R = f"{D}/regnum/recipe"


def shaped(name, pattern, key, result, count=1, cat="misc"):
    k = {}
    for ch, v in key.items():
        k[ch] = {"tag": v[1:]} if v.startswith("#") else {"item": v}
    w(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern, "key": k,
                           "result": {"id": result, "count": count}})


def shapeless(name, ingredients, result, count=1, cat="misc"):
    ing = [{"tag": v[1:]} if v.startswith("#") else {"item": v} for v in ingredients]
    w(f"{R}/{name}.json", {"type": "minecraft:crafting_shapeless", "category": cat, "ingredients": ing,
                           "result": {"id": result, "count": count}})


shaped("town_hall", ["GBG", "PLP", "SSS"], {"G": "minecraft:gold_ingot", "B": "minecraft:bell", "P": "#minecraft:planks",
                                            "L": "minecraft:lectern", "S": "minecraft:stone_bricks"}, "regnum:town_hall", cat="building")
shaped("barracks", ["WIW", "PCP", "SSS"], {"W": "minecraft:iron_sword", "I": "minecraft:iron_ingot", "P": "#minecraft:planks",
                                           "C": "minecraft:crafting_table", "S": "minecraft:cobblestone"}, "regnum:barracks", cat="building")
shaped("market", ["WWW", "PEP", "PBP"], {"W": "#minecraft:wool", "P": "#minecraft:planks", "E": "minecraft:emerald",
                                         "B": "minecraft:barrel"}, "regnum:market", cat="building")
shaped("watchtower", ["TST", "LLL", "CLC"], {"T": "minecraft:torch", "S": "minecraft:spyglass", "L": "#minecraft:logs",
                                            "C": "minecraft:cobblestone"}, "regnum:watchtower", cat="building")
shaped("master_forge", ["III", "IAI", "BFB"], {"I": "minecraft:iron_ingot", "A": "minecraft:anvil", "B": "minecraft:bricks",
                                               "F": "minecraft:blast_furnace"}, "regnum:master_forge")
shaped("builder_hut", ["PPP", "LCL", "PPP"], {"P": "#minecraft:planks", "L": "#minecraft:logs", "C": "minecraft:crafting_table"}, "regnum:builder_hut", cat="building")
shaped("training_ground", ["SHS", "PTP", "PPP"], {"S": "minecraft:wooden_sword", "H": "minecraft:hay_block", "T": "minecraft:target",
                                                  "P": "#minecraft:planks"}, "regnum:training_ground", cat="building")
shaped("library", ["PPP", "BLB", "PPP"], {"P": "#minecraft:planks", "B": "minecraft:bookshelf", "L": "minecraft:lectern"}, "regnum:library", cat="building")
shaped("university", ["QBQ", "BLB", "SSS"], {"Q": "minecraft:quartz_block", "B": "minecraft:bookshelf", "L": "minecraft:lectern", "S": "minecraft:stone_bricks"}, "regnum:university", cat="building")
shaped("infirmary", ["WWW", "BHB", "PPP"], {"W": "#minecraft:wool", "B": "minecraft:brewing_stand", "H": "minecraft:honey_bottle", "P": "#minecraft:planks"}, "regnum:infirmary", cat="building")
shaped("smithy", ["III", "FAF", "SSS"], {"I": "minecraft:iron_ingot", "F": "minecraft:furnace", "A": "minecraft:anvil", "S": "minecraft:stone_bricks"}, "regnum:smithy", cat="building")
shaped("warehouse", ["PCP", "CBC", "PCP"], {"P": "#minecraft:planks", "C": "minecraft:chest", "B": "minecraft:barrel"}, "regnum:warehouse", cat="building")
shaped("stable", ["PHP", "FLF", "PPP"], {"P": "#minecraft:planks", "H": "minecraft:hay_block", "F": "minecraft:oak_fence", "L": "minecraft:lead"}, "regnum:stable", cat="building")
shaped("commander_baton", ["  G", " S ", "S  "], {"G": "minecraft:gold_ingot", "S": "minecraft:stick"}, "regnum:commander_baton", cat="equipment")
shapeless("bandage_from_wool", ["#minecraft:wool"], "regnum:bandage", 3)
shapeless("bandage_from_paper", ["minecraft:paper", "minecraft:paper", "minecraft:string"], "regnum:bandage", 1)
shapeless("splint", ["minecraft:stick", "minecraft:stick", "regnum:bandage"], "regnum:splint")
shapeless("herbal_decoction", ["regnum:healing_herb", "regnum:healing_herb", "minecraft:glass_bottle"], "regnum:herbal_decoction")
shapeless("medkit", ["regnum:bandage", "regnum:bandage", "regnum:splint", "regnum:herbal_decoction", "minecraft:leather"], "regnum:medkit")
shapeless("antidote", ["regnum:rot_root", "regnum:healing_herb", "minecraft:glass_bottle"], "regnum:antidote", 2)
shaped("star_blade", [" S ", " S ", " B "], {"S": "regnum:star_iron", "B": "minecraft:blaze_rod"}, "regnum:star_blade", cat="equipment")
shaped("sun_amulet", [" T ", "GAG", " G "], {"T": "minecraft:string", "G": "minecraft:gold_ingot", "A": "regnum:sun_amber"}, "regnum:sun_amulet", cat="equipment")
shapeless("survivor_journal", ["minecraft:book", "minecraft:feather"], "regnum:survivor_journal")

# ------------------------------------------------------------------ лут
L = f"{D}/regnum/loot_table"


def item_entry(name, weight=1, count=None, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = []
    if count:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    if extra:
        fns += extra
    if fns:
        e["functions"] = fns
    return e


def pool(entries, rolls):
    r = rolls if isinstance(rolls, (int, float)) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}
    return {"rolls": r, "entries": entries}


for b in CUBE_BLOCKS:
    w(f"{L}/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"regnum:{b}"}],
                                                                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

ENCH = [{"function": "minecraft:enchant_randomly"}]
w(f"{L}/chests/crypt.json", {"type": "minecraft:chest", "pools": [
    pool([item_entry("minecraft:bone", 10, (2, 6)), item_entry("minecraft:rotten_flesh", 8, (2, 5)),
          item_entry("minecraft:iron_ingot", 6, (1, 4)), item_entry("minecraft:gold_ingot", 5, (1, 3)),
          item_entry("minecraft:emerald", 5, (1, 4)), item_entry("regnum:bandage", 6, (1, 3)),
          item_entry("regnum:healing_herb", 5, (1, 3)), item_entry("minecraft:arrow", 5, (4, 12)),
          item_entry("minecraft:candle", 3, (1, 3))], (4, 8)),
    pool([item_entry("minecraft:book", 3, extra=ENCH), item_entry("minecraft:iron_sword", 2, extra=ENCH),
          item_entry("regnum:mastery_scroll", 3), item_entry("regnum:herbal_decoction", 3), item_entry("minecraft:diamond", 1)], (1, 2)),
]})
w(f"{L}/chests/crypt_treasure.json", {"type": "minecraft:chest", "pools": [
    pool([item_entry("minecraft:gold_ingot", 8, (3, 8)), item_entry("minecraft:emerald", 8, (4, 10)),
          item_entry("minecraft:diamond", 4, (1, 3)), item_entry("regnum:medkit", 3), item_entry("regnum:mastery_scroll", 4, (1, 2)),
          item_entry("minecraft:golden_apple", 3)], (4, 7)),
    pool([item_entry("minecraft:diamond_sword", 2, extra=ENCH), item_entry("minecraft:diamond_chestplate", 1, extra=ENCH),
          item_entry("minecraft:book", 4, extra=ENCH), item_entry("minecraft:enchanted_golden_apple", 1)], 1),
]})
w(f"{L}/chests/bandit_stash.json", {"type": "minecraft:chest", "pools": [
    pool([item_entry("minecraft:emerald", 10, (2, 8)), item_entry("minecraft:iron_ingot", 6, (2, 5)),
          item_entry("minecraft:bread", 6, (2, 5)), item_entry("minecraft:arrow", 5, (6, 16)),
          item_entry("regnum:bandage", 6, (1, 4)), item_entry("minecraft:leather", 4, (1, 4)),
          item_entry("regnum:trick_elixir", 2), item_entry("minecraft:gold_ingot", 3, (1, 3)),
          item_entry("regnum:spices", 3, (1, 3)), item_entry("regnum:silk", 2, (1, 2)), item_entry("regnum:wine", 3, (1, 3)),
          item_entry("regnum:amber", 1)], (4, 7)),
    pool([item_entry("minecraft:crossbow", 2, extra=ENCH), item_entry("minecraft:saddle", 2), item_entry("regnum:mastery_scroll", 2),
          item_entry("minecraft:diamond", 1)], (0, 1)),
]})
w(f"{L}/gameplay/wounded_reward.json", {"type": "minecraft:gift", "pools": [
    pool([item_entry("minecraft:emerald", 10, (3, 8)), item_entry("minecraft:iron_ingot", 6, (2, 5)),
          item_entry("minecraft:book", 4, extra=ENCH), item_entry("regnum:mastery_scroll", 3), item_entry("minecraft:golden_carrot", 4, (2, 4)),
          item_entry("regnum:medkit", 2)], (1, 2))]})
w(f"{L}/gameplay/hermit_reward.json", {"type": "minecraft:gift", "pools": [
    pool([item_entry("regnum:mastery_scroll", 8, (1, 2)), item_entry("minecraft:book", 6, extra=ENCH),
          item_entry("minecraft:experience_bottle", 4, (3, 6)), item_entry("minecraft:golden_apple", 2)], (1, 2))]})

KILLED = [{"condition": "minecraft:killed_by_player"}]
w(f"{L}/entities/crypt_lord.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "regnum:crypt_heart"}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "regnum:morgrath_blade"}], "conditions": KILLED},
    pool([item_entry("minecraft:emerald", 1, (8, 16))], 1),
    pool([item_entry("minecraft:bone", 1, (6, 12))], 1),
    pool([item_entry("minecraft:wither_skeleton_skull", 1)], 1),
]})
w(f"{L}/entities/bandit.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:emerald", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}], "conditions": KILLED},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "regnum:bandage"}],
     "conditions": [{"condition": "minecraft:random_chance", "chance": 0.15}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:arrow", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 3}}]}]},
]})

def boss_loot(trophy, unique, extra):
    return {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": trophy, "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": unique}], "conditions": KILLED},
        pool([item_entry("minecraft:emerald", 1, (10, 18))], 1),
        pool(extra, 1)]}


w(f"{L}/entities/mire_mother.json", boss_loot("regnum:rot_root", "regnum:mire_staff", [item_entry("regnum:antidote", 1, (2, 3))]))
w(f"{L}/entities/forgemaster.json", boss_loot("regnum:star_iron", "regnum:star_iron", [item_entry("minecraft:netherite_scrap", 1, (1, 2))]))
w(f"{L}/entities/scarab_queen.json", boss_loot("regnum:sun_amber", "regnum:sun_amber", [item_entry("minecraft:gold_block", 1, (2, 4))]))


def dungeon_chest(theme):
    return {"type": "minecraft:chest", "pools": [
        pool([item_entry("minecraft:emerald", 8, (2, 6)), item_entry("minecraft:iron_ingot", 6, (2, 5)),
              item_entry("minecraft:gold_ingot", 5, (1, 4)), item_entry("regnum:bandage", 6, (1, 3)),
              item_entry("regnum:mastery_scroll", 3)] + theme, (4, 8)),
        pool([item_entry("minecraft:book", 4, extra=ENCH), item_entry("minecraft:diamond", 2, (1, 2)),
              item_entry("regnum:medkit", 2), item_entry("minecraft:golden_apple", 2)], (1, 2))]}


w(f"{L}/chests/sunken_shrine.json", dungeon_chest([item_entry("regnum:healing_herb", 8, (2, 6)), item_entry("regnum:herb_bundle", 4, (1, 2)),
                                                   item_entry("minecraft:slime_ball", 5, (2, 5)), item_entry("regnum:herbal_decoction", 4)]))
w(f"{L}/chests/forge_fortress.json", dungeon_chest([item_entry("regnum:iron_goods", 6, (1, 3)), item_entry("minecraft:iron_block", 2),
                                                    item_entry("minecraft:coal", 6, (4, 12)), item_entry("minecraft:netherite_scrap", 1)]))
w(f"{L}/chests/sand_tomb.json", dungeon_chest([item_entry("regnum:spices", 6, (1, 3)), item_entry("regnum:amber", 3),
                                               item_entry("minecraft:gold_block", 2), item_entry("minecraft:lapis_lazuli", 6, (4, 10))]))

# ------------------------------------------------------------------ worldgen
w(f"{D}/regnum/worldgen/structure/crypt.json", {
    "type": "regnum:crypt",
    "biomes": "#regnum:has_structure/crypt",
    "step": "surface_structures",
    "spawn_overrides": {},
    "terrain_adaptation": "none"})
w(f"{D}/regnum/worldgen/structure_set/crypts.json", {
    "structures": [{"structure": "regnum:crypt", "weight": 1}],
    "placement": {"type": "minecraft:random_spread", "spacing": 36, "separation": 14, "salt": 739215461}})
w(f"{D}/regnum/tags/worldgen/biome/has_structure/crypt.json", {"values": [
    "#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains",
    "minecraft:swamp", "minecraft:meadow", "minecraft:savanna", "minecraft:windswept_hills", "minecraft:dark_forest",
    "minecraft:old_growth_birch_forest", "minecraft:cherry_grove"]})
w(f"{D}/regnum/tags/worldgen/structure/crypts.json", {"values": ["regnum:crypt"]})
for name, kind, biomes, spacing, salt in (
        ("sunken_shrine", "shrine", ["minecraft:swamp", "minecraft:mangrove_swamp"], 24, 518273641),
        ("forge_fortress", "fortress", ["#minecraft:is_mountain", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:stony_shore"], 32, 618273642),
        ("sand_tomb", "tomb", ["minecraft:desert", "#minecraft:is_badlands"], 30, 718273643)):
    w(f"{D}/regnum/worldgen/structure/{name}.json", {"type": "regnum:region_dungeon", "kind": kind, "biomes": f"#regnum:has_structure/{name}",
                                                    "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none"})
    w(f"{D}/regnum/worldgen/structure_set/{name}.json", {"structures": [{"structure": f"regnum:{name}", "weight": 1}],
                                                        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": spacing // 3, "salt": salt}})
    w(f"{D}/regnum/tags/worldgen/biome/has_structure/{name}.json", {"values": biomes})
    w(f"{D}/regnum/tags/worldgen/structure/{name}.json", {"values": [f"regnum:{name}"]})
w(f"{D}/regnum/tags/worldgen/structure/region_dungeons.json", {"values": ["regnum:crypt", "regnum:sunken_shrine", "regnum:forge_fortress", "regnum:sand_tomb"]})

# ------------------------------------------------------------------ урон, теги
w(f"{D}/regnum/damage_type/bleeding.json", {"exhaustion": 0.0, "message_id": "regnum.bleeding", "scaling": "never"})
w(f"{D}/minecraft/tags/damage_type/bypasses_armor.json", {"replace": False, "values": ["regnum:bleeding"]})
w(f"{D}/minecraft/tags/damage_type/no_knockback.json", {"replace": False, "values": ["regnum:bleeding"]})
w(f"{D}/minecraft/tags/entity_type/undead.json", {"replace": False, "values": ["regnum:crypt_lord", "regnum:scarab_queen"]})
w(f"{D}/minecraft/tags/entity_type/inverted_healing_and_harm.json", {"replace": False, "values": ["regnum:crypt_lord", "regnum:scarab_queen"]})
w(f"{D}/minecraft/tags/entity_type/can_breathe_under_water.json", {"replace": False, "values": ["regnum:mire_mother"]})
w(f"{D}/minecraft/tags/entity_type/wither_friends.json", {"replace": False, "values": ["regnum:crypt_lord"]})
w(f"{D}/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ["regnum:town_hall", "regnum:barracks", "regnum:market", "regnum:training_ground", "regnum:builder_hut", "regnum:library", "regnum:university", "regnum:infirmary", "regnum:smithy", "regnum:warehouse", "regnum:stable"]})
w(f"{D}/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": ["regnum:watchtower", "regnum:master_forge", "regnum:ember_core"]})
w(f"{L}/blocks/ember_core.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:magma_cream"}]}]})
print("ok")

# оружие культур (дописывает свои строки в язык и рецепты)
import gen_weapons  # noqa: E402
gen_weapons.main()
import gen_kit  # noqa: E402
gen_kit.main()
import gen_armor  # noqa: E402
gen_armor.main()
