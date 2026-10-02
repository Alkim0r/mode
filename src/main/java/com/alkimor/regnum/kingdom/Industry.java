package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Запасы города: провиант, дерево, камень, железо, травы, порох. Игроки сдают предметы на Склад (ПКМ по блоку склада,
 * Shift — все подходящие). Каждый день город кормит людей и армию, кузница тратит железо, склады спасают от порчи.
 */
public final class Industry {
    private Industry() {}

    public static final int HUNGER_DESERT_DAYS = 3;

    // ------------------------------------------------------------------ сдача на склад

    /** Сдать предметы из рук (или все подходящие при Shift). Возвращает true, если что-то сдано. */
    public static boolean deposit(ServerPlayer sp, City c, boolean all) {
        if (!Council.can(c, sp.getUUID(), Council.Role.STOCK)) return false;
        int total = 0;
        StringBuilder sb = new StringBuilder();
        java.util.EnumMap<Resource, Integer> got = new java.util.EnumMap<>(Resource.class);
        List<ItemStack> stacks = new ArrayList<>();
        if (all) stacks.addAll(sp.getInventory().items);
        else stacks.add(sp.getMainHandItem());
        for (ItemStack s : stacks) {
            Resource.Value v = Resource.of(s);
            if (v == null) continue;
            int room = Math.max(0, c.capacity() - c.stock(v.res()));
            int items = Math.min(s.getCount(), room / v.units());
            if (items <= 0) continue;
            int added = c.deposit(v.res(), items * v.units());
            int used = Math.max(1, added / v.units());
            s.shrink(Math.min(used, s.getCount()));
            got.merge(v.res(), added, Integer::sum);
            total += added;
        }
        if (total == 0) {
            Text.info(sp, "Склад не принял: предмет не годится или полки полны (до " + c.capacity() + " каждого ресурса).");
            return false;
        }
        got.forEach((r, n) -> sb.append(r.title).append(" +").append(n).append("  "));
        Text.good(sp, "Сдано на склад «" + c.name + "»: " + sb.toString().trim());
        Skills_xp(sp, total);
        c.addContrib(sp.getUUID(), Math.max(1, total / 4));
        KingdomData.get(sp.server).setDirty();
        return true;
    }

    private static void Skills_xp(ServerPlayer sp, int total) {
        com.alkimor.regnum.survival.Skills.addXp(sp, com.alkimor.regnum.survival.Skill.STEWARD, Math.max(1, total / 20));
    }

    // ------------------------------------------------------------------ ежедневный цикл

    /** Суточный баланс провианта: приход, расход, порча, итог. */
    public record FoodBalance(int produce, int eatPeople, int eatArmy, int eatHorses, int spoil, int net) {
        /** Через сколько суток запас закончится (-1, если баланс не отрицательный). */
        public int daysLeft(int stock) {
            return net >= 0 ? -1 : (int) Math.ceil(stock / (double) -net);
        }
    }

    public static FoodBalance foodBalance(ServerLevel ow, City c) {
        int pop = c.population, soldiers = c.soldiers.size();
        boolean agri = Science.has(ow.getServer(), c.owner, Science.Tech.AGRICULTURE);
        int produce = (int) Math.round(pop * (agri ? 1.35 : 1.15) * Priority.foodMult(c));
        if (c.droughtDays > 0) produce /= 2;
        int mounted = 0;
        for (int v : c.soldiers.values()) if (SoldierType.byId(v).mounted()) mounted++;
        int spoil = c.stock(Resource.FOOD) / (c.count(BuildingType.WAREHOUSE) > 0 ? 60 : 25);
        return new FoodBalance(produce, pop, soldiers, mounted, spoil, produce - pop - soldiers - mounted - spoil);
    }

    /** Строки «баланса за сутки» с объяснением причин (идея I033). */
    public static List<String> balanceReport(ServerLevel ow, City c) {
        List<String> out = new ArrayList<>();
        FoodBalance b = foodBalance(ow, c);
        out.add("Провиант: " + c.stock(Resource.FOOD) + " (приход +" + b.produce() + ", едят жители −" + b.eatPeople() + ", армия −" + b.eatArmy()
                + (b.eatHorses() > 0 ? ", кони −" + b.eatHorses() : "") + ", порча −" + b.spoil() + " = " + (b.net() >= 0 ? "+" : "") + b.net() + "/сут.)");
        int dl = b.daysLeft(c.stock(Resource.FOOD));
        if (dl >= 0) out.add("  ⚠ Запас закончится примерно через " + dl + " дн. Причина: " + (b.eatArmy() + b.eatHorses() > b.eatPeople() / 2 ? "большая армия" : "мало рук и полей")
                + (c.count(BuildingType.WAREHOUSE) == 0 ? "; без Склада еда портится быстрее" : "") + ".");
        int smithies = c.count(BuildingType.SMITHY);
        if (smithies > 0) {
            int need = Math.max(1, (int) Math.round((1 + c.soldiers.size() / 5) * (c.mech > 0 ? 0.7 : 1.0) * (c.spec == 1 ? 0.7 : 1.0)));
            out.add("Железо: " + c.stock(Resource.IRON) + " (кузница тратит " + need + "/сут., хватит на " + (c.stock(Resource.IRON) / need) + " дн.)");
        } else {
            out.add("Железо: " + c.stock(Resource.IRON) + " (расходуется только на ковку и орудия)");
        }
        if (c.plagueDays > 0) out.add("Травы: " + c.stock(Resource.HERBS) + " — идёт эпидемия, лазарет и карантин расходуют запас.");
        else out.add("Травы: " + c.stock(Resource.HERBS) + " — резерв на случай эпидемии.");
        out.add("Дерево " + c.stock(Resource.WOOD) + ", камень " + c.stock(Resource.STONE) + ", порох " + c.stock(Resource.GUNPOWDER) + " — тратятся на развитие, стены и орудия.");
        com.alkimor.regnum.kingdom.KingdomData kd = com.alkimor.regnum.kingdom.KingdomData.get(ow.getServer());
        int tax = c.dailyIncome(com.alkimor.regnum.core.RegnumConfig.TAX_PER_VILLAGER.get());
        int bonus = Realms.dailyBonus(kd, c) + Villages.dailyBonus(kd, c);
        int upkeep = com.alkimor.regnum.core.RegnumConfig.UPKEEP_ENABLED.get() ? c.dailyUpkeep() : 0;
        int net = tax + bonus - upkeep;
        out.add("Казна: " + c.treasury + " монет. Приход: налоги " + tax + (bonus != 0 ? ", торговля и деревни +" + bonus : "") + "; расход: жалованье армии " + upkeep
                + " = " + (net >= 0 ? "+" : "") + net + "/сут." + (net < 0 && c.treasury > 0 ? " Казна опустеет примерно через " + (c.treasury / -net) + " дн. — тогда бойцы начнут уходить." : ""));
        return out;
    }

    /** Вызывается из экономики города раз в сутки. Возвращает строку для сводки владельца. */
    public static String daily(ServerLevel ow, KingdomData data, City c) {
        int pop = c.population, soldiers = c.soldiers.size();
        boolean agri = Science.has(ow.getServer(), c.owner, Science.Tech.AGRICULTURE);
        int produce = (int) Math.round(pop * (agri ? 1.35 : 1.15));
        int mounted = 0;
        for (int v : c.soldiers.values()) if (SoldierType.byId(v).mounted()) mounted++;
        int eat = pop + soldiers + mounted;
        int spoil = c.stock(Resource.FOOD) / (c.count(BuildingType.WAREHOUSE) > 0 ? 60 : 25);
        int net = produce - eat - spoil;
        int food = c.stock(Resource.FOOD) + net;
        String msg;
        if (food >= 0) {
            c.stock.put(Resource.FOOD, Math.min(food, c.capacity()));
            c.hungerDays = 0;
            msg = "провиант " + c.stock(Resource.FOOD) + " (" + (net >= 0 ? "+" : "") + net + ")";
        } else {
            c.stock.put(Resource.FOOD, 0);
            c.hungerDays++;
            msg = "ГОЛОД " + c.hungerDays + " дн.";
            c.treasury -= Math.min(c.treasury, c.dailyIncome(2) / 4);
            if (c.hungerDays >= HUNGER_DESERT_DAYS && !c.soldiers.isEmpty() && c.postPlagueDays <= 0) {
                UUID u = new ArrayList<>(c.soldiers.keySet()).get(ow.random.nextInt(c.soldiers.size()));
                c.soldiers.remove(u);
                msg += ", боец ушёл искать еду";
            }
        }
        if (c.quarantine) c.treasury -= Math.min(c.treasury, Math.max(1, c.dailyIncome(2) / 6));
        if (c.droughtDays > 0 && --c.droughtDays == 0) {
            // цепочка событий: засуха → голодный бунт, если запасы пусты, иначе город пережил её
            if (c.stock(Resource.FOOD) < 20) {
                int loss = Math.max(1, c.treasury / 10);
                c.treasury -= loss;
                c.hungerDays = Math.max(c.hungerDays, 1);
                msg += ", после засухи — голодный бунт (−" + loss + " в казне)";
            } else {
                msg += ", засуха кончилась, запасы выручили";
            }
        }
        c.mech = Compat.mechanization(ow, c);
        if (c.mech > 0) msg += ", механизмы x" + c.mech;
        // кузница
        int smithies = c.count(BuildingType.SMITHY);
        if (smithies == 0) {
            c.forged = 0;
        } else {
            int need = Math.max(1, (int) Math.round((1 + c.soldiers.size() / 5) * (c.mech > 0 ? 0.7 : 1.0)));
            if (c.take(Resource.IRON, need)) {
                c.forged = Math.min(3, smithies + (Science.has(ow.getServer(), c.owner, Science.Tech.STEEL) ? 1 : 0) + (c.spec == 1 ? 1 : 0));
                msg += ", кузница −" + need + " железа (броня +" + c.forged + ")";
            } else {
                c.forged = 0;
                msg += ", кузне нечем ковать (нужно " + need + " железа)";
            }
        }
        String labor = Labor.daily(c);
        if (!labor.isEmpty()) msg += ", " + labor;
        String health = Health.daily(ow, data, c);
        if (!health.isEmpty()) msg += ", " + health;
        return msg;
    }

    // ------------------------------------------------------------------ команды

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("stock")
                .executes(ctx -> show(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("report").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null) { Text.info(p, "Рядом нет вашего города."); return 0; }
                    p.sendSystemMessage(Text.of("— Баланс «" + c.name + "» за сутки —", ChatFormatting.GOLD));
                    for (String l : balanceReport(p.serverLevel(), c)) p.sendSystemMessage(Text.of(l, ChatFormatting.GRAY));
                    return 1;
                }))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("res", StringArgumentType.word())
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 5000)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                                    Resource r = Resource.byName(StringArgumentType.getString(ctx, "res"));
                                    if (c == null || r == null) return 0;
                                    c.deposit(r, IntegerArgumentType.getInteger(ctx, "n"));
                                    KingdomData.get(p.server).setDirty();
                                    return 1;
                                }))))));
    }

    private static int show(ServerPlayer p) {
        City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
        if (c == null) {
            Text.info(p, "Рядом нет вашего города.");
            return 0;
        }
        p.sendSystemMessage(Text.of("— Склад «" + c.name + "» (до " + c.capacity() + ") —", ChatFormatting.GOLD));
        for (Resource r : Resource.values()) {
            p.sendSystemMessage(Text.of("  " + r.title + ": " + c.stock(r) + " — " + r.use, ChatFormatting.GRAY));
        }
        if (c.plagueDays > 0) p.sendSystemMessage(Text.of("  ☣ Эпидемия: ещё " + c.plagueDays + " дн., сила " + c.plagueLevel + (c.quarantine ? " (карантин)" : ""), ChatFormatting.RED));
        if (c.hungerDays > 0) p.sendSystemMessage(Text.of("  ⚠ Голод: " + c.hungerDays + " дн.", ChatFormatting.RED));
        if (c.forged > 0) p.sendSystemMessage(Text.of("  ⚒ Кузница: броня бойцов +" + c.forged, ChatFormatting.GREEN));
        return 1;
    }
}
