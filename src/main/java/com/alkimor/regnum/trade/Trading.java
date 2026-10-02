package com.alkimor.regnum.trade;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.core.network.TradeActionPayload;
import com.alkimor.regnum.core.network.TradeInfoPayload;
import com.alkimor.regnum.kingdom.BuildingType;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.wanderers.WandererEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Торговля в духе Crimson Desert: региональные цены, спрос и предложение,
 * бонус за дальность доставки, пошлина в казну города.
 */
public final class Trading {
    private Trading() {}

    // ------------------------------------------------------------------ цены

    private static double pressureFactor(int p) {
        return Math.max(0.45, Math.min(1.6, 1.0 - 0.04 * p));
    }

    public static int buyPrice(TradeGood g, Region r, MarketState m, int lvl) {
        double v = g.base * g.factor(r) * TradeNews.factor(g) * pressureFactor(m.pressure[g.ordinal()]) * 1.15 * (1 - 0.002 * lvl);
        return Math.max(1, (int) Math.round(v));
    }

    public static double distanceBonus(@Nullable TradeOrigin o, BlockPos here, boolean routes) {
        if (o == null) return 1.0;
        double d = Math.sqrt(o.pos().distSqr(here));
        return 1.0 + Math.min(0.6, d / 800.0) * (routes ? 1.2 : 1.0);
    }

    /** Торговые бонусы героя: уровень навыка, перки, черты. lvl кодирует всё: младшие 7 бит — уровень, далее флаги. */
    public static int traderCode(ServerPlayer sp) {
        int code = Skills.level(sp, Skill.TRADE);
        if (Skills.has(sp, com.alkimor.regnum.survival.Perk.TR_PRINCE)) code |= 1 << 8;
        if (Skills.has(sp, com.alkimor.regnum.survival.Perk.TR_ROUTES)) code |= 1 << 9;
        if (Skills.has(sp, com.alkimor.regnum.survival.Trait.HAGGLER)) code |= 1 << 10;
        if (Skills.has(sp, com.alkimor.regnum.survival.Perk.TR_TRENDS)) code |= 1 << 11;
        return code;
    }

    public static int sellPrice(TradeGood g, Region r, MarketState m, int code, @Nullable TradeOrigin o, BlockPos here) {
        int lvl = code & 0x7F;
        double v = g.base * g.factor(r) * TradeNews.factor(g) * pressureFactor(m.pressure[g.ordinal()]) * 0.85 * (1 + 0.002 * lvl)
                * ((code & (1 << 8)) != 0 ? 1.1 : 1.0) * ((code & (1 << 10)) != 0 ? 1.05 : 1.0)
                * distanceBonus(o, here, (code & (1 << 9)) != 0);
        // нельзя продать туда же, где купил, с прибылью
        if (o != null && Region.byId(o.region()) == r && o.pos().closerThan(here, 64)) v = Math.min(v, g.base * g.factor(r) * 0.8);
        return Math.max(1, (int) Math.round(v));
    }

    // ------------------------------------------------------------------ точки торговли

    private record Market(String title, BlockPos pos, Region region, MarketState state, @Nullable City city) {}

    @Nullable
    private static Market resolve(ServerPlayer sp, boolean entity, long key) {
        ServerLevel level = sp.serverLevel();
        long day = level.getDayTime() / 24000L;
        if (entity) {
            Entity e = level.getEntity((int) key);
            if (!(e instanceof WandererEntity w) || !w.isAlive() || sp.distanceToSqr(w) > 10 * 10) return null;
            MarketState st = w.market(day);
            return new Market(w.shortName(), w.blockPosition(), Region.at(level, w.blockPosition()), st, null);
        }
        BlockPos pos = BlockPos.of(key);
        if (sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 10 * 10) return null;
        KingdomData kd = KingdomData.get(sp.server);
        City city = kd.at(pos);
        if (city != null && city.buildings.get(pos.asLong()) != BuildingType.MARKET) city = null;
        String title = city != null ? "Рынок города «" + city.name + "»" : "Рынок";
        return new Market(title, pos, Region.at(level, pos), TradeData.get(sp.server).market(key, day), city);
    }

    public static void openBlock(ServerPlayer sp, BlockPos pos) {
        send(sp, false, pos.asLong());
    }

    public static void openMerchant(ServerPlayer sp, WandererEntity w) {
        send(sp, true, w.getId());
    }

    private static void send(ServerPlayer sp, boolean entity, long key) {
        Market m = resolve(sp, entity, key);
        if (m == null) return;
        int code = traderCode(sp);
        int lvl = code & 0x7F;
        int n = TradeGood.values().length;
        int[] buy = new int[n], sell = new int[n], have = new int[n], trend = new int[n];
        for (TradeGood g : TradeGood.values()) {
            int i = g.ordinal();
            buy[i] = buyPrice(g, m.region(), m.state(), lvl);
            sell[i] = bestSellFor(sp, g, m, code);
            have[i] = InvUtil.count(sp, TradeModule.GOODS.get(g).get());
            if ((code & (1 << 11)) != 0) {
                double f = g.factor(m.region()) * pressureFactor(m.state().pressure[i]);
                trend[i] = f < 0.85 ? -1 : f > 1.15 ? 1 : 0;
            } else {
                trend[i] = 9;
            }
        }
        PacketDistributor.sendToPlayer(sp, new TradeInfoPayload(entity, key, m.title(), m.region().title, buy, sell, have, trend, lvl));
    }

    /** Цена продажи «лучшего» ящика игрока (с учётом дальности), или базовая, если ящиков нет. */
    private static int bestSellFor(ServerPlayer sp, TradeGood g, Market m, int lvl) {
        int best = 0;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof TradeGoodItem t && t.good == g) {
                best = Math.max(best, sellPrice(g, m.region(), m.state(), lvl, s.get(TradeModule.ORIGIN.get()), m.pos()));
            }
        }
        return best > 0 ? best : sellPrice(g, m.region(), m.state(), lvl, null, m.pos());
    }

    // ------------------------------------------------------------------ сделки

    public static void handle(ServerPlayer sp, TradeActionPayload p) {
        Market m = resolve(sp, p.entity(), p.key());
        if (m == null) {
            Text.bad(sp, "Вы отошли от прилавка.");
            return;
        }
        TradeGood g = TradeGood.byId(p.good());
        int code = traderCode(sp);
        int lvl = code & 0x7F;
        int amount = Math.max(-64, Math.min(64, p.amount()));
        int total = 0, done = 0;
        if (amount > 0) {
            for (int k = 0; k < amount; k++) {
                if (m.state().pressure[g.ordinal()] <= MarketState.MIN) {
                    Text.bad(sp, "Товар «" + g.title + "» здесь распродан. Загляните через пару дней.");
                    break;
                }
                int price = buyPrice(g, m.region(), m.state(), lvl);
                if (!InvUtil.take(sp, Items.EMERALD, price)) {
                    if (done == 0) Text.bad(sp, "Не хватает изумрудов (нужно " + price + ").");
                    break;
                }
                ItemStack crate = new ItemStack(TradeModule.GOODS.get(g).get());
                crate.set(TradeModule.ORIGIN.get(), new TradeOrigin(m.pos(), m.region().ordinal()));
                InvUtil.give(sp, crate);
                m.state().pressure[g.ordinal()]--;
                total += price;
                done++;
            }
            if (done > 0) {
                Text.good(sp, "Куплено: " + g.title + " ×" + done + " за " + total + " изумр.");
                Skills.addXp(sp, Skill.TRADE, done);
            }
        } else if (amount < 0) {
            int want = -amount;
            Inventory inv = sp.getInventory();
            for (int i = 0; i < inv.getContainerSize() && done < want; i++) {
                ItemStack s = inv.getItem(i);
                while (!s.isEmpty() && s.getItem() instanceof TradeGoodItem t && t.good == g && done < want) {
                    int price = sellPrice(g, m.region(), m.state(), code, s.get(TradeModule.ORIGIN.get()), m.pos());
                    s.shrink(1);
                    if (m.state().pressure[g.ordinal()] < MarketState.MAX) m.state().pressure[g.ordinal()]++;
                    total += price;
                    done++;
                }
            }
            if (done >= 8 && Skills.has(sp, com.alkimor.regnum.survival.Perk.TR_BULK)) total = Math.round(total * 1.05f);
            if (done > 0) {
                InvUtil.give(sp, new ItemStack(Items.EMERALD, total));
                Text.good(sp, "Продано: " + g.title + " ×" + done + " за " + total + " изумр.");
                Skills.addXp(sp, Skill.TRADE, Math.max(2, total / 3));
            } else {
                Text.bad(sp, "У вас нет товара «" + g.title + "».");
            }
        }
        if (done > 0) {
            sp.serverLevel().playSound(null, m.pos(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.8f, 1.1f);
            if (m.city() != null && !(m.city().owner.equals(sp.getUUID()) && Skills.has(sp, com.alkimor.regnum.survival.Perk.TR_TOLLS))) {
                int duty = Math.max(1, total * 5 / 100);
                m.city().treasury += duty;
                KingdomData.get(sp.server).setDirty();
            }
            TradeData.get(sp.server).setDirty();
        }
        send(sp, p.entity(), p.key());
    }

    /** Сколько ящиков товара несёт игрок. */
    public static int cratesCarried(ServerPlayer sp) {
        int n = 0;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof TradeGoodItem) n += s.getCount();
        }
        return n;
    }
}
