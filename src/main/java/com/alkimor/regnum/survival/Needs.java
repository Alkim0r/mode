package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Жажда и тепло (идея из «Tough As Nails», переписано под Regnum, без чужого кода и ресурсов).
 * <p>Жажда 0…1000: тратится от бега, жары и боя; пьют воду из фляг/бутылок, котлов, рек (сырая вода рискованна),
 * едят сочное. Тепло −100…+100: тянется к температуре местности с поправками на ночь, высоту, подземелье, дождь,
 * огонь рядом и одежду. Холод и жара накладывают штрафы, крайности — урон.</p>
 */
public final class Needs {
    private Needs() {}

    public static final String THIRST = "regnum_thirst", WARMTH = "regnum_warmth", RAW_DAY = "regnum_rawday";
    public static final int MAX_THIRST = 1000, PARCHED = 200;

    public static int thirst(Player p) {
        var pd = p.getPersistentData();
        return pd.contains(THIRST) ? pd.getInt(THIRST) : MAX_THIRST;
    }

    public static int warmth(Player p) {
        return p.getPersistentData().getInt(WARMTH);
    }

    public static void drink(Player p, int amount) {
        p.getPersistentData().putInt(THIRST, Math.min(MAX_THIRST, thirst(p) + amount));
    }

    public static String thirstTitle(int t) {
        return t >= 700 ? "напились" : t >= 400 ? "лёгкая жажда" : t >= PARCHED ? "мучит жажда" : t > 0 ? "горло пересохло" : "обезвоживание";
    }

    public static String warmthTitle(int w) {
        return w <= -85 ? "лютый холод" : w <= -60 ? "озноб" : w <= -25 ? "прохладно" : w < 25 ? "комфортно" : w < 60 ? "тепло" : w < 85 ? "жара" : "тепловой удар";
    }

    /** Температура окружения в «условных единицах» (−150…+150), без инерции тела. */
    public static int ambient(Player p) {
        var level = p.level();
        BlockPos pos = p.blockPosition();
        float base = level.getBiome(pos).value().getBaseTemperature();
        double a = (base - 0.8) / 0.9 * 60.0; // равнины ≈ 0, пустыня ≈ +80, тундра ≈ −85
        long dt = level.getDayTime() % 24000L;
        boolean night = dt >= 13000 && dt <= 23000;
        boolean sky = level.canSeeSky(pos);
        if (night && sky) a -= 18;
        if (!night && sky && base > 1.0f) a += 10; // дневное пекло
        if (pos.getY() > 110) a -= (pos.getY() - 110) * 0.6;
        if (pos.getY() < 45) a = a * 0.4 - 6; // под землёй ровнее и прохладнее
        if (p.isInWaterOrRain()) a -= p.isInWater() ? 30 : 15;
        // тепло от огня рядом
        int heat = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3 && heat < 60; dx++) for (int dy = -1; dy <= 2 && heat < 60; dy++) for (int dz = -3; dz <= 3 && heat < 60; dz++) {
            m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
            BlockState s = level.getBlockState(m);
            if (s.isAir()) continue;
            if (s.is(BlockTags.FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)) heat += 18;
            else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) heat += 25;
            else if (s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT)
                    && (s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER))) heat += 12;
        }
        a += Seasons.warmthOffset(level);
        a += Math.min(60, heat);
        // одежда: каждая часть брони греет (кожаная лучше), в жару — наоборот добавляет духоту
        double ins = 0;
        for (EquipmentSlot es : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(es);
            if (st.isEmpty()) continue;
            ins += st.is(Items.LEATHER_HELMET) || st.is(Items.LEATHER_CHESTPLATE) || st.is(Items.LEATHER_LEGGINGS) || st.is(Items.LEATHER_BOOTS) ? 9 : 6;
        }
        if (a < 0) a = Math.min(0, a + ins); else a += ins * 0.35;
        return (int) Math.max(-150, Math.min(150, a));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || p.tickCount % 40 != 0) return;
        if (!Injuries.enabled()) return;
        var pd = p.getPersistentData();
        // ----- тепло: тело тянется к окружению
        int w = warmth(p), amb = ambient(p);
        int target = Math.max(-100, Math.min(100, amb));
        int nw = w + Integer.signum(target - w) * Math.min(Math.abs(target - w), p.isSprinting() ? 4 : 2);
        pd.putInt(WARMTH, nw);
        if (nw != w && ((w > -60 && nw <= -60) || (w < 60 && nw >= 60) || (w > -25 && nw <= -25) || (w < 25 && nw >= 25)))
            p.displayClientMessage(Text.of(nw < 0 ? "Вам " + warmthTitle(nw) + ". Тёплая одежда и костёр помогут." : "Вам " + warmthTitle(nw) + ". Найдите тень и воду.",
                    nw < 0 ? net.minecraft.ChatFormatting.AQUA : net.minecraft.ChatFormatting.GOLD), true);
        if (nw <= -60) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, nw <= -85 ? 1 : 0, false, false, true));
            if (nw <= -85) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0, false, false, true));
                p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 40, p.getTicksFrozen() + 60)); // ледяной урон как от снега
            }
        } else if (nw >= 60) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false, true));
            if (nw >= 85) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, true));
        }
        // ----- жажда
        int t = thirst(p), before = t;
        int drain = 1;
        if (p.isSprinting()) drain += 2;
        if (nw >= 60) drain += 2; else if (nw >= 25) drain += 1;
        if (p.hasEffect(SurvivalModule.INFECTION)) drain += 1;
        // питьё из реки: присесть по пояс в воде с пустой рукой
        if (p.isInWater() && p.isCrouching() && p.getMainHandItem().isEmpty() && t < MAX_THIRST) {
            t += 90;
            long day = p.serverLevel().getGameTime() / 24000L;
            if (p.getRandom().nextFloat() < 0.15f && !(p.level().getBiome(p.blockPosition()).value().getBaseTemperature() < 0.1f)) {
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                p.displayClientMessage(Text.of("Сырая вода оказалась дурной. Кипячёная или из фляги — безопаснее.", net.minecraft.ChatFormatting.DARK_GREEN), false);
            }
            pd.putLong(RAW_DAY, day);
        }
        t = Math.max(0, Math.min(MAX_THIRST, t - drain));
        pd.putInt(THIRST, t);
        if (before >= PARCHED && t < PARCHED)
            p.displayClientMessage(Text.of("Вас мучит жажда. Найдите воду.", net.minecraft.ChatFormatting.GOLD), true);
        if (t < PARCHED) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, true));
        if (t <= 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, false, true));
            // обезвоживание бьёт до половины сердца, не добивает
            if (p.getHealth() > 2.0f && p.tickCount % 120 == 0) p.hurt(p.damageSources().starve(), 1.0f);
        }
    }

    /** Вода из бутылки/фляги, молоко и сочная еда. */
    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack s = event.getItem();
        int add = 0;
        if (s.is(Items.POTION)) {
            var pc = s.get(DataComponents.POTION_CONTENTS);
            add = pc != null && pc.is(Potions.WATER) ? 300 : 80;
        } else if (s.is(Items.MILK_BUCKET)) add = 250;
        else if (s.is(Items.HONEY_BOTTLE)) add = 120;
        else if (s.is(Items.MELON_SLICE)) add = 90;
        else if (s.is(Items.APPLE) || s.is(Items.GLOW_BERRIES) || s.is(Items.SWEET_BERRIES) || s.is(Items.CARROT)) add = 45;
        else if (s.is(Items.MUSHROOM_STEW) || s.is(Items.BEETROOT_SOUP) || s.is(Items.RABBIT_STEW) || s.is(Items.SUSPICIOUS_STEW)) add = 120;
        if (add > 0) drink(p, add);
    }

    /** Котёл с водой: присядьте и ПКМ пустой рукой — глоток. */
    @SubscribeEvent
    public static void onCauldron(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!p.isCrouching() || !p.getMainHandItem().isEmpty() || thirst(p) >= MAX_THIRST - 50) return;
        BlockState st = p.level().getBlockState(event.getPos());
        if (!st.is(Blocks.WATER_CAULDRON)) return;
        drink(p, 160);
        LayeredCauldronBlock.lowerFillLevel(st, p.level(), event.getPos());
        p.displayClientMessage(Text.of("Вы пьёте из котла.", net.minecraft.ChatFormatting.AQUA), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("needs")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Нужды ══");
                    Text.info(p, "Жажда: " + thirst(p) + "/" + MAX_THIRST + " — " + thirstTitle(thirst(p)));
                    Text.info(p, "Сезон: " + Seasons.name(p.level()) + ", день " + Seasons.dayInSeason(p.level()) + "/" + Seasons.DAYS);
                    Text.info(p, "Тепло: " + warmth(p) + " — " + warmthTitle(warmth(p)) + " (окружение: " + ambient(p) + ")");
                    Text.info(p, "Пейте воду из бутылок и котлов (присесть + ПКМ), из реки (присесть по пояс в воде, риск), ешьте сочное. Тёплая одежда и костёр спасают от холода.");
                    return 1;
                })
                .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("thirst", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, MAX_THIRST))
                                .then(Commands.argument("warmth", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-100, 100)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    p.getPersistentData().putInt(THIRST, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "thirst"));
                                    p.getPersistentData().putInt(WARMTH, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "warmth"));
                                    return 1;
                                }))))));
    }
}
