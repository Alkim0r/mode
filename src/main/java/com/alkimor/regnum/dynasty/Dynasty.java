package com.alkimor.regnum.dynasty;

import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.RegnumConfig;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.kingdom.SoldierEntity;
import com.alkimor.regnum.survival.SurvivorData;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.wanderers.Persona;
import com.alkimor.regnum.wanderers.WandererEntity;
import com.alkimor.regnum.wanderers.WanderersModule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Династия в духе Mount &amp; Blade: сватовство к знатным особам, брак, дети,
 * взрослые наследники (командиры и наместники), продолжение игры за наследника.
 */
public final class Dynasty {
    private Dynasty() {}

    public static final int ADULT_DAYS = 8;
    public static final int BIRTH_INTERVAL_DAYS = 4;
    public static final int MAX_CHILDREN = 5;

    public static DynastyData data(Player p) {
        return p.getData(DynastyModule.DYNASTY);
    }

    public static long today(ServerLevel any) {
        return any.getServer().overworld().getDayTime() / 24000L;
    }

    // ================================================================== знатная особа

    public static void nobleGreet(WandererEntity w, ServerPlayer p) {
        int aff = w.affection.getOrDefault(p.getUUID(), 0);
        if (Skills.data(p).honor() < 0) {
            w.say(p, "Человек с такой молвой мне не пара. Ступай своей дорогой.");
            return;
        }
        w.say(p, aff < 30 ? "Приветствую, путник. Не каждый день в эти края заезжают гости." :
                aff < 70 ? "Ах, это снова вы! Рада нашей встрече." : "Я ждала вас... Сердце радуется, когда вы рядом.");
        p.sendSystemMessage(Text.of("  Расположение: " + aff + "/100", ChatFormatting.LIGHT_PURPLE));
        w.options(p, "Подарить подарок", "Побеседовать", aff >= 100 ? "Просить руки" : null, "Уйти");
    }

    public static void nobleOption(WandererEntity w, ServerPlayer p, int option) {
        int aff = w.affection.getOrDefault(p.getUUID(), 0);
        switch (option) {
            case 0 -> {
                int gain = takeGift(p);
                if (gain == 0) {
                    w.say(p, "Благодарю, но у вас нет ничего, что могло бы меня порадовать. (алмаз, золото, изумруды, торт, цветы)");
                    return;
                }
                if (Skills.has(p, com.alkimor.regnum.survival.Perk.CH_GIFTS)) gain = Math.round(gain * 1.25f);
                setAff(w, p, aff + gain);
                Skills.addXp(p, com.alkimor.regnum.survival.Skill.CHARM, 5);
                w.say(p, gain >= 15 ? "Какая красота! Вы меня балуете." : "Спасибо, это очень мило.");
            }
            case 1 -> {
                long day = today(p.serverLevel());
                Long last = w.lastTalkDay.get(p.getUUID());
                if (last != null && last == day) {
                    w.say(p, "Мы уже говорили сегодня. Приходите завтра — расскажете новые истории.");
                    return;
                }
                w.lastTalkDay.put(p.getUUID(), day);
                int honor = Skills.data(p).honor();
                int gain = 4 + Math.max(0, honor / 10) + Skills.level(p, com.alkimor.regnum.survival.Skill.CHARM) / 20;
                setAff(w, p, aff + gain);
                Skills.addXp(p, com.alkimor.regnum.survival.Skill.CHARM, 4);
                String[] lines = {"Расскажите о своих странствиях... Неужели вы правда видели Костяного Владыку?",
                        "Говорят, вы строите город. Каким он будет?", "Отец хочет выдать меня за старого боярина. Но разве сердцу прикажешь?",
                        "Я люблю смотреть на звёзды. А вы?"};
                w.say(p, lines[p.getRandom().nextInt(lines.length)]);
            }
            case 2 -> propose(w, p, aff);
            default -> w.say(p, "До встречи!");
        }
        p.sendSystemMessage(Text.of("  Расположение: " + w.affection.getOrDefault(p.getUUID(), 0) + "/100", ChatFormatting.LIGHT_PURPLE));
    }

    private static void setAff(WandererEntity w, ServerPlayer p, int v) {
        w.affection.put(p.getUUID(), Math.min(100, v));
    }

    private static int takeGift(ServerPlayer p) {
        if (InvUtil.take(p, Items.DIAMOND, 1)) return 20;
        if (InvUtil.take(p, Items.CAKE, 1)) return 14;
        if (InvUtil.take(p, Items.GOLD_INGOT, 1)) return 8;
        if (InvUtil.count(p, Items.EMERALD) >= 3 && InvUtil.take(p, Items.EMERALD, 3)) return 6;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ItemTags.FLOWERS)) {
                s.shrink(1);
                return 4;
            }
        }
        return 0;
    }

    private static void propose(WandererEntity w, ServerPlayer p, int aff) {
        DynastyData d = data(p);
        if (d.married()) {
            w.say(p, "Вы уже связаны узами брака!");
            return;
        }
        if (aff < 100) {
            w.say(p, "Не торопитесь... Мы ещё так мало знаем друг друга.");
            return;
        }
        int needHonor = Skills.has(p, com.alkimor.regnum.survival.Perk.CH_MATCH) ? 8 : 20;
        if (Skills.data(p).honor() < needHonor) {
            w.say(p, "Отец не отдаст меня за человека без доброго имени. Пусть о вас пойдёт честная слава (честь " + needHonor + "+).");
            return;
        }
        KingdomData kd = KingdomData.get(p.server);
        City city = kd.nearestOwned(p.getUUID(), p.blockPosition());
        if (city == null) {
            w.say(p, "Куда же вы приведёте жену? Сначала обзаведитесь своим городом.");
            return;
        }
        String name = w.getCustomName() != null ? w.getCustomName().getString().replace(Persona.NOBLE.title + " ", "") : "Злата";
        w.becomeFamily(Persona.SPOUSE, p.getUUID(), name + ", супруга правителя");
        BlockPos home = city.hall.offset(2, 1, 2);
        w.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
        w.restrictTo(city.hall, 12);
        d.spouseId = w.getUUID();
        d.spouseName = name;
        d.lastBirthDay = today(p.serverLevel());
        w.say(p, "Да! Тысячу раз да!");
        p.server.getPlayerList().broadcastSystemMessage(Text.of("💍 " + p.getName().getString() + " и " + name
                + " сыграли свадьбу в городе «" + city.name + "»!", ChatFormatting.LIGHT_PURPLE), false);
        Skills.addHonor(p, 5, "свадьба");
        Skills.chronicle(p, "Свадьба с " + name + " в городе «" + city.name + "»");
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.5f);
    }

    // ================================================================== семья

    public static void familyGreet(WandererEntity w, ServerPlayer p) {
        if (!p.getUUID().equals(w.familyOf)) {
            w.say(p, "Простите, я жду своего правителя.");
            return;
        }
        long day = today(p.serverLevel());
        if (w.getPersona() == Persona.SPOUSE) {
            w.say(p, "Рада видеть тебя дома, мой правитель. Как прошёл поход?");
            w.options(p, "Как наши дети?", "Береги дом", "Уйти");
        } else if (w.isChild()) {
            String[] lines = {"Папа! А ты правда победил дракона?", "Я тоже хочу быть рыцарем!", "Мама сказала, что ты самый храбрый.",
                    "Научи меня держать меч!"};
            w.say(p, lines[p.getRandom().nextInt(lines.length)] + " (вырастет через " + Math.max(0, ADULT_DAYS - (day - w.bornDay)) + " дн.)");
        } else {
            w.say(p, "Я готов служить роду. Что прикажешь?");
            w.options(p, "Будь моим полководцем", "Правь городом как наместник", "Оставайся при дворе");
        }
    }

    public static void familyOption(WandererEntity w, ServerPlayer p, int option) {
        if (!p.getUUID().equals(w.familyOf)) return;
        DynastyData d = data(p);
        long day = today(p.serverLevel());
        if (w.getPersona() == Persona.SPOUSE) {
            if (option == 0) {
                if (d.children.isEmpty()) w.say(p, "Пока детей у нас нет... но всё впереди.");
                for (DynastyData.Child c : d.children) {
                    p.sendSystemMessage(Text.of("  • " + c.name() + " — " + (DynastyData.adult(c, day) ? "взрослый" : "ребёнок, " + (day - c.bornDay()) + " дн.")
                            + (c.role().isEmpty() ? "" : ", " + roleTitle(c.role())), ChatFormatting.GRAY));
                }
            } else if (option == 1) {
                w.say(p, "Я присмотрю за всем. Возвращайся живым.");
            }
            return;
        }
        if (w.isChild()) return;
        String role = switch (option) {
            case 0 -> "commander";
            case 1 -> "governor";
            default -> "";
        };
        setRole(w, p, d, role);
    }

    public static String roleTitle(String role) {
        return switch (role) {
            case "commander" -> "полководец";
            case "governor" -> "наместник";
            default -> "при дворе";
        };
    }

    private static void setRole(WandererEntity w, ServerPlayer p, DynastyData d, String role) {
        KingdomData kd = KingdomData.get(p.server);
        // снять с прежнего наместничества
        for (City c : kd.ownedBy(p.getUUID())) {
            if (c.governor.equals(w.getUUID().toString())) {
                c.governor = "";
                kd.setDirty();
            }
        }
        w.heirRole = role;
        d.children.replaceAll(c -> c.id().equals(w.getUUID()) ? c.withRole(role) : c);
        switch (role) {
            case "commander" -> {
                w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                w.clearRestriction();
                w.say(p, "Я поведу твоих воинов! Рядом со мной они будут биться яростнее.");
            }
            case "governor" -> {
                City c = kd.nearestOwned(p.getUUID(), p.blockPosition());
                if (c == null) {
                    w.say(p, "Но у нас нет города, которым я мог бы править.");
                    w.heirRole = "";
                    return;
                }
                c.governor = w.getUUID().toString();
                c.governorName = w.getCustomName() != null ? w.getCustomName().getString() : "наместник";
                kd.setDirty();
                w.restrictTo(c.hall, 12);
                w.moveTo(c.hall.getX() + 0.5, c.hall.getY() + 1, c.hall.getZ() + 2.5, 0, 0);
                w.say(p, "Город «" + c.name + "» в надёжных руках: налоги +10%, жалованье армии −25%.");
            }
            default -> w.say(p, "Как скажешь.");
        }
    }

    /** Поведение членов семьи (вызывается из aiStep странника на сервере). */
    public static void tickFamily(WandererEntity w) {
        if (!(w.level() instanceof ServerLevel sl) || w.familyOf == null) return;
        long day = today(sl);
        if (w.getPersona() == Persona.HEIR && w.isChild() && day - w.bornDay >= ADULT_DAYS) {
            w.setChild(false);
            ServerPlayer parent = sl.getServer().getPlayerList().getPlayer(w.familyOf);
            if (parent != null) {
                Text.gold(parent, "Ваш наследник " + w.getName().getString() + " вырос! Поговорите с ним, чтобы назначить роль.");
                Skills.chronicle(parent, w.getName().getString() + " достиг совершеннолетия");
            }
        }
        if ("commander".equals(w.heirRole) && w.tickCount % 20 == 0) {
            Player king = sl.getPlayerByUUID(w.familyOf);
            if (king != null && king.level() == sl) {
                double d2 = w.distanceToSqr(king);
                if (d2 > 32 * 32) w.moveTo(king.getX(), king.getY(), king.getZ(), w.getYRot(), 0);
                else if (d2 > 25) w.getNavigation().moveTo(king, 1.2);
                if (w.tickCount % 100 == 0) {
                    for (SoldierEntity s : sl.getEntitiesOfClass(SoldierEntity.class, w.getBoundingBox().inflate(16), s -> s.isOwnedBy(king))) {
                        s.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, false));
                    }
                }
            }
        }
    }

    public static void onFamilyDeath(ServerLevel sl, WandererEntity w) {
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(w.familyOf);
        if (p == null) return;
        DynastyData d = data(p);
        if (w.getUUID().equals(d.spouseId)) {
            d.spouseId = null;
            Text.bad(p, "Горе в доме: " + d.spouseName + " погибла.");
            Skills.chronicle(p, "Погибла супруга " + d.spouseName);
        } else if (d.children.removeIf(c -> c.id().equals(w.getUUID()))) {
            Text.bad(p, "Горе в доме: погиб наследник " + w.getName().getString() + ".");
            Skills.chronicle(p, "Погиб наследник " + w.getName().getString());
        }
    }

    // ================================================================== рождение детей

    public static void tickBirths(ServerPlayer p) {
        DynastyData d = data(p);
        if (!d.married() || d.children.size() >= MAX_CHILDREN) return;
        ServerLevel ow = p.server.overworld();
        long day = today(ow);
        if (day - d.lastBirthDay < (Skills.has(p, com.alkimor.regnum.survival.Perk.CH_DYNASTY) ? BIRTH_INTERVAL_DAYS / 2 : BIRTH_INTERVAL_DAYS)) return;
        Entity spouse = ow.getEntity(d.spouseId);
        if (!(spouse instanceof WandererEntity mother) || !mother.isAlive()) return;
        WandererEntity child = WanderersModule.WANDERER.get().create(ow);
        if (child == null) return;
        String name = Persona.HEIR.names[ow.random.nextInt(Persona.HEIR.names.length)];
        child.moveTo(mother.getX() + 1, mother.getY(), mother.getZ(), 0, 0);
        child.finalizeSpawn(ow, ow.getCurrentDifficultyAt(mother.blockPosition()), MobSpawnType.BREEDING, null);
        child.becomeFamily(Persona.HEIR, p.getUUID(), name);
        child.setChild(true);
        child.bornDay = day;
        if (mother.hasRestriction()) child.restrictTo(mother.getRestrictCenter(), 12);
        ow.addFreshEntity(child);
        d.children.add(new DynastyData.Child(child.getUUID(), name, day, ""));
        d.lastBirthDay = day;
        ow.sendParticles(ParticleTypes.HEART, child.getX(), child.getY() + 1, child.getZ(), 8, 0.4, 0.4, 0.4, 0);
        Text.gold(p, "👶 В вашем роду пополнение: родился наследник " + name + "!");
        Skills.chronicle(p, "Родился наследник " + name);
    }

    // ================================================================== смерть и наследник

    /** После смерти: штраф к навыкам (как в PZ), но можно продолжить за взрослого наследника без потерь. */
    public static void onRespawn(ServerPlayer p, ServerPlayer original) {
        double loss = RegnumConfig.SKILL_LOSS_ON_DEATH.get();
        SurvivorData s = Skills.data(p);
        DynastyData d = data(p);
        d.xpSnapshot.clear();
        d.xpSnapshot.putAll(s.snapshot());
        d.snapshotTime = p.serverLevel().getGameTime();
        if (loss > 0) {
            s.scaleAll(1.0 - loss);
            Text.bad(p, "Смерть забрала часть опыта: навыки ослабли на " + Math.round(loss * 100) + "%.");
        }
        long day = today(p.serverLevel());
        d.firstAdult(day).ifPresent(heir -> p.sendSystemMessage(Text.of("Род не прервётся. ", ChatFormatting.GOLD)
                .append(Text.button("Продолжить за наследника " + heir.name(), "/regnum heir",
                        "Вы продолжите путь как " + heir.name() + ": опыт навыков сохранится, поколение рода +1"))));
        Skills.sync(p, false);
    }

    public static void continueAsHeir(ServerPlayer p) {
        DynastyData d = data(p);
        long day = today(p.serverLevel());
        if (d.xpSnapshot.isEmpty() || p.serverLevel().getGameTime() - d.snapshotTime > 20 * 60 * 10) {
            Text.bad(p, "Передать наследие можно только в течение 10 минут после гибели.");
            return;
        }
        var heirOpt = d.firstAdult(day);
        if (heirOpt.isEmpty()) {
            Text.bad(p, "У вас нет взрослого наследника.");
            return;
        }
        DynastyData.Child heir = heirOpt.get();
        Skills.data(p).restore(d.xpSnapshot);
        d.xpSnapshot.clear();
        d.children.remove(heir);
        d.generation++;
        Entity e = p.server.overworld().getEntity(heir.id());
        if (e != null) {
            p.teleportTo(p.server.overworld(), e.getX(), e.getY(), e.getZ(), e.getYRot(), 0);
            e.discard();
        }
        KingdomData kd = KingdomData.get(p.server);
        for (City c : kd.ownedBy(p.getUUID())) {
            if (c.governor.equals(heir.id().toString())) {
                c.governor = "";
                c.governorName = "";
                kd.setDirty();
            }
        }
        com.alkimor.regnum.survival.PlayerStats.apply(p);
        Skills.sync(p, false);
        p.server.getPlayerList().broadcastSystemMessage(Text.of("👑 Род продолжает " + heir.name() + " — " + d.generation + "-е поколение династии.",
                ChatFormatting.GOLD), false);
        Skills.chronicle(p, "Наследие принял " + heir.name() + " (" + d.generation + "-е поколение)");
    }

    /** Строки для страницы «Род» в дневнике. */
    public static List<String> familyLines(ServerPlayer p) {
        DynastyData d = data(p);
        long day = today(p.serverLevel());
        List<String> lines = new ArrayList<>();
        lines.add("Поколение династии: " + d.generation);
        lines.add(d.married() ? "Супруга: " + d.spouseName : "Не в браке (ищите знатных особ среди странников)");
        if (d.children.isEmpty()) lines.add("Детей пока нет");
        for (DynastyData.Child c : d.children) {
            lines.add("• " + c.name() + " — " + (DynastyData.adult(c, day) ? "взрослый, " + roleTitle(c.role()) : "ребёнок (" + (day - c.bornDay()) + " дн.)"));
        }
        return lines;
    }

    @SuppressWarnings("unused")
    private static Component unused() {
        return Component.empty();
    }
}
