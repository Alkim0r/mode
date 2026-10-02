package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.core.network.CityActionPayload;
import com.alkimor.regnum.core.network.CityInfoPayload;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Серверная логика экрана ратуши. */
public final class CityActions {
    private CityActions() {}

    public static String rulerTitle(KingdomData data, UUID owner) {
        List<City> cs = data.ownedBy(owner);
        int total = 0;
        for (City c : cs) total += c.level;
        if (cs.size() >= 5 && total >= 15) return "Император";
        if (cs.size() >= 3 && total >= 8) return "Король";
        if (cs.size() >= 2 || total >= 3) return "Князь";
        return "Вождь";
    }

    public static void openScreen(ServerPlayer sp, BlockPos hall) {
        KingdomData data = KingdomData.get(sp.server);
        City c = data.byHall(hall);
        if (c == null) {
            Text.info(sp, "Эта ратуша не основала город (возможно, её поставили не вы).");
            return;
        }
        if (!Council.isMember(c, sp.getUUID()) || c.trusted.contains(sp.getUUID()) && !c.owner.equals(sp.getUUID()) && c.roles.getOrDefault(sp.getUUID(), 0) == 0) {
            Text.info(sp, "Город «" + c.name + "», уровень " + c.level + ". Правитель — не вы.");
            return;
        }
        long day = sp.serverLevel().getDayTime() / 24000L;
        c.refreshOwner(sp);
        ServerPlayer ruler = sp.server.getPlayerList().getPlayer(c.owner);
        int lead = ruler != null ? Skills.armyBonus(ruler) : 0;
        PacketDistributor.sendToPlayer(sp, new CityInfoPayload(c.hall, c.name, rulerTitle(data, c.owner), c.level, c.radius(), c.population,
                c.treasury, c.glory, c.armyUsed(), c.armyCap(lead),
                c.count(BuildingType.BARRACKS), c.count(BuildingType.MARKET), c.count(BuildingType.WATCHTOWER),
                (int) Math.max(0, c.nextRaidDay - day), c.raidActive, c.recruitSquad,
                c.upgradeCost(), c.upgradeGlory(), c.dailyIncome(com.alkimor.regnum.core.RegnumConfig.TAX_PER_VILLAGER.get()), c.dailyUpkeep(),
                c.countSoldiers(SoldierType.SWORDSMAN) + c.countSoldiers(SoldierType.MILITIA), c.countSoldiers(SoldierType.ARCHER), c.countSoldiers(SoldierType.KNIGHT),
                c.bonds, c.bondStrategy, c.bonds > 0 && c.lastBondDay >= 0 ? (int) Math.max(0, 7 - (day - c.lastBondDay)) : 7, c.culture, byType(c), extra(c)));
    }

    private static int[] extra(City c) {
        int[] r = new int[com.alkimor.regnum.core.network.CityInfoPayload.EXTRA_LEN];
        for (Resource res : Resource.values()) r[res.ordinal()] = c.stock(res);
        r[6] = c.capacity();
        r[7] = c.plagueDays;
        r[8] = c.plagueLevel;
        r[9] = c.quarantine ? 1 : 0;
        r[10] = c.hungerDays;
        r[11] = c.forged;
        r[12] = 40 * c.level;
        r[13] = 40 * c.level;
        return r;
    }

    private static int[] byType(City c) {
        int[] r = new int[SoldierType.values().length];
        for (int v : c.soldiers.values()) if (v >= 0 && v < r.length) r[v]++;
        return r;
    }

    public static void handle(ServerPlayer sp, CityActionPayload p) {
        KingdomData data = KingdomData.get(sp.server);
        City c = data.byHall(p.hall());
        if (c == null) return;
        Council.Role need = switch (p.action()) {
            case CityActionPayload.DEPOSIT, CityActionPayload.WITHDRAW, CityActionPayload.INVEST, CityActionPayload.DIVEST, CityActionPayload.UPGRADE -> Council.Role.TREASURY;
            case CityActionPayload.RECRUIT, CityActionPayload.SET_SQUAD -> Council.Role.COMMAND;
            default -> null;
        };
        if (need == null ? !c.owner.equals(sp.getUUID()) : !Council.can(c, sp.getUUID(), need)) {
            if (c.owner.equals(sp.getUUID()) || Council.isMember(c, sp.getUUID())) Text.bad(sp, "У вас нет на это права в совете города.");
            return;
        }
        if (!sp.blockPosition().closerThan(c.hall, c.radius() + 16)) {
            Text.bad(sp, "Вы слишком далеко от города.");
            return;
        }
        switch (p.action()) {
            case CityActionPayload.DEPOSIT -> {
                int n = InvUtil.count(sp, Items.EMERALD);
                if (n == 0) {
                    Text.bad(sp, "В инвентаре нет изумрудов.");
                } else {
                    InvUtil.take(sp, Items.EMERALD, n);
                    c.treasury += n;
                    c.addContrib(sp.getUUID(), n);
                    Text.good(sp, "В казну внесено " + n + " изумр.");
                }
            }
            case CityActionPayload.WITHDRAW -> {
                int n = Math.min(16, c.treasury);
                if (n > 0) {
                    c.treasury -= n;
                    InvUtil.give(sp, new ItemStack(Items.EMERALD, n));
                    Text.good(sp, "Из казны взято " + n + " изумр.");
                }
            }
            case CityActionPayload.RECRUIT -> recruit(sp, c, SoldierType.byId(p.arg()));
            case CityActionPayload.UPGRADE -> upgrade(sp, c);
            case CityActionPayload.RENAME -> {
                String name = p.text().trim();
                if (name.length() >= 2 && name.length() <= 32) {
                    c.name = name;
                    Text.good(sp, "Город переименован в «" + name + "».");
                }
            }
            case CityActionPayload.SET_SQUAD -> c.recruitSquad = Math.max(1, Math.min(4, p.arg()));
            case CityActionPayload.INVEST -> {
                int n = Math.min(16, c.treasury);
                if (n <= 0) {
                    Text.bad(sp, "Казна пуста.");
                } else {
                    if (c.bonds == 0) c.lastBondDay = sp.serverLevel().getDayTime() / 24000L;
                    c.treasury -= n;
                    c.bonds += n;
                    Text.good(sp, "Казначей вложил " + n + " изумр. в облигации (" + City.STRATEGIES[c.bondStrategy] + " стратегия).");
                    Skills.addXp(sp, Skill.TRADE, 2);
                }
            }
            case CityActionPayload.DIVEST -> {
                if (c.bonds > 0) {
                    c.treasury += c.bonds;
                    Text.good(sp, "Облигации проданы: +" + c.bonds + " изумр. в казну.");
                    c.bonds = 0;
                    c.lastBondDay = -1;
                }
            }
            case CityActionPayload.STRATEGY -> c.bondStrategy = (c.bondStrategy + 1) % City.STRATEGIES.length;
            case CityActionPayload.CULTURE -> {
                c.culture = Culture.byId(c.culture + 1).ordinal();
                Culture cul = Culture.byId(c.culture);
                // перекрасить и переодеть войска города, что сейчас в мире
                for (SoldierEntity s : sp.serverLevel().getEntities(KingdomModule.SOLDIER.get(), e -> c.id.equals(e.getCityId()))) {
                    s.setCulture(cul);
                }
                Text.good(sp, "Культура города: " + cul.title + " — " + cul.look + ".");
            }
            default -> {}
        }
        data.setDirty();
        openScreen(sp, c.hall);
    }

    private static void recruit(ServerPlayer sp, City c, SoldierType type) {
        if (type == SoldierType.MILITIA) {
            Text.info(sp, "Ополченцев вербуют среди жителей: возьмите жезл командира и нажмите ПКМ по жителю (4 изумруда).");
            return;
        }
        if (c.count(BuildingType.BARRACKS) == 0) {
            Text.bad(sp, "Для найма нужна казарма.");
            return;
        }
        if (type.tech() != null && !Science.has(sp.server, c.owner, type.tech())) {
            Text.bad(sp, type.title + ": нужна технология «" + type.tech().title + "» (/regnum science).");
            return;
        }
        if (type.mounted() && c.count(BuildingType.STABLE) == 0) {
            Text.bad(sp, "Коннице нужна конюшня в городе.");
            return;
        }
        if (c.level < type.minCityLevel) {
            Text.bad(sp, type.title + " доступен с " + type.minCityLevel + " уровня города.");
            return;
        }
        int cap = c.armyCap(Skills.armyBonus(sp.server.getPlayerList().getPlayer(c.owner) != null ? sp.server.getPlayerList().getPlayer(c.owner) : sp));
        if (c.armyUsed() + type.slots() > cap) {
            Text.bad(sp, "Армия города достигла предела (" + cap + "). Стройте казармы и развивайте Командование.");
            return;
        }
        if (type.gunpowderCost() > 0 && (c.stock(Resource.GUNPOWDER) < type.gunpowderCost() || c.stock(Resource.IRON) < type.ironCost())) {
            Text.bad(sp, type.title + ": на складе нужно пороха " + type.gunpowderCost() + (type.ironCost() > 0 ? " и железа " + type.ironCost() : "")
                    + " (сдайте на Склад, /regnum stock).");
            return;
        }
        c.refreshOwner(sp);
        int cost = c.recruitCost(type);
        if (c.treasury < cost) {
            Text.bad(sp, "В казне недостаточно изумрудов: нужно " + cost + ".");
            return;
        }
        ServerLevel level = sp.serverLevel();
        BlockPos at = c.hall;
        for (Map.Entry<Long, BuildingType> e : c.buildings.entrySet()) {
            if (e.getValue() == BuildingType.BARRACKS) {
                at = BlockPos.of(e.getKey());
                break;
            }
        }
        SoldierEntity s = KingdomModule.SOLDIER.get().create(level);
        if (s == null) return;
        BlockPos spawn = findStandPos(level, at);
        s.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, sp.getYRot() + 180f, 0f);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.MOB_SUMMONED, null);
        s.setup(type, c.owner, c.id, c.recruitSquad);
        s.command(Order.HOLD, Vec3.atBottomCenterOf(spawn), sp.getYRot(), Formation.LINE, 0, 1, null);
        level.addFreshEntity(s);
        if (type.gunpowderCost() > 0) {
            c.take(Resource.GUNPOWDER, type.gunpowderCost());
            c.take(Resource.IRON, type.ironCost());
        }
        c.treasury -= cost;
        c.soldiers.put(s.getUUID(), type.ordinal());
        level.playSound(null, spawn, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 1f, 1f);
        Text.good(sp, "Нанят: " + type.title + " (отряд " + c.recruitSquad + "). Жалованье: " + type.upkeep + " изумр./день.");
        Skills.addXp(sp, Skill.LEADERSHIP, 6);
    }

    private static BlockPos findStandPos(ServerLevel level, BlockPos near) {
        for (int i = 0; i < 16; i++) {
            BlockPos p = near.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos q = p.above(dy);
                if (level.getBlockState(q.below()).isSolid() && level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()) return q;
            }
        }
        return near.above();
    }

    private static void upgrade(ServerPlayer sp, City c) {
        if (c.level >= City.MAX_LEVEL) {
            Text.info(sp, "Город уже достиг наивысшего уровня.");
            return;
        }
        if (c.treasury < c.upgradeCost() || c.glory < c.upgradeGlory()) {
            Text.bad(sp, "Для развития нужно " + c.upgradeCost() + " изумр. в казне и " + c.upgradeGlory() + " славы.");
            return;
        }
        int wood = 40 * c.level, stone = 40 * c.level;
        if (c.stock(Resource.WOOD) < wood || c.stock(Resource.STONE) < stone) {
            Text.bad(sp, "Для развития нужны запасы на складе: дерево " + wood + " (есть " + c.stock(Resource.WOOD) + ") и камень " + stone
                    + " (есть " + c.stock(Resource.STONE) + "). Сдайте материалы на Склад (ПКМ), /regnum stock.");
            return;
        }
        c.take(Resource.WOOD, wood);
        c.take(Resource.STONE, stone);
        c.treasury -= c.upgradeCost();
        c.level++;
        sp.serverLevel().playSound(null, c.hall, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 0.8f);
        Text.gold(sp, "🏰 «" + c.name + "» вырос до " + c.level + " уровня! Территория: " + c.radius() + " блоков.");
        Skills.addXp(sp, Skill.STEWARD, 40 * c.level);
        Skills.addXp(sp, Skill.LEADERSHIP, 15 * c.level);
        Skills.chronicle(sp, "«" + c.name + "» вырос до " + c.level + " уровня");
    }
}
