package com.alkimor.regnum.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Город игрока. Хранится в {@link KingdomData}. */
public class City {
    public static final int MAX_LEVEL = 5;

    public final UUID id;
    public UUID owner;
    public String name;
    public BlockPos hall;
    public int level = 1;
    /** Деревни уже прислали ополчение на эту войну (не сохраняется). */
    public transient boolean levied = false;
    public int treasury = 0;
    public int glory = 0;
    /** Пленные разбойники и враги, взятые у города. */
    public int prisoners = 0;
    /** Сколько игровых суток подряд правителя не было в сети (при 5+ без регента или наместника доход падает). */
    public int absentDays = 0;
    /** Заказ производства (0 — нет; см. Labor). */
    public int labor = 0;
    /** Дней засухи осталось (производство провианта вдвое ниже). */
    public int droughtDays = 0;
    /** Вклад игроков в город (очки: изумруды в казну, ресурсы на склад). Даёт титул и порядок в совете. */
    public final java.util.Map<UUID, Integer> contrib = new java.util.HashMap<>();

    public void addContrib(UUID who, int pts) {
        if (pts > 0) contrib.merge(who, pts, Integer::sum);
    }

    public static String rankTitle(int pts) {
        return pts >= 2000 ? "Хранитель города" : pts >= 800 ? "Опора города" : pts >= 250 ? "Житель с заслугами" : pts >= 50 ? "Помощник" : "Новичок";
    }


    public boolean hasRegent() {
        for (int v : roles.values()) if ((v & Council.Role.TREASURY.bit) != 0) return true;
        return false;
    }

    public int population = 0;
    public int recruitSquad = 1;
    public long lastEconomyDay = -1;
    public long nextRaidDay = -1;
    /** Доверенные игроки: могут строить и ломать на земле города. */
    public final Set<UUID> trusted = new HashSet<>();
    /** Роли совета: игрок -> битовая маска Council.Role. */
    public final java.util.Map<UUID, Integer> roles = new java.util.HashMap<>();
    /** Идёт война: защита земли снята, у построек есть прочность. */
    public boolean war = false;
    /** Союзники уже прислали подмогу в этой войне (не сохраняется). */
    public boolean aided = false;
    /** Текущая стройка стены рабочими. */
    public WallJob wall = null;
    /** Чертёж достроенной стены — по нему строитель чинит повреждения после осады. */
    public WallJob wallPlan = null;

    // --- Казначейство (облигации)
    public static final String[] STRATEGIES = {"Надёжная", "Сбалансированная", "Рискованная"};
    public int bonds = 0;
    /** Наместник-наследник (UUID сущности) и его имя. */
    public String governor = "";
    public String governorName = "";
    public int bondStrategy = 0;
    /** Культура (облик войск). */
    public int culture = 0;
    /** Кэш перков правителя (обновляется, когда он в сети) — экономика работает и без него. */
    public int ownerPerks = 0;
    public int ownerSteward = 0;
    public static final int P_TAX = 1, P_QUARTER = 2, P_BUILD = 4, P_VAULT = 8, P_REGENT = 16, P_GROWTH = 32, P_GOLDEN = 64,
            P_WALLS = 128, P_PEOPLE = 256, P_PAY = 512, P_RECRUIT = 1024;

    public boolean perk(int flag) {
        return (ownerPerks & flag) != 0;
    }

    public void refreshOwner(net.minecraft.world.entity.player.Player p) {
        if (p == null || !p.getUUID().equals(owner)) return;
        int f = 0;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_TAX)) f |= P_TAX;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_QUARTER)) f |= P_QUARTER;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_BUILD)) f |= P_BUILD;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_VAULT)) f |= P_VAULT;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_REGENT)) f |= P_REGENT;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_GROWTH)) f |= P_GROWTH;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_GOLDEN)) f |= P_GOLDEN;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.ST_WALLS)) f |= P_WALLS;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.CH_PEOPLE)) f |= P_PEOPLE;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.LD_PAY)) f |= P_PAY;
        if (com.alkimor.regnum.survival.Skills.has(p, com.alkimor.regnum.survival.Perk.LD_RECRUIT)) f |= P_RECRUIT;
        ownerPerks = f;
        ownerSteward = com.alkimor.regnum.survival.Skills.level(p, com.alkimor.regnum.survival.Skill.STEWARD);
    }

    public int recruitCost(SoldierType t) {
        float k = (perk(P_RECRUIT) ? 0.8f : 1f) * (spec == 4 && t.mounted() ? 0.85f : spec == 2 ? 1.1f : 1f);
        return Math.max(1, Math.round(t.cost * k));
    }
    public long lastBondDay = -1;
    /** Специализация города (см. Specialization): 0 нет, 1 кузнечный, 2 торговый, 3 учёный, 4 конный. */
    public int spec = 0;
    public long specDay = -100;

    // --- Запасы, здоровье и ремесло (Industry / Health)
    public final java.util.EnumMap<Resource, Integer> stock = new java.util.EnumMap<>(Resource.class);
    /** Дней до конца эпидемии (0 — нет), её сила 1..3, карантин. */
    public int plagueDays = 0;
    public int plagueLevel = 0;
    public boolean quarantine = false;
    /** Дней подряд без провианта. */
    public int hungerDays = 0;
    /** Бонус брони от кузницы (0..3): действует, если сегодня в кузнице было железо. */
    public int forged = 0;
    /** Сколько легендарных клинков выковано в городе. */
    public int legends = 0;
    /** Дни без эпидемии (для рандома). */
    public int healthyDays = 0;
    /** Источник вспышки: 0 скученность, 1 голод, 2 переполненные казармы. */
    public int plagueSource = 0;
    /** Дни передышки после эпидемии: новая не начинается, голод не разгоняет армию. */
    public int postPlagueDays = 0;
    /** Механизация (Create/Aeronautics рядом с ратушей, кузницей и складом), 0..3. */
    public int mech = 0;

    public int stock(Resource r) {
        return stock.getOrDefault(r, 0);
    }

    /** Вместимость склада по каждому ресурсу. */
    public int capacity() {
        return 200 + 100 * level + 400 * count(BuildingType.WAREHOUSE) + 150 * mech;
    }

    /** Положить на склад; возвращает сколько реально поместилось. */
    public int deposit(Resource r, int n) {
        int room = Math.max(0, capacity() - stock(r));
        int add = Math.min(room, n);
        if (add > 0) stock.put(r, stock(r) + add);
        return add;
    }

    public boolean take(Resource r, int n) {
        if (stock(r) < n) return false;
        stock.put(r, stock(r) - n);
        return true;
    }

    /** Постройки: позиция блока → тип. */
    public final Map<Long, BuildingType> buildings = new HashMap<>();
    /** Разрушенные при разграблении постройки (не работают, пока не починены). */
    public final Map<Long, BuildingType> ruined = new HashMap<>();

    /** Выводит из строя до n построек. Возвращает названия. */
    public java.util.List<String> ruinSome(net.minecraft.util.RandomSource r, int n) {
        java.util.List<Long> keys = new java.util.ArrayList<>(buildings.keySet());
        java.util.List<String> names = new java.util.ArrayList<>();
        while (n-- > 0 && !keys.isEmpty()) {
            Long k = keys.remove(r.nextInt(keys.size()));
            BuildingType t = buildings.remove(k);
            if (t != null) { ruined.put(k, t); names.add(t.title); }
        }
        return names;
    }

    /** Солдаты города: UUID → тип (ordinal). */
    public final Map<UUID, Integer> soldiers = new HashMap<>();

    // --- Набег
    public boolean raidActive = false;
    public int raidWave = 0;
    public int raidWavesTotal = 0;
    public int raidDelay = 0;
    public int raidTimer = 0;
    public int raidStolen = 0;
    public int raidWaveSize = 0;
    public final Set<UUID> raiders = new HashSet<>();

    public City(UUID id, UUID owner, String name, BlockPos hall) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.hall = hall;
    }

    public int radius() {
        return 24 + 8 * level;
    }

    public int count(BuildingType t) {
        int n = 0;
        for (BuildingType b : buildings.values()) if (b == t) n++;
        return n;
    }

    public int armyCap(int leadershipLevel) {
        return 4 + count(BuildingType.BARRACKS) * 5 + level * 2 + leadershipLevel;
    }

    /** Занятые места в армии: конница занимает два (боец и конь). */
    public int armyUsed() {
        int n = 0;
        for (int v : soldiers.values()) n += SoldierType.byId(v).slots();
        return n;
    }

    public int countSoldiers(SoldierType t) {
        int n = 0;
        for (int v : soldiers.values()) if (v == t.ordinal()) n++;
        return n;
    }

    public int dailyUpkeep() {
        int u = 0;
        for (int v : soldiers.values()) u += SoldierType.byId(v).upkeep;
        double m = (governor.isEmpty() ? 1.0 : 0.75) * (perk(P_QUARTER) ? 0.9 : 1.0) * (perk(P_PAY) ? 0.85 : 1.0);
        return (int) Math.round(u * m);
    }

    public int dailyIncome(int taxPerVillager) {
        double tax = population * taxPerVillager * (perk(P_TAX) ? 1.1 : 1.0) * (perk(P_PEOPLE) ? 1.1 : 1.0);
        double base = tax + count(BuildingType.MARKET) * 3 + level;
        double m = (1 + 0.002 * ownerSteward) * (perk(P_REGENT) ? 1.1 : 1.0) * (perk(P_GOLDEN) ? 1.15 : 1.0) * (spec == 2 ? 1.15 : spec == 3 ? 0.95 : 1.0)
                * (labor > 0 ? 0.85 : 1.0) * (absentDays >= 5 && governor.isEmpty() && !hasRegent() ? 0.8 : 1.0);
        int total = (int) Math.round(base * m);
        return governor.isEmpty() ? total : total + Math.max(1, total / 10);
    }

    public int upgradeCost() {
        return perk(P_BUILD) ? Math.round(30 * level * 0.85f) : 30 * level;
    }

    public int upgradeGlory() {
        return 20 * (level - 1);
    }

    public boolean contains(BlockPos p) {
        double dx = p.getX() - hall.getX(), dz = p.getZ() - hall.getZ();
        return dx * dx + dz * dz <= (double) radius() * radius();
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putUUID("owner", owner);
        t.putString("name", name);
        t.putLong("hall", hall.asLong());
        t.putInt("level", level);
        t.putInt("treasury", treasury);
        t.putInt("glory", glory);
        t.putInt("prisoners", prisoners);
        t.putInt("absentDays", absentDays);
        t.putInt("labor", labor);
        t.putInt("droughtDays", droughtDays);
        t.putInt("population", population);
        t.putInt("recruitSquad", recruitSquad);
        t.putLong("lastEconomyDay", lastEconomyDay);
        t.putLong("nextRaidDay", nextRaidDay);
        t.putInt("bonds", bonds);
        t.putString("governor", governor);
        t.putString("governorName", governorName);
        t.putInt("bondStrategy", bondStrategy);
        t.putLong("lastBondDay", lastBondDay);

        ListTag b = new ListTag();
        buildings.forEach((pos, type) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", pos);
            e.putString("type", type.name());
            b.add(e);
        });
        t.put("buildings", b);
        ListTag ruinList = new ListTag();
        ruined.forEach((pos, type) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("pos", pos);
            e.putString("type", type.name());
            ruinList.add(e);
        });
        t.put("ruined", ruinList);

        ListTag s = new ListTag();
        soldiers.forEach((uuid, type) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", uuid);
            e.putInt("type", type);
            s.add(e);
        });
        t.put("soldiers", s);

        t.putBoolean("war", war);
        CompoundTag st = new CompoundTag();
        stock.forEach((k, v) -> st.putInt(k.name(), v));
        t.put("stock", st);
        t.putInt("plagueDays", plagueDays);
        t.putInt("plagueLevel", plagueLevel);
        t.putBoolean("quarantine", quarantine);
        t.putInt("hungerDays", hungerDays);
        t.putInt("forged", forged);
        t.putInt("legends", legends);
        t.putInt("healthyDays", healthyDays);
        t.putInt("plagueSource", plagueSource);
        t.putInt("spec", spec);
        t.putLong("specDay", specDay);
        t.putInt("postPlagueDays", postPlagueDays);
        t.putInt("mech", mech);
        if (wall != null) t.put("wall", wall.save());
        if (wallPlan != null) t.put("wallPlan", wallPlan.save());
        ListTag tr = new ListTag();
        for (UUID u : trusted) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", u);
            tr.add(e);
        }
        t.put("trusted", tr);
        ListTag rl = new ListTag();
        roles.forEach((u, m) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", u);
            e.putInt("m", m);
            rl.add(e);
        });
        t.put("roles", rl);
        ListTag cl = new ListTag();
        contrib.forEach((u, v) -> { CompoundTag e = new CompoundTag(); e.putUUID("id", u); e.putInt("v", v); cl.add(e); });
        t.put("contrib", cl);
        t.putInt("culture", culture);
        t.putInt("ownerPerks", ownerPerks);
        t.putInt("ownerSteward", ownerSteward);
        t.putBoolean("raidActive", raidActive);
        t.putInt("raidWave", raidWave);
        t.putInt("raidWavesTotal", raidWavesTotal);
        t.putInt("raidStolen", raidStolen);
        ListTag r = new ListTag();
        for (UUID u : raiders) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", u);
            r.add(e);
        }
        t.put("raiders", r);
        return t;
    }

    public static City load(CompoundTag t) {
        City c = new City(t.getUUID("id"), t.getUUID("owner"), t.getString("name"), BlockPos.of(t.getLong("hall")));
        c.level = Math.max(1, t.getInt("level"));
        c.treasury = t.getInt("treasury");
        c.glory = t.getInt("glory");
        c.prisoners = t.getInt("prisoners");
        c.absentDays = t.getInt("absentDays");
        c.labor = t.getInt("labor");
        c.droughtDays = t.getInt("droughtDays");
        c.population = t.getInt("population");
        c.recruitSquad = Math.max(1, t.getInt("recruitSquad"));
        c.lastEconomyDay = t.getLong("lastEconomyDay");
        c.nextRaidDay = t.getLong("nextRaidDay");
        c.bonds = t.getInt("bonds");
        c.governor = t.getString("governor");
        c.governorName = t.getString("governorName");
        c.bondStrategy = t.getInt("bondStrategy");
        c.culture = t.getInt("culture");
        c.war = t.getBoolean("war");
        CompoundTag st = t.getCompound("stock");
        for (Resource r : Resource.values()) if (st.contains(r.name())) c.stock.put(r, st.getInt(r.name()));
        c.plagueDays = t.getInt("plagueDays");
        c.plagueLevel = t.getInt("plagueLevel");
        c.quarantine = t.getBoolean("quarantine");
        c.hungerDays = t.getInt("hungerDays");
        c.forged = t.getInt("forged");
        c.legends = t.getInt("legends");
        c.healthyDays = t.getInt("healthyDays");
        c.plagueSource = t.getInt("plagueSource");
        c.spec = t.getInt("spec");
        c.specDay = t.contains("specDay") ? t.getLong("specDay") : -100;
        c.postPlagueDays = t.getInt("postPlagueDays");
        c.mech = t.getInt("mech");
        if (t.contains("wall")) c.wall = WallJob.load(t.getCompound("wall"));
        if (t.contains("wallPlan")) c.wallPlan = WallJob.load(t.getCompound("wallPlan"));
        for (Tag e : t.getList("trusted", Tag.TAG_COMPOUND)) c.trusted.add(((CompoundTag) e).getUUID("id"));
        for (Tag e : t.getList("contrib", Tag.TAG_COMPOUND)) c.contrib.put(((CompoundTag) e).getUUID("id"), ((CompoundTag) e).getInt("v"));
        for (Tag e : t.getList("roles", Tag.TAG_COMPOUND)) c.roles.put(((CompoundTag) e).getUUID("id"), ((CompoundTag) e).getInt("m"));
        c.ownerPerks = t.getInt("ownerPerks");
        c.ownerSteward = t.getInt("ownerSteward");
        c.lastBondDay = t.contains("lastBondDay") ? t.getLong("lastBondDay") : -1;
        for (Tag e : t.getList("buildings", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) e;
            try {
                c.buildings.put(ct.getLong("pos"), BuildingType.valueOf(ct.getString("type")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (Tag e : t.getList("ruined", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) e;
            try {
                c.ruined.put(ct.getLong("pos"), BuildingType.valueOf(ct.getString("type")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (Tag e : t.getList("soldiers", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) e;
            c.soldiers.put(ct.getUUID("id"), ct.getInt("type"));
        }
        c.raidActive = t.getBoolean("raidActive");
        c.raidWave = t.getInt("raidWave");
        c.raidWavesTotal = t.getInt("raidWavesTotal");
        c.raidStolen = t.getInt("raidStolen");
        for (Tag e : t.getList("raiders", Tag.TAG_COMPOUND)) {
            c.raiders.add(((CompoundTag) e).getUUID("id"));
        }
        return c;
    }
}
