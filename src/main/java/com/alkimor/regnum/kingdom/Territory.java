package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Жёсткая защита земель королевства (от бедрока до неба) и прочность построек на войне.
 * <ul>
 * <li>Мир: чужак не может ставить и ломать блоки, лить воду и лаву, поджигать, взрывать.</li>
 * <li>Война ({@link City#war}): защита снимается, но блоки не ломаются с одного удара — у каждого есть прочность;
 * установка блоков медленная (против столбов и ям). Осадные орудия и сапёры бьют через {@link #damage}.</li>
 * </ul>
 */
public final class Territory {
    private Territory() {}

    /** Прочность блоков под ударами: позиция → оставшаяся прочность. Забывается, если не бить. */
    private static final Map<Long, Fort> FORTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_PLACE = new HashMap<>();
    private static final int FORGET_TICKS = 1200, PLACE_COOLDOWN = 30;

    private static final class Fort {
        float hp;
        final float max;
        long lastHit;

        Fort(float max) {
            this.max = max;
            this.hp = max;
        }
    }

    // ------------------------------------------------------------------ запросы

    public static City cityAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return null;
        return KingdomData.get(sl.getServer()).at(pos);
    }

    public static boolean isFriend(City c, Player p) {
        return c.owner.equals(p.getUUID()) || c.trusted.contains(p.getUUID()) || p.isCreative() && p.hasPermissions(2);
    }

    /** Прочность блока в ударах киркой: чем твёрже блок, тем больше. Мягкие (трава, листья) ломаются сразу. */
    public static float maxHp(BlockState st, ServerLevel sl, BlockPos pos) {
        float hardness = st.getDestroySpeed(sl, pos);
        if (hardness < 0) return 1000f;
        if (hardness <= 0.3f) return 0f;
        return Math.max(3f, Math.min(80f, hardness * 6f));
    }

    /** Нанести урон постройке. Возвращает true, если блок разрушен (вызывающий ломает его сам). */
    public static boolean damage(ServerLevel sl, BlockPos pos, float amount) {
        BlockState st = sl.getBlockState(pos);
        float max = maxHp(st, sl, pos);
        if (max <= 0f) return true;
        long key = pos.asLong();
        long now = sl.getGameTime();
        Fort f = FORTS.get(key);
        if (f == null || now - f.lastHit > FORGET_TICKS) {
            f = new Fort(max);
            FORTS.put(key, f);
        }
        f.hp -= amount;
        f.lastHit = now;
        if (f.hp <= 0) {
            FORTS.remove(key);
            sl.destroyBlockProgress(pos.hashCode(), pos, -1);
            return true;
        }
        int stage = Math.min(9, (int) (10 * (1f - f.hp / f.max)));
        sl.destroyBlockProgress(pos.hashCode(), pos, stage);
        return false;
    }

    // ------------------------------------------------------------------ события

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        City c = cityAt(sl, event.getPos());
        if (c == null) return;
        Player p = event.getPlayer();
        if (isFriend(c, p)) return;
        if (!c.war) {
            deny(p, c);
            event.setCanceled(true);
            return;
        }
        // война: блок держится, пока его не пробьют
        float bonus = p.getMainHandItem().getDestroySpeed(event.getState()) > 4f ? 1.5f : 1f;
        if (damage(sl, event.getPos(), bonus)) return;
        event.setCanceled(true);
        sl.playSound(null, event.getPos(), event.getState().getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8f, 0.7f);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !(event.getEntity() instanceof Player p)) return;
        City c = cityAt(sl, event.getPos());
        if (c == null || isFriend(c, p)) return;
        if (!c.war) {
            deny(p, c);
            event.setCanceled(true);
            return;
        }
        long now = sl.getGameTime();
        Long last = LAST_PLACE.get(p.getUUID());
        if (last != null && now - last < PLACE_COOLDOWN) {
            p.displayClientMessage(Component.literal("В чужой крепости строить быстро нельзя").withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
            return;
        }
        LAST_PLACE.put(p.getUUID(), now);
    }

    /** Вода, лава и огонь у чужих стен. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack s = event.getItemStack();
        if (!(s.getItem() instanceof BucketItem) && !(s.getItem() instanceof FlintAndSteelItem) && !(s.getItem() instanceof FireChargeItem)) return;
        Player p = event.getEntity();
        City c = cityAt(p.level(), event.getPos().relative(event.getFace()));
        if (c == null || c.war || isFriend(c, p)) return;
        deny(p, c);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplode(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        Iterator<BlockPos> it = event.getAffectedBlocks().iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            City c = cityAt(sl, pos);
            if (c == null) continue;
            if (!c.war || !damage(sl, pos, 4f)) it.remove();
        }
        // рамки, картины и стойки в мирном городе взрыв не трогает
        event.getAffectedEntities().removeIf(e -> isDecor(e) && peaceCityAt(sl, e) != null);
    }

    private static boolean isDecor(net.minecraft.world.entity.Entity e) {
        return e instanceof net.minecraft.world.entity.decoration.HangingEntity || e instanceof net.minecraft.world.entity.decoration.ArmorStand;
    }

    private static City peaceCityAt(net.minecraft.world.level.Level lv, net.minecraft.world.entity.Entity e) {
        City c = cityAt(lv, e.blockPosition());
        return c != null && !c.war ? c : null;
    }

    /** Стрела или снаряд чужака не сбивает рамки, картины и стойки в мирном городе. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDecorShot(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit) || !isDecor(hit.getEntity())) return;
        City c = peaceCityAt(hit.getEntity().level(), hit.getEntity());
        if (c == null) return;
        if (event.getProjectile().getOwner() instanceof Player p && isFriend(c, p)) return;
        event.setCanceled(true);
    }

    /** Поршни снаружи не двигают чужие постройки: проверяем всю линию толчка до 13 блоков. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPiston(net.neoforged.neoforge.event.level.PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        City own = cityAt(sl, event.getPos());
        BlockPos p = event.getPos();
        for (int i = 1; i <= 13; i++) {
            p = p.relative(event.getDirection());
            City c = cityAt(sl, p);
            if (c != null && !c.war && c != own) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Чужак не вытаптывает поля королевства. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        net.minecraft.world.entity.Entity ent = event.getEntity();
        Player p = ent instanceof Player pl ? pl : ent.getControllingPassenger() instanceof Player rider ? rider : null;
        if (p == null) return;
        City c = cityAt(sl, event.getPos());
        if (c != null && !c.war && !isFriend(c, p)) event.setCanceled(true);
    }

    /** Сундуки, бочки, печи и склады — только своим. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onContainer(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel sl)) return;
        City c = cityAt(sl, event.getPos());
        if (c == null || c.war || isFriend(c, p)) return;
        if (!(sl.getBlockEntity(event.getPos()) instanceof net.minecraft.world.Container)) return;
        deny(p, c);
        event.setCanceled(true);
    }

    /** Рамки, картины и стойки для брони чужаку не тронуть. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHangingAttack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        guardDecor(event.getEntity(), event.getTarget(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHangingUse(PlayerInteractEvent.EntityInteract event) {
        guardDecor(event.getEntity(), event.getTarget(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHangingUseAt(PlayerInteractEvent.EntityInteractSpecific event) {
        guardDecor(event.getEntity(), event.getTarget(), event);
    }

    private static void guardDecor(Player p, net.minecraft.world.entity.Entity target, net.neoforged.bus.api.ICancellableEvent ev) {
        if (!(target instanceof net.minecraft.world.entity.decoration.HangingEntity) && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)) return;
        City c = cityAt(p.level(), target.blockPosition());
        if (c == null || c.war || isFriend(c, p)) return;
        deny(p, c);
        ev.setCanceled(true);
    }

    private static void deny(Player p, City c) {
        p.displayClientMessage(Component.literal("Земля королевства «" + c.name + "» под защитой. Только война снимет запрет.")
                .withStyle(ChatFormatting.RED), true);
        if (p instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.4f, 0.6f);
        }
    }

    // ------------------------------------------------------------------ команды

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum")
                .then(Commands.literal("trust")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                            City c = ownCity(p);
                            if (c == null) return 0;
                            c.trusted.add(t.getUUID());
                            KingdomData.get(p.server).setDirty();
                            Text.good(p, t.getName().getString() + " теперь может строить и ломать в «" + c.name + "».");
                            Text.info(t, p.getName().getString() + " доверяет вам землю «" + c.name + "».");
                            return 1;
                        })))
                        .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerPlayer t = EntityArgument.getPlayer(ctx, "player");
                            City c = ownCity(p);
                            if (c == null) return 0;
                            c.trusted.remove(t.getUUID());
                            KingdomData.get(p.server).setDirty();
                            Text.good(p, t.getName().getString() + " больше не доверенный в «" + c.name + "».");
                            return 1;
                        })))
                        .then(Commands.literal("list").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            City c = ownCity(p);
                            if (c == null) return 0;
                            StringBuilder sb = new StringBuilder();
                            for (UUID u : c.trusted) {
                                ServerPlayer o = p.server.getPlayerList().getPlayer(u);
                                sb.append(sb.isEmpty() ? "" : ", ").append(o != null ? o.getName().getString() : u.toString().substring(0, 8));
                            }
                            Text.info(p, "Доверенные «" + c.name + "»: " + (sb.isEmpty() ? "никого" : sb));
                            return 1;
                        })))
                // отладка до появления дипломатии: объявить/закончить войну за город, в котором стоит оп
                .then(Commands.literal("war").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("state", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            City c = cityAt(p.level(), p.blockPosition());
                            if (c == null) {
                                Text.bad(p, "Вы не на земле королевства.");
                                return 0;
                            }
                            c.war = StringArgumentType.getString(ctx, "state").equals("on");
                            KingdomData.get(p.server).setDirty();
                            p.sendSystemMessage(Text.of(c.war ? "⚔ Война за «" + c.name + "»: защита земли снята, стены держатся ударами."
                                    : "Мир в «" + c.name + "»: земля снова под защитой.", ChatFormatting.GOLD));
                            return 1;
                        }))));
    }

    private static City ownCity(ServerPlayer p) {
        City c = cityAt(p.level(), p.blockPosition());
        if (c == null || !c.owner.equals(p.getUUID())) {
            Text.bad(p, "Встаньте на землю своего города.");
            return null;
        }
        return c;
    }
}
