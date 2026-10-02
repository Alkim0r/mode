package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.RegnumConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;

/** Обработчики модуля «Выживание». */
public final class SurvivalEvents {
    private SurvivalEvents() {}

    // ---------- Опыт за бой и травмы ----------

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        float dmg = event.getNewDamage();
        if (dmg <= 0) return;

        // Травмы получает только игрок
        if (!(victim instanceof ServerPlayer player) || !Injuries.enabled() || player.isCreative()) return;

        if (src.is(DamageTypeTags.IS_FALL)) {
            float threshold = (Skills.level(player, Skill.ATHLETICS) >= 50 ? 7f : 5f) - (Skills.has(player, Trait.BRITTLE) ? 2.5f : 0f);
            if (dmg >= threshold && player.getRandom().nextFloat() < 0.35f + 0.08f * (dmg - threshold)) {
                Injuries.breakBone(player);
            }
            return;
        }

        Entity attacker = src.getEntity();
        Entity direct = src.getDirectEntity();
        boolean wound = direct != null && (attacker instanceof Mob || direct instanceof Projectile || attacker instanceof ServerPlayer)
                && !src.is(DamageTypeTags.IS_EXPLOSION) && !src.is(DamageTypeTags.IS_FIRE);
        if (wound) {
            float armorFactor = 1f - Math.min(0.75f, player.getArmorValue() / 40f);
            float chance = (0.12f + 0.04f * dmg) * armorFactor * RegnumConfig.BLEEDING_CHANCE_MULTIPLIER.get().floatValue()
                    * (Skills.has(player, Trait.BLEEDER) ? 1.5f : 1f);
            if (player.getRandom().nextFloat() < Math.min(0.6f, chance)) {
                Injuries.startBleeding(player);
            }
        }
        float infect = 0.12f * (Skills.has(player, Perk.MD_IMMUNE) ? 0.5f : 1f) * (Skills.has(player, Trait.IRON_GUT) ? 0.5f : 1f);
        if (attacker instanceof Zombie && player.getRandom().nextFloat() < infect) {
            Injuries.infect(player);
        }
    }

    // ---------- Выносливость и развитие инфекции ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;

        if (player.isSprinting() || player.isSwimming()) {
            Skills.addXp(player, Skill.ATHLETICS, 1);
        }
        Encumbrance.update(player);

        MobEffectInstance inf = player.getEffect(SurvivalModule.INFECTION);
        if (inf != null && !inf.isInfiniteDuration() && inf.getDuration() <= 40) {
            int next = inf.getAmplifier() + 1;
            player.removeEffect(SurvivalModule.INFECTION);
            if (next >= 2) {
                player.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, MobEffectInstance.INFINITE_DURATION, 2));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Инфекция в тяжёлой стадии! Срочно нужен отвар или аптечка.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
            } else {
                player.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, Injuries.INFECTION_STAGE_TICKS, next));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Вам становится хуже — инфекция прогрессирует.")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    // ---------- Собирательство ----------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();

        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            Skills.addXp(player, Skill.SCOUTING, 1);
            return;
        }

        boolean grass = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH) || state.is(Blocks.SWEET_BERRY_BUSH);
        boolean leaves = state.is(BlockTags.LEAVES);
        if (!grass && !leaves) return;

        int lvl = Skills.level(player, Skill.SCOUTING);
        float chance = (grass ? 0.05f + 0.0015f * lvl : 0.02f + 0.0008f * lvl) * (Skills.has(player, Perk.SC_TRACKER) ? 1.4f : 1f);
        if (player.getRandom().nextFloat() < chance) {
            ItemStack find = forageFind(player, lvl);
            if (Skills.has(player, Perk.SC_GATHER)) find.grow(find.getCount());
            Block.popResource(level, event.getPos(), find);
            if (Skills.has(player, Perk.SC_ROOT) && player.getRandom().nextFloat() < 0.05f) {
                Block.popResource(level, event.getPos(), new ItemStack(SurvivalModule.LIFE_ROOT.get()));
                com.alkimor.regnum.core.Text.gold(player, "Вы нашли редкий Корень жизни!");
            }
            Skills.addXp(player, Skill.SCOUTING, 3);
        } else if (player.getRandom().nextFloat() < 0.15f) {
            Skills.addXp(player, Skill.SCOUTING, 1);
        }
    }

    private static ItemStack forageFind(ServerPlayer player, int lvl) {
        float r = player.getRandom().nextFloat();
        if (r < 0.70f) return new ItemStack(SurvivalModule.HEALING_HERB.get());
        if (r < 0.80f) return new ItemStack(Items.STRING, 1 + player.getRandom().nextInt(2));
        if (r < 0.88f) return new ItemStack(Items.FLINT);
        if (r < 0.95f) return new ItemStack(Items.BONE_MEAL, 2);
        return lvl >= 50 ? new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(3)) : new ItemStack(SurvivalModule.HEALING_HERB.get(), 2);
    }

    // ---------- Лутинг: медицина в сундуках ----------

    private static final Set<String> MED_LOOT_TABLES = Set.of(
            "chests/simple_dungeon", "chests/abandoned_mineshaft", "chests/desert_pyramid", "chests/jungle_temple",
            "chests/shipwreck_supply", "chests/pillager_outpost", "chests/stronghold_corridor", "chests/igloo_chest",
            "chests/village/village_plains_house", "chests/village/village_taiga_house", "chests/village/village_savanna_house",
            "chests/village/village_snowy_house", "chests/village/village_desert_house", "chests/village/village_temple",
            "chests/village/village_weaponsmith", "chests/ruined_portal", "chests/trial_chambers/supply"
    );

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace()) || !MED_LOOT_TABLES.contains(name.getPath())) return;
        event.getTable().addPool(LootPool.lootPool()
                .name("regnum_medical")
                .setRolls(UniformGenerator.between(0, 2))
                .add(LootItem.lootTableItem(SurvivalModule.BANDAGE.get()).setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .add(LootItem.lootTableItem(SurvivalModule.HEALING_HERB.get()).setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                .add(LootItem.lootTableItem(SurvivalModule.SPLINT.get()).setWeight(4))
                .add(LootItem.lootTableItem(SurvivalModule.HERBAL_DECOCTION.get()).setWeight(4))
                .add(LootItem.lootTableItem(SurvivalModule.MEDKIT.get()).setWeight(1))
                .build());
    }

    // ---------- Вход / возрождение ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SurvivorData d = Skills.data(sp);
            d.addCharXp(0);
            PlayerStats.apply(sp);
            Skills.sync(sp, false);
            if (!d.created()) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("⚜ Ваш герой ещё не создан. ")
                        .withStyle(net.minecraft.ChatFormatting.GOLD)
                        .append(net.minecraft.network.chat.Component.literal("[Создать героя]")
                                .withStyle(st -> st.withColor(net.minecraft.ChatFormatting.GREEN).withUnderlined(true)
                                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/regnum hero"))))
                        .append(net.minecraft.network.chat.Component.literal("  (класс, атрибуты, черты; можно и позже — /regnum hero)")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)));
            } else if (d.freeFocus > 0 || d.freeAttr > 0 || !d.pendingPerks().isEmpty()) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("⚜ Есть нераспределённые очки или выбор перков — откройте дневник героя.")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerStats.apply(sp);
            Skills.sync(sp, false);
        }
    }
}
