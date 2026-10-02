package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.dungeon.RegionsModule;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Кампания «Хроника»: сквозная цепочка заданий от создания героя до титула императора.
 * Прогресс — в persistent-данных игрока: шаг, счётчики убийств.
 */
public final class Campaign {
    private Campaign() {}

    public record Step(String title, String hint, BiPredicate<ServerPlayer, CompoundTag> done, String reward, int count, int xp) {}

    private static final String TAG = "regnum_campaign";

    private static City city(ServerPlayer p) {
        List<City> l = KingdomData.get(p.server).ownedBy(p.getUUID());
        City best = null;
        for (City c : l) if (best == null || c.level > best.level) best = c;
        return best;
    }

    private static boolean has(City c, BuildingType t) {
        return c != null && c.buildings.values().stream().anyMatch(b -> b == t);
    }

    private static boolean realm(ServerPlayer p, java.util.function.Predicate<Realm> f) {
        for (Realm r : KingdomData.get(p.server).realms()) if (f.test(r)) return true;
        return false;
    }

    public static final List<Step> STEPS = List.of(
            new Step("Рождение героя", "Откройте Журнал и создайте героя (класс и черты).", (p, t) -> Skills.data(p).created(), "bandage:6", 0, 30),
            new Step("Горн мастера", "Скуйте Горн мастера — он нужен для лучшего оружия.", (p, t) -> t.getBoolean("forge"), "star_iron:2", 0, 40),
            new Step("Основание города", "Поставьте Ратушу на ровной земле — это начало вашего королевства.", (p, t) -> city(p) != null, "builder_plan:1", 0, 60),
            new Step("Рынок", "Постройте Рынок в городе.", (p, t) -> has(city(p), BuildingType.MARKET), "bread:16", 0, 40),
            new Step("Казармы", "Постройте Казармы, чтобы нанимать воинов.", (p, t) -> has(city(p), BuildingType.BARRACKS), "iron_ingot:12", 0, 40),
            new Step("Дружина", "Наймите 5 воинов в казарме.", (p, t) -> { City c = city(p); return c != null && c.soldiers.size() >= 5; }, "gold_ingot:8", 0, 50),
            new Step("Охота на разбойников", "Убейте 3 разбойников.", (p, t) -> t.getInt("bandits") >= 3, "healing_herb:6", 3, 60),
            new Step("Город растёт", "Доведите город до 2 уровня.", (p, t) -> { City c = city(p); return c != null && c.level >= 2; }, "diamond:2", 0, 60),
            new Step("Стены", "Создайте план стен (Чертёж строителя) и начните строительство.", (p, t) -> { City c = city(p); return c != null && (c.wall != null || c.wallPlan != null); }, "iron_ingot:16", 0, 60),
            new Step("Морграт", "Одолейте Повелителя склепа в подземелье.", (p, t) -> t.getBoolean("crypt"), "star_iron:3", 0, 120),
            new Step("Хозяин региона", "Победите владыку болот, кузни или пустыни.", (p, t) -> t.getBoolean("region"), "star_iron:4", 0, 140),
            new Step("Соседи", "Узнайте о соседнем королевстве (караваны, странники, послы).", (p, t) -> realm(p, r -> r.known >= 2), "emerald:8", 0, 60),
            new Step("Торговый договор", "Заключите торговый договор с королевством.", (p, t) -> realm(p, r -> r.trade), "gold_ingot:12", 0, 80),
            new Step("Союз", "Заключите союз: /regnum realm alliance <id>.", (p, t) -> realm(p, r -> r.ally), "diamond:3", 0, 100),
            new Step("Отбить нападение", "Отразите армию вражеского королевства.", (p, t) -> realm(p, r -> r.armiesLost >= 1), "netherite_scrap:2", 0, 120),
            new Step("Столица", "Доведите город до 5 уровня — вы достойны короны.", (p, t) -> { City c = city(p); return c != null && c.level >= 5; }, "star_blade:1", 0, 300)
    );

    public static int step(Player p) {
        return p.getPersistentData().getCompound(TAG).getInt("step");
    }

    public static boolean finished(Player p) {
        return step(p) >= STEPS.size();
    }

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(TAG)) root.put(TAG, new CompoundTag());
        return root.getCompound(TAG);
    }

    private static void give(ServerPlayer p, String spec) {
        String[] a = spec.split(":");
        var item = BuiltInRegistries.ITEM.get(Regnum.id(a[0]));
        if (item == net.minecraft.world.item.Items.AIR) item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(a[0]));
        if (item == net.minecraft.world.item.Items.AIR) return;
        ItemStack st = new ItemStack(item, Integer.parseInt(a[1]));
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    /** Проверить и при выполнении выдать награду. Возвращает true, если шаг пройден. */
    public static boolean check(ServerPlayer p) {
        CompoundTag d = data(p);
        int s = d.getInt("step");
        if (s >= STEPS.size()) return false;
        Step st = STEPS.get(s);
        if (!st.done().test(p, d)) return false;
        d.putInt("step", s + 1);
        Text.gold(p, "★ Хроника " + (s + 1) + "/" + STEPS.size() + " выполнено: " + st.title());
        if (st.reward() != null) give(p, st.reward());
        Skills.data(p).addCharXp(st.xp());
        Skills.chronicle(p, "Хроника: " + st.title());
        if (s + 1 >= STEPS.size()) finale(p);
        else Text.info(p, "Следующая цель — " + STEPS.get(s + 1).title() + ": " + STEPS.get(s + 1).hint());
        Skills.sync(p, -1);
        return true;
    }

    private static void finale(ServerPlayer p) {
        Text.gold(p, "══ Вы стали Императором. Слава о вас пройдёт сквозь века! ══");
        Skills.addHonor(p, 100, "Хроника завершена");
        p.getServer().getPlayerList().broadcastSystemMessage(Text.of("★ " + p.getName().getString() + " завершил Хронику и принял титул Императора!", ChatFormatting.GOLD), false);
    }

    public static String status(Player p) {
        int s = step(p);
        if (s >= STEPS.size()) return "Хроника завершена: вы Император.";
        Step st = STEPS.get(s);
        String c = st.count() > 0 ? " (" + Math.min(st.count(), counter(p, s)) + "/" + st.count() + ")" : "";
        return "Хроника " + (s + 1) + "/" + STEPS.size() + ": " + st.title() + c + " — " + st.hint();
    }

    private static int counter(Player p, int s) {
        return data(p).getInt("bandits");
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.tickCount % 40 != 7 || finished(p)) return;
        check(p);
        if (p.tickCount % 200 == 7 && !finished(p)) Text.bar(p, status(p), ChatFormatting.GOLD);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        EntityType<?> t = e.getEntity().getType();
        CompoundTag d = data(p);
        if (t == KingdomModule.BANDIT.get()) d.putInt("bandits", d.getInt("bandits") + 1);
        else if (t == DungeonModule.CRYPT_LORD.get()) d.putBoolean("crypt", true);
        else if (t == RegionsModule.MIRE_MOTHER.get() || t == RegionsModule.FORGEMASTER.get() || t == RegionsModule.SCARAB_QUEEN.get()) d.putBoolean("region", true);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && BuiltInRegistries.ITEM.getKey(e.getCrafting().getItem()).getPath().equals("master_forge")) {
            data(p).putBoolean("forge", true);
        }
    }
}
