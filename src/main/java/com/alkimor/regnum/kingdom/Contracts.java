package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Доска заказов: каждый день городу выдаются три контракта (снабжение складов, охота на разбойников, набор войск, постройка,
 * разведка). Они связывают системы мода: склад, армия, дипломатия, наука. Награда — казна, слава и опыт управителя.
 */
public class Contracts extends SavedData {
    private static final String FILE = "regnum_contracts";
    public static final int SLOTS = 3;

    public enum Kind {
        SUPPLY("Снабжение"), HUNT("Охота на разбойников"), MUSTER("Набор войск"), BUILD("Строительство"), SCOUT("Разведка"), PLAGUE("Лекарства");

        public final String title;

        Kind(String t) {
            title = t;
        }
    }

    public static class Contract {
        public Kind kind;
        public int arg;        // SUPPLY: Resource.ordinal; BUILD: BuildingType.ordinal
        public int amount;
        public int progress;
        public int gold, glory;
        public long expires;   // номер игрового дня
        public int advance;    // уже выплаченный аванс (удерживается из награды; при срыве возвращается из казны)

        public String describe() {
            return switch (kind) {
                case SUPPLY -> "Сдать на склад: " + Resource.values()[arg].title + " ×" + amount;
                case HUNT -> "Убить разбойников: " + progress + "/" + amount;
                case MUSTER -> "Довести армию до " + amount + " бойцов";
                case BUILD -> "Построить: " + BuildingType.values()[arg].title + " (нужно " + amount + ")";
                case SCOUT -> "Разведать королевств: " + amount;
                case PLAGUE -> "Запасти трав на складе: " + amount;
            };
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("kind", kind.name());
            t.putInt("arg", arg);
            t.putInt("amount", amount);
            t.putInt("progress", progress);
            t.putInt("gold", gold);
            t.putInt("glory", glory);
            t.putLong("expires", expires);
            t.putInt("advance", advance);
            return t;
        }

        static Contract load(CompoundTag t) {
            Contract c = new Contract();
            try {
                c.kind = Kind.valueOf(t.getString("kind"));
            } catch (IllegalArgumentException e) {
                c.kind = Kind.HUNT;
            }
            c.arg = t.getInt("arg");
            c.amount = t.getInt("amount");
            c.progress = t.getInt("progress");
            c.gold = t.getInt("gold");
            c.glory = t.getInt("glory");
            c.expires = t.getLong("expires");
            c.advance = t.getInt("advance");
            return c;
        }
    }

    private final Map<UUID, List<Contract>> map = new HashMap<>();
    /** Сдано контрактов за игровой день (защита от бесконечного фарма): день и счётчик. */
    private final Map<UUID, long[]> doneToday = new HashMap<>();
    public static final int MAX_PER_DAY = 5;
    /** Репутация поставщика: надёжность растёт с выполненными заказами, срыв её роняет. Влияет на награду в пределах −10…+20%. */
    private final Map<UUID, Integer> rep = new HashMap<>();
    public static final int REP_MIN = -10, REP_MAX = 20;

    public int reputation(UUID owner) {
        return rep.getOrDefault(owner, 0);
    }

    public void addRep(UUID owner, int d) {
        rep.put(owner, Math.max(REP_MIN, Math.min(REP_MAX, reputation(owner) + d)));
        setDirty();
    }

    /** Множитель награды по репутации. */
    public static double rewardFactor(int rep) {
        return 1.0 + Math.max(REP_MIN, Math.min(REP_MAX, rep)) * 0.01;
    }

    /** Сколько заказов предлагают: с плохой репутацией доверяют меньше. */
    public static int slotsFor(int rep) {
        return rep <= -6 ? 2 : SLOTS;
    }

    /** Аванс: 40% награды сразу; при срыве срока сумма списывается из казны и репутация падает. */
    public static boolean takeAdvance(ServerPlayer p, int index) {
        Contracts cs = get(p.server);
        List<Contract> l = cs.of(p.getUUID());
        City c = mainCity(p.server, p.getUUID());
        if (c == null || index < 0 || index >= l.size()) return false;
        Contract k = l.get(index);
        if (k.advance > 0) {
            Text.bad(p, "Аванс по этому заказу уже получен.");
            return false;
        }
        if (cs.reputation(p.getUUID()) < 0) {
            Text.bad(p, "С такой репутацией аванс не дают — выполните несколько заказов в срок.");
            return false;
        }
        k.advance = k.gold * 40 / 100;
        c.treasury += k.advance;
        cs.setDirty();
        KingdomData.get(p.server).setDirty();
        Text.gold(p, "Аванс " + k.advance + " монет получен. Сорвёте срок — вернёте деньги из казны и потеряете репутацию.");
        return true;
    }

    public static Contracts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Contracts::new, Contracts::load), FILE);
    }

    private static Contracts load(CompoundTag tag, HolderLookup.Provider p) {
        Contracts c = new Contracts();
        CompoundTag m = tag.getCompound("map");
        for (String k : m.getAllKeys()) {
            List<Contract> l = new ArrayList<>();
            for (Tag t : m.getList(k, Tag.TAG_COMPOUND)) l.add(Contract.load((CompoundTag) t));
            c.map.put(UUID.fromString(k), l);
        }
        CompoundTag rp = tag.getCompound("rep");
        for (String k : rp.getAllKeys()) c.rep.put(UUID.fromString(k), rp.getInt(k));
        CompoundTag dn = tag.getCompound("done");
        for (String k : dn.getAllKeys()) {
            long[] v = dn.getLongArray(k);
            if (v.length == 2) c.doneToday.put(UUID.fromString(k), v);
        }
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        CompoundTag m = new CompoundTag();
        map.forEach((k, v) -> {
            ListTag l = new ListTag();
            for (Contract c : v) l.add(c.save());
            m.put(k.toString(), l);
        });
        tag.put("map", m);
        CompoundTag rp = new CompoundTag();
        rep.forEach((k, v) -> rp.putInt(k.toString(), v));
        tag.put("rep", rp);
        CompoundTag dn = new CompoundTag();
        doneToday.forEach((k, v) -> dn.putLongArray(k.toString(), v));
        tag.put("done", dn);
        return tag;
    }

    public List<Contract> of(UUID owner) {
        return map.computeIfAbsent(owner, k -> new ArrayList<>());
    }

    // ------------------------------------------------------------------ генерация

    public static Contract generate(Random r, City c, long day, MinecraftServer server) {
        Contract k = new Contract();
        int lvl = Math.max(1, c.level);
        int roll = r.nextInt(100);
        if (roll < 35) {
            k.kind = Kind.SUPPLY;
            Resource[] pool = {Resource.FOOD, Resource.WOOD, Resource.STONE, Resource.IRON, Resource.HERBS};
            Resource res = pool[r.nextInt(pool.length)];
            k.arg = res.ordinal();
            k.amount = (res == Resource.IRON || res == Resource.HERBS ? 15 : 40) * (1 + lvl / 2) + r.nextInt(20);
            k.gold = 12 + k.amount / (res == Resource.IRON ? 1 : 3);
        } else if (roll < 55) {
            k.kind = Kind.HUNT;
            k.amount = 4 + lvl * 2 + r.nextInt(4);
            k.gold = 10 + k.amount * 3;
        } else if (roll < 70) {
            k.kind = Kind.MUSTER;
            k.amount = Math.max(c.soldiers.size() + 3, 6 + lvl * 4);
            k.gold = 20 + k.amount * 2;
        } else if (roll < 85) {
            BuildingType[] bt = BuildingType.values();
            BuildingType b = bt[r.nextInt(bt.length)];
            if (b.tech != null && !Science.has(server, c.owner, b.tech)) b = BuildingType.MARKET;
            k.kind = Kind.BUILD;
            k.arg = b.ordinal();
            k.amount = c.count(b) + 1;
            k.gold = 30 + 10 * lvl;
        } else if (roll < 95) {
            k.kind = Kind.SCOUT;
            int known = 0;
            for (Realm re : KingdomData.get(server).realms()) if (re.known >= 2) known++;
            k.amount = known + 1;
            k.gold = 35 + 5 * lvl;
        } else {
            k.kind = Kind.PLAGUE;
            k.amount = 20 + 10 * lvl;
            k.gold = 25 + 5 * lvl;
        }
        k.glory = 3 + lvl;
        k.expires = day + 3 + r.nextInt(3);
        return k;
    }

    private static City mainCity(MinecraftServer server, UUID owner) {
        City best = null;
        for (City c : KingdomData.get(server).ownedBy(owner)) if (best == null || c.level > best.level) best = c;
        return best;
    }

    public static void refresh(MinecraftServer server, UUID owner, long day) {
        City c = mainCity(server, owner);
        if (c == null) return;
        Contracts cs = get(server);
        List<Contract> l = cs.of(owner);
        for (java.util.Iterator<Contract> it = l.iterator(); it.hasNext(); ) {
            Contract k = it.next();
            if (k.expires >= day) continue;
            it.remove();
            if (k.advance > 0) c.treasury -= Math.min(c.treasury, k.advance); // аванс возвращается всегда, сдан заказ или нет
            if (ready(server, owner, c, k)) continue; // условие было выполнено, но не сдано — без штрафа репутации
            cs.addRep(owner, -2);
            ServerPlayer op = server.getPlayerList().getPlayer(owner);
            if (op != null) Text.bad(op, "Заказ «" + k.kind.title + "» сорван" + (k.advance > 0 ? ": аванс " + k.advance + " возвращён из казны" : "") + ". Репутация поставщика −2.");
        }
        int slots = slotsFor(cs.reputation(owner));
        long[] dn = cs.doneToday.get(owner);
        if (dn != null && dn[0] == day && dn[1] >= MAX_PER_DAY) {
            cs.setDirty();
            return;
        }
        Random r = new Random(owner.hashCode() * 31L + day);
        int guard = 0;
        while (l.size() < slots && guard++ < 12) {
            Contract k = generate(r, c, day, server);
            boolean dup = false;
            for (Contract o : l) if (o.kind == k.kind && o.arg == k.arg) dup = true;
            if (!dup) l.add(k);
        }
        cs.setDirty();
    }

    // ------------------------------------------------------------------ проверка и сдача

    /** Выполнено ли условие (для сдачи на складе — наличие ресурса). */
    public static boolean ready(MinecraftServer server, UUID owner, City c, Contract k) {
        return switch (k.kind) {
            case SUPPLY -> c.stock(Resource.values()[k.arg]) >= k.amount;
            case HUNT -> k.progress >= k.amount;
            case MUSTER -> c.soldiers.size() >= k.amount;
            case BUILD -> c.count(BuildingType.values()[k.arg]) >= k.amount;
            case SCOUT -> {
                int known = 0;
                for (Realm re : KingdomData.get(server).realms()) if (re.known >= 2) known++;
                yield known >= k.amount;
            }
            case PLAGUE -> c.stock(Resource.HERBS) >= k.amount;
        };
    }

    /** Сдать выполненный контракт. Возвращает true, если награда выдана. */
    public static boolean complete(ServerPlayer p, int index) {
        Contracts cs = get(p.server);
        List<Contract> l = cs.of(p.getUUID());
        City c = mainCity(p.server, p.getUUID());
        if (c == null || index < 0 || index >= l.size()) return false;
        Contract k = l.get(index);
        if (!ready(p.server, p.getUUID(), c, k)) {
            Text.bad(p, "Контракт ещё не выполнен: " + k.describe());
            return false;
        }
        if (k.kind == Kind.SUPPLY) c.take(Resource.values()[k.arg], k.amount);
        if (k.kind == Kind.PLAGUE) c.take(Resource.HERBS, k.amount);
        int reward = Math.max(0, (int) Math.round(k.gold * rewardFactor(cs.reputation(p.getUUID()))) - k.advance);
        c.treasury += reward;
        c.glory += k.glory;
        cs.addRep(p.getUUID(), 1);
        Skills.addXp(p, Skill.STEWARD, 10 + k.gold / 3);
        l.remove(index);
        long day = p.serverLevel().getDayTime() / 24000L;
        long[] dn = cs.doneToday.computeIfAbsent(p.getUUID(), x -> new long[]{day, 0});
        if (dn[0] != day) {
            dn[0] = day;
            dn[1] = 0;
        }
        dn[1]++;
        cs.setDirty();
        KingdomData.get(p.server).setDirty();
        Text.gold(p, "Контракт «" + k.kind.title + "» выполнен: +" + reward + " в казну" + (k.advance > 0 ? " (аванс " + k.advance + " уже получен)" : "") + ", +" + k.glory + " славы. Репутация поставщика: " + cs.reputation(p.getUUID()) + ".");
        return true;
    }

    private static int show(ServerPlayer p) {
        City c = mainCity(p.server, p.getUUID());
        if (c == null) {
            Text.info(p, "Доска заказов доступна владельцам города.");
            return 0;
        }
        long day = p.serverLevel().getDayTime() / 24000L;
        refresh(p.server, p.getUUID(), day);
        List<Contract> l = get(p.server).of(p.getUUID());
        int rp = get(p.server).reputation(p.getUUID());
        p.sendSystemMessage(Text.of("— Доска заказов «" + c.name + "» — репутация поставщика " + rp + " (награда ×" + String.format("%.2f", rewardFactor(rp)) + ") —", ChatFormatting.GOLD));
        for (int i = 0; i < l.size(); i++) {
            Contract k = l.get(i);
            boolean ok = ready(p.server, p.getUUID(), c, k);
            var line = net.minecraft.network.chat.Component.empty();
            line.append(Text.of("  " + (i + 1) + ". " + k.describe() + " — награда " + k.gold + " монет, " + k.glory + " славы (дней: " + Math.max(0, k.expires - day) + ") ",
                    ok ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (ok) line.append(Text.button("[сдать]", "/regnum contracts done " + (i + 1), "Получить награду"));
            else if (k.advance == 0 && rp >= 0) line.append(Text.button("[аванс]", "/regnum contracts advance " + (i + 1), "40% награды сейчас; за срыв — возврат и штраф репутации"));
            p.sendSystemMessage(line);
        }
        return 1;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 43 || !DayClock.newDay(e.getServer(), "contracts")) return;
        long day = e.getServer().overworld().getDayTime() / 24000L;
        for (UUID owner : KingdomData.get(e.getServer()).all().stream().map(c -> c.owner).distinct().toList()) {
            refresh(e.getServer(), owner, day);
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(owner);
            if (p != null) Text.info(p, "На доске заказов новые контракты: /regnum contracts");
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof BanditEntity)) return;
        Entity src = e.getSource().getEntity();
        UUID owner = null;
        if (src instanceof ServerPlayer p) owner = p.getUUID();
        else if (src instanceof SoldierEntity s) owner = s.getOwnerId();
        if (owner == null || e.getEntity().level().isClientSide) return;
        MinecraftServer server = e.getEntity().getServer();
        Contracts cs = get(server);
        for (Contract k : cs.of(owner)) {
            if (k.kind == Kind.HUNT && k.progress < k.amount) {
                k.progress++;
                cs.setDirty();
                if (k.progress == k.amount) {
                    ServerPlayer p = server.getPlayerList().getPlayer(owner);
                    if (p != null) Text.good(p, "Охота завершена! Сдайте контракт: /regnum contracts");
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("contracts")
                .executes(ctx -> show(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("advance").then(Commands.argument("n", IntegerArgumentType.integer(1, SLOTS)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = takeAdvance(p, IntegerArgumentType.getInteger(ctx, "n") - 1);
                    if (ok) show(p);
                    return ok ? 1 : 0;
                })))
                .then(Commands.literal("done").then(Commands.argument("n", IntegerArgumentType.integer(1, SLOTS)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    boolean ok = complete(p, IntegerArgumentType.getInteger(ctx, "n") - 1);
                    if (ok) show(p);
                    return ok ? 1 : 0;
                })))));
    }
}
