package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Map;

/** Починка построек, разрушенных при разграблении города: дерево и камень со склада, а если их нет — золото из казны. */
public final class Ruins {
    private Ruins() {}

    public static final int WOOD = 20, STONE = 15, GOLD = 90;

    /** Чинит одну постройку, возвращает текст результата (null — нечего чинить). */
    public static String repairOne(City c) {
        return repairOne(c, null);
    }

    /** Чинит одну постройку. С уровнем проверяет, что блок здания ещё стоит и лимит не нарушен. */
    public static String repairOne(City c, net.minecraft.server.level.ServerLevel sl) {
        if (c.ruined.isEmpty()) return null;
        Map.Entry<Long, BuildingType> e = c.ruined.entrySet().iterator().next();
        if (sl != null) {
            BlockPos pos = BlockPos.of(e.getKey());
            if (sl.isLoaded(pos) && !(sl.getBlockState(pos).getBlock() instanceof BuildingBlock)) {
                BuildingType t = e.getValue();
                c.ruined.remove(e.getKey());
                return "Блок «" + t.title + "» уже разрушен, запись удалена: постройте его заново.";
            }
            if (c.buildings.containsKey(e.getKey())) {
                c.ruined.remove(e.getKey());
                return "На этом месте уже стоит другая постройка, запись удалена.";
            }
        }
        String how;
        if (c.stock(Resource.WOOD) >= WOOD && c.stock(Resource.STONE) >= STONE) {
            c.take(Resource.WOOD, WOOD);
            c.take(Resource.STONE, STONE);
            how = "дерево " + WOOD + ", камень " + STONE;
        } else if (c.treasury >= GOLD) {
            c.treasury -= GOLD;
            how = GOLD + " монет";
        } else {
            return "Не хватает: нужно дерево " + WOOD + " и камень " + STONE + " на складе или " + GOLD + " монет в казне.";
        }
        c.ruined.remove(e.getKey());
        c.buildings.put(e.getKey(), e.getValue());
        return "Починено: " + e.getValue().title + " (" + how + ").";
    }

    /** Бесплатная починка одной постройки трудом пленных; null — чинить нечего. */
    public static String repairFree(City c) {
        if (c.ruined.isEmpty()) return null;
        Map.Entry<Long, BuildingType> e = c.ruined.entrySet().iterator().next();
        c.ruined.remove(e.getKey());
        c.buildings.put(e.getKey(), e.getValue());
        return "Починено трудом пленных: " + e.getValue().title;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("repair").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            KingdomData data = KingdomData.get(p.server);
            City c = data.nearestOwned(p.getUUID(), p.blockPosition());
            if (c == null) { Text.bad(p, "Рядом нет вашего города."); return 0; }
            if (c.ruined.isEmpty()) { Text.info(p, "В «" + c.name + "» нечего чинить."); return 1; }
            int done = 0;
            for (int i = 0, n = new ArrayList<>(c.ruined.keySet()).size(); i < n; i++) {
                String r = repairOne(c, p.serverLevel());
                if (r == null) break;
                if (r.startsWith("Не хватает")) { Text.bad(p, r); break; }
                if (r.startsWith("Починено")) { Text.good(p, r); done++; } else Text.info(p, r);
            }
            data.setDirty();
            Text.info(p, "Осталось разрушенных построек: " + c.ruined.size());
            return done;
        })));
    }
}
