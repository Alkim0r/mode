package com.alkimor.regnum.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Общий конфиг: config/regnum-common.toml */
public final class RegnumConfig {
    private RegnumConfig() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // --- Выживание ---
    public static final ModConfigSpec.BooleanValue SURVIVAL_ENABLED;
    public static final ModConfigSpec.DoubleValue SKILL_XP_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue INJURIES_ENABLED;
    public static final ModConfigSpec.DoubleValue BLEEDING_CHANCE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENCUMBRANCE_ENABLED;
    public static final ModConfigSpec.DoubleValue SKILL_LOSS_ON_DEATH;

    // --- Королевство ---
    public static final ModConfigSpec.BooleanValue KINGDOM_ENABLED;
    public static final ModConfigSpec.IntValue MAX_CITIES_PER_PLAYER;
    public static final ModConfigSpec.BooleanValue RAIDS_ENABLED;
    public static final ModConfigSpec.IntValue RAID_INTERVAL_DAYS;
    public static final ModConfigSpec.BooleanValue UPKEEP_ENABLED;
    public static final ModConfigSpec.IntValue TAX_PER_VILLAGER;

    // --- Сложность ---
    public enum Preset { NORMAL, HARD, NIGHTMARE }
    public static final ModConfigSpec.EnumValue<Preset> PRESET;
    public static final ModConfigSpec.BooleanValue SMART_MOBS;
    public static final ModConfigSpec.BooleanValue ARMY_LOD;
    public static final ModConfigSpec.BooleanValue MOB_PARRY;
    public static final ModConfigSpec.DoubleValue SOLDIER_DAMAGE_TO_BOSSES;
    public static final ModConfigSpec.DoubleValue BOSS_HP_PER_EXTRA_PLAYER;
    public static final ModConfigSpec.BooleanValue LOOT_SCARCITY;
    public static final ModConfigSpec.IntValue TACTICS_RANGE;

    // --- Странники ---
    public static final ModConfigSpec.BooleanValue WANDERERS_ENABLED;
    public static final ModConfigSpec.DoubleValue WANDERER_SPAWN_CHANCE;

    static {
        B.comment("Модуль «Выживание»: навыки, травмы, медицина").push("survival");
        SURVIVAL_ENABLED = B.comment("Включить прокачку навыков").define("enabled", true);
        SKILL_XP_MULTIPLIER = B.comment("Множитель опыта навыков").defineInRange("skillXpMultiplier", 1.0, 0.1, 20.0);
        INJURIES_ENABLED = B.comment("Кровотечения, переломы, инфекции").define("injuries", true);
        BLEEDING_CHANCE_MULTIPLIER = B.comment("Множитель шанса кровотечения").defineInRange("bleedingChanceMultiplier", 1.0, 0.0, 5.0);
        ENCUMBRANCE_ENABLED = B.comment("Перегруз: полный инвентарь замедляет").define("encumbrance", true);
        SKILL_LOSS_ON_DEATH = B.comment("Доля опыта навыков, теряемая при смерти (0 — без потерь). Взрослый наследник позволяет избежать потерь.")
                .defineInRange("skillLossOnDeath", 0.25, 0.0, 1.0);
        B.pop();

        B.comment("Модуль «Королевство»: города, армия, набеги").push("kingdom");
        KINGDOM_ENABLED = B.define("enabled", true);
        MAX_CITIES_PER_PLAYER = B.comment("Сколько городов может основать один игрок").defineInRange("maxCitiesPerPlayer", 5, 1, 64);
        RAIDS_ENABLED = B.comment("Набеги разбойников на города").define("raids", true);
        RAID_INTERVAL_DAYS = B.comment("Раз в сколько игровых дней приходит набег").defineInRange("raidIntervalDays", 3, 1, 100);
        UPKEEP_ENABLED = B.comment("Ежедневное жалованье армии из казны").define("upkeep", true);
        TAX_PER_VILLAGER = B.comment("Налог с одного жителя в день (изумруды)").defineInRange("taxPerVillager", 1, 0, 64);
        B.pop();

        B.comment("Сложность: умные враги, тяжёлые боссы, дефицит").push("difficulty");
        PRESET = B.comment("Пресет: NORMAL — мягко, HARD — по умолчанию (бывалые и ветераны среди врагов, боссы крепче), NIGHTMARE — элита повсюду")
                .defineEnum("preset", Preset.HARD);
        SMART_MOBS = B.comment("Враги дерутся как игроки: уклоны, отскоки, обход, уход от стрел, отступление на лечение")
                .define("smartMobs", true);
        MOB_PARRY = B.comment("Ветераны и элита парируют слабые удары (наказывают за «закликивание»)").define("mobParry", true);
        SOLDIER_DAMAGE_TO_BOSSES = B.comment("Доля урона солдат по боссам (армией босса не задавить)")
                .defineInRange("soldierDamageToBosses", 0.35, 0.0, 1.0);
        BOSS_HP_PER_EXTRA_PLAYER = B.comment("Прибавка здоровья босса за каждого дополнительного игрока рядом")
                .defineInRange("bossHpPerExtraPlayer", 0.75, 0.0, 3.0);
        LOOT_SCARCITY = B.comment("Дефицит: в сундуках меньше алмазов, золота, книг чар и золотых яблок")
                .define("lootScarcity", true);
        TACTICS_RANGE = B.comment("Тактика включается только у врагов ближе этого расстояния к игроку (оптимизация)")
                .defineInRange("tacticsRange", 32, 8, 96);
        ARMY_LOD = B.comment("Оптимизация для слабых серверов: мирные солдаты вдали от игроков (72+ блока) думают в 4 раза реже")
                .define("armyLod", true);
        B.pop();

        B.comment("Модуль «Странники»: NPC с характером").push("wanderers");
        WANDERERS_ENABLED = B.define("enabled", true);
        WANDERER_SPAWN_CHANCE = B.comment("Шанс появления странника рядом с игроком раз в минуту").defineInRange("spawnChance", 0.12, 0.0, 1.0);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    public static Preset preset() {
        return PRESET.get();
    }

    /** 0 — обычный, 1 — тяжёлый, 2 — кошмар. */
    public static int tier() {
        return PRESET.get().ordinal();
    }
}
