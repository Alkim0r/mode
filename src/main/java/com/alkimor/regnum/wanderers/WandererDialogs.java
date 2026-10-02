package com.alkimor.regnum.wanderers;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.InvUtil;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.dungeon.DungeonModule;
import com.alkimor.regnum.kingdom.BanditEntity;
import com.alkimor.regnum.kingdom.KingdomModule;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.survival.SurvivalModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Диалоги странников. Номер варианта приходит из команды /regnum talk. */
public final class WandererDialogs {
    private WandererDialogs() {}

    public static final ResourceKey<LootTable> WOUNDED_REWARD = ResourceKey.create(Registries.LOOT_TABLE, Regnum.id("gameplay/wounded_reward"));
    public static final ResourceKey<LootTable> HERMIT_REWARD = ResourceKey.create(Registries.LOOT_TABLE, Regnum.id("gameplay/hermit_reward"));
    public static final ResourceKey<LootTable> BANDIT_STASH = ResourceKey.create(Registries.LOOT_TABLE, Regnum.id("chests/bandit_stash"));

    private record Riddle(String question, String[] answers, int correct) {}

    private static final Riddle[] RIDDLES = {
            new Riddle("Без окон, без дверей — полна горница людей.", new String[]{"Тыква", "Огурец", "Улей"}, 1),
            new Riddle("Зимой и летом одним цветом.", new String[]{"Ель", "Камень", "Снег"}, 0),
            new Riddle("Сидит дед, во сто шуб одет. Кто его раздевает, тот слёзы проливает.", new String[]{"Капуста", "Лук", "Медведь"}, 1),
            new Riddle("Не лает, не кусает, а в дом не пускает.", new String[]{"Замок", "Забор", "Стражник"}, 0),
            new Riddle("Шипит и зеленеет, подкрадётся тихо — и от дома останется лишь яма.", new String[]{"Змея", "Крипер", "Ведьма"}, 1),
            new Riddle("Из лавы и воды рождён, лишь алмазу покорён.", new String[]{"Обсидиан", "Базальт", "Булыжник"}, 0),
            new Riddle("Высок, чёрен, взгляда не терпит, блоки уносит, сквозь даль скользит.", new String[]{"Ворон", "Эндермен", "Иссушитель"}, 1),
            new Riddle("Что всегда идёт, а с места не сходит?", new String[]{"Часы", "Река", "Время"}, 0),
            new Riddle("Чем больше из неё берёшь, тем больше она становится.", new String[]{"Яма", "Казна", "Слава"}, 0),
    };

    // ================================================================== приветствие

    public static void greet(WandererEntity w, ServerPlayer p) {
        Persona persona = w.getPersona();
        if (persona == Persona.NOBLE) {
            com.alkimor.regnum.dynasty.Dynasty.nobleGreet(w, p);
            return;
        }
        if (persona.isFamily()) {
            com.alkimor.regnum.dynasty.Dynasty.familyGreet(w, p);
            return;
        }
        if (w.done.contains(p.getUUID()) && persona != Persona.GUIDE && persona != Persona.TRICKSTER) {
            w.say(p, "Нам больше не о чем говорить, путник.");
            return;
        }
        int honor = Skills.data(p).honor();
        switch (persona) {
            case GUIDE -> {
                w.say(p, "Здравствуй, путник! Я исходил эти земли вдоль и поперёк. Ищешь древние логова — склепы, капища, гробницы? За "
                        + guidePrice(honor) + " изумр. нарисую карту к ближайшему.");
                w.options(p, "Купить карту логова (" + guidePrice(honor) + " изумр.)", "Расспросить о землях", "Уйти");
            }
            case TRICKSTER -> {
                w.say(p, "Эй, дружище! Такого товара больше ни у кого нет! Карта к тайнику разбойников — всего 3 изумруда. "
                        + "А чудо-эликсир сделает тебя сильнее медведя — 2!");
                w.options(p, "Карта сокровищ (3 изумр.)", "Чудо-эликсир (2 изумр.)", "Что-то ты мне не нравишься...", "Уйти");
            }
            case WOUNDED -> {
                w.say(p, "Помоги... разбойники отняли всё... Дай хоть корку хлеба... или перевяжи рану...");
                w.options(p, "Дать еды", "Перевязать рану (бинт)", "Обыскать его", "Пройти мимо");
            }
            case MERCHANT -> {
                w.say(p, "Мир тебе, путник! Везу товары издалека. Хочешь — покупай, есть что продать — возьму. "
                        + "Цены у меня по здешним меркам.");
                w.options(p, "Торговать", "Какие вести с дорог?", "Уйти");
            }
            case HERMIT -> {
                if (honor <= -20) {
                    w.say(p, "Я не говорю с теми, о ком дурная молва. Ступай прочь.");
                    return;
                }
                w.say(p, "Ищешь мудрости? Отгадай загадку — получишь награду. Ошибёшься — пеняй на себя.");
                w.options(p, "Слушаю загадку", "Уйти");
            }
        }
        com.alkimor.regnum.story.Quests.offerFromWanderer(p, persona.name());
    }

    // ================================================================== ответы

    public static void handle(WandererEntity w, ServerPlayer p, int option) {
        Skills.addXp(p, com.alkimor.regnum.survival.Skill.CHARM, 2);
        if (!w.isAlive() || p.distanceToSqr(w) > 8 * 8) {
            Text.info(p, "Вы слишком далеко от собеседника.");
            return;
        }
        if (w.done.contains(p.getUUID()) && w.getPersona() != Persona.GUIDE && w.getPersona() != Persona.TRICKSTER) return;
        if (w.getPersona() == Persona.NOBLE) {
            com.alkimor.regnum.dynasty.Dynasty.nobleOption(w, p, option);
            return;
        }
        if (w.getPersona().isFamily()) {
            com.alkimor.regnum.dynasty.Dynasty.familyOption(w, p, option);
            return;
        }
        switch (w.getPersona()) {
            case GUIDE -> guide(w, p, option);
            case TRICKSTER -> trickster(w, p, option);
            case WOUNDED -> wounded(w, p, option);
            case HERMIT -> hermit(w, p, option);
            case MERCHANT -> merchant(w, p, option);
        }
    }

    private static int guidePrice(int honor) {
        return honor >= 30 ? 3 : 6;
    }

    private static final String[] TIPS = {
            "Склепы прячутся под каменными мавзолеями. Внутри — нежить и алтарь древнего владыки.",
            "Сердце склепа нужно кузнецам для высшей закалки. Его носит в груди сам Владыка.",
            "Не верь торговцам, что продают карты сокровищ задёшево. Иной раз там ждут разбойники.",
            "Перевязывай раны вовремя — гнилая рана быстро становится лихорадкой.",
            "Целебные травы растут в высокой траве. Опытный глаз находит их чаще.",
            "Короли вербуют ополчение прямо в деревнях — жезлом командира и парой изумрудов.",
            "Разбойники нападают на города в ночь. Сторожевые башни замечают их загодя."
    };

    private static void guide(WandererEntity w, ServerPlayer p, int option) {
        switch (option) {
            case 0 -> {
                int price = guidePrice(Skills.data(p).honor());
                if (InvUtil.count(p, Items.EMERALD) < price && !p.getAbilities().instabuild) {
                    w.say(p, "Бесплатно только птицы поют. Приходи с изумрудами.");
                    return;
                }
                ServerLevel sl = p.serverLevel();
                BlockPos target = sl.findNearestMapStructure(com.alkimor.regnum.dungeon.RegionsModule.REGION_DUNGEONS, w.blockPosition(), 100, false);
                if (target == null) {
                    w.say(p, "Хм... В этих краях склепов я не встречал. Попытай счастья в другой стороне.");
                    return;
                }
                InvUtil.take(p, Items.EMERALD, price);
                ItemStack map = MapItem.create(sl, target.getX(), target.getZ(), (byte) 2, true, true);
                MapItem.renderBiomePreviewMap(sl, map);
                MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
                map.set(DataComponents.ITEM_NAME, Component.literal("Карта к древнему логову"));
                InvUtil.give(p, map);
                w.say(p, "Держи. Крестом отмечено логово древнего хозяина тех мест. Без хорошего меча и бинтов туда не суйся.");
            }
            case 1 -> {
                if (p.getRandom().nextBoolean()) {
                    BlockPos camp = p.serverLevel().findNearestMapStructure(DungeonModule.CAMPS_TAG, w.blockPosition(), 64, false);
                    if (camp != null) {
                        w.say(p, "Слыхал я, у " + camp.getX() + ", " + camp.getZ() + " разбойники стали лагерем. Добра у них награблено немало, "
                                + "да только атаман их — зверь.");
                        return;
                    }
                }
                w.say(p, TIPS[p.getRandom().nextInt(TIPS.length)]);
            }
            default -> w.say(p, "Доброй дороги!");
        }
    }

    private static void trickster(WandererEntity w, ServerPlayer p, int option) {
        switch (option) {
            case 0 -> {
                if (!pay(w, p, 3)) return;
                ServerLevel sl = p.serverLevel();
                double a = p.getRandom().nextDouble() * Math.PI * 2;
                int d = 120 + p.getRandom().nextInt(100);
                int x = w.getBlockX() + (int) (Math.cos(a) * d);
                int z = w.getBlockZ() + (int) (Math.sin(a) * d);
                BlockPos target = new BlockPos(x, sl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
                boolean trap = !Skills.has(p, com.alkimor.regnum.survival.Perk.RG_KING)
                        && p.getRandom().nextFloat() < (Skills.has(p, com.alkimor.regnum.survival.Perk.CH_SPEECH) ? 0.4f : 0.6f);
                ItemStack map = MapItem.create(sl, x, z, (byte) 1, true, true);
                MapItem.renderBiomePreviewMap(sl, map);
                MapItemSavedData.addTargetDecoration(map, target, "regnum_stash", MapDecorationTypes.RED_X);
                map.set(DataComponents.ITEM_NAME, Component.literal("Карта сокровищ"));
                map.set(WanderersModule.TREASURE_MARK.get(), new TreasureMark(target, trap));
                InvUtil.give(p, map);
                w.say(p, "Отличный выбор! Там зарыт клад самого атамана. Ступай скорее, пока не опередили!");
                if (p.getRandom().nextFloat() < 0.4f) {
                    w.say(p, "Ну, мне пора. Дела, знаешь ли...");
                    w.leaveSoon(40);
                }
            }
            case 1 -> {
                if (!pay(w, p, 2)) return;
                InvUtil.give(p, new ItemStack(WanderersModule.TRICK_ELIXIR.get()));
                w.say(p, "Пей до дна! Только не всё сразу... хе-хе.");
            }
            case 2 -> {
                int honor = Skills.data(p).honor();
                if (honor >= 10 || Skills.has(p, com.alkimor.regnum.survival.Perk.CH_SPEECH) || p.getRandom().nextFloat() < 0.5f + Skills.level(p, com.alkimor.regnum.survival.Skill.CHARM) * 0.004f) {
                    w.say(p, "Ладно-ладно, не кипятись! Не все мои карты... гм... честные. Удачи тебе, путник!");
                    w.leaveSoon(30);
                } else {
                    w.say(p, "Да как ты смеешь?! Я честный торговец! Не хочешь — не бери.");
                }
            }
            default -> w.say(p, "Заходи, если передумаешь!");
        }
    }

    private static final String[] TRADE_TIPS = {
            "Меха в тайге стоят гроши, а в пустыне за них дают втрое.",
            "Пряности везут из джунглей и пустынь — на севере их рвут с руками.",
            "Соль берут у моря. В горах без неё никуда.",
            "Не сбывай весь товар в одном городе — цена рухнет. Развози по разным рынкам.",
            "Чем дальше везёшь груз, тем дороже он стоит. Но и разбойники это знают.",
            "Свой рынок в своём городе платит казне пошлину с каждой сделки."
    };

    private static void merchant(WandererEntity w, ServerPlayer p, int option) {
        switch (option) {
            case 0 -> com.alkimor.regnum.trade.Trading.openMerchant(p, w);
            case 1 -> w.say(p, TRADE_TIPS[p.getRandom().nextInt(TRADE_TIPS.length)]);
            default -> w.say(p, "Попутного ветра!");
        }
    }

    private static boolean pay(WandererEntity w, ServerPlayer p, int price) {
        if (InvUtil.count(p, Items.EMERALD) < price && !p.getAbilities().instabuild) {
            w.say(p, "Без изумрудов — без товара!");
            return false;
        }
        InvUtil.take(p, Items.EMERALD, price);
        return true;
    }

    private static void wounded(WandererEntity w, ServerPlayer p, int option) {
        switch (option) {
            case 0, 1 -> {
                if (option == 0 && !takeFood(p)) {
                    Text.bad(p, "У вас нет еды.");
                    return;
                }
                if (option == 1 && !InvUtil.take(p, SurvivalModule.BANDAGE.get(), 1)) {
                    Text.bad(p, "У вас нет бинта.");
                    return;
                }
                w.done.add(p.getUUID());
                if (w.trap) {
                    springTrap(w, p, "Хе-хе! Добрая душа — лёгкая добыча! Ребята, сюда!");
                    return;
                }
                w.say(p, "Спасибо тебе, добрый человек... Возьми, что у меня осталось. И да хранят тебя боги.");
                giveLoot(p, w, WOUNDED_REWARD);
                Skills.addHonor(p, 5, "помощь раненому");
                if (option == 1) Skills.addXp(p, Skill.MEDICINE, 15);
                if (p.getRandom().nextFloat() < 0.4f) {
                    BlockPos crypt = p.serverLevel().findNearestMapStructure(DungeonModule.CRYPTS_TAG, w.blockPosition(), 64, false);
                    if (crypt != null) {
                        w.say(p, "Слушай... Я видел старый мавзолей у " + crypt.getX() + ", " + crypt.getZ() + ". Там нечисто, будь осторожен.");
                    }
                }
                w.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                w.leaveSoon(20 * 20);
            }
            case 2 -> {
                w.done.add(p.getUUID());
                if (w.trap) {
                    springTrap(w, p, "Ишь, какой шустрый! Обыскать меня вздумал? Ребята!");
                    return;
                }
                int n = 2 + p.getRandom().nextInt(4);
                InvUtil.give(p, new ItemStack(Items.EMERALD, n));
                w.say(p, "Нет... не надо... это последнее...");
                Skills.addHonor(p, -10, "ограблен раненый");
                w.leaveSoon(60);
            }
            default -> {
                w.say(p, "...");
                if (w.trap && p.getRandom().nextFloat() < 0.5f) {
                    w.done.add(p.getUUID());
                    springTrap(w, p, "Не хочешь по-хорошему? Тогда по-плохому!");
                }
            }
        }
    }

    private static boolean takeFood(ServerPlayer p) {
        if (p.getAbilities().instabuild) return true;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.has(DataComponents.FOOD)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Засада: «раненый» оказывается атаманом, из кустов выходят разбойники. */
    private static void springTrap(WandererEntity w, ServerPlayer p, String line) {
        w.say(p, line);
        ServerLevel sl = p.serverLevel();
        spawnBandit(sl, w.blockPosition(), BanditEntity.CAPTAIN, p);
        int n = 2 + p.getRandom().nextInt(3);
        for (int i = 0; i < n; i++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2;
            BlockPos at = w.blockPosition().offset((int) (Math.cos(a) * 7), 0, (int) (Math.sin(a) * 7));
            at = new BlockPos(at.getX(), sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
            spawnBandit(sl, at, p.getRandom().nextBoolean() ? BanditEntity.ARCHER : BanditEntity.THUG, p);
        }
        Text.bad(p, "Это засада!");
        w.discard();
    }

    public static void spawnBandit(ServerLevel sl, BlockPos at, int variant, ServerPlayer target) {
        BanditEntity b = KingdomModule.BANDIT.get().create(sl);
        if (b == null) return;
        b.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        b.setup(variant);
        b.setTarget(target);
        sl.addFreshEntity(b);
    }

    private static void hermit(WandererEntity w, ServerPlayer p, int option) {
        Integer current = w.riddle.get(p.getUUID());
        if (current == null) {
            if (option != 0) {
                w.say(p, "Ступай с миром.");
                return;
            }
            int idx = p.getRandom().nextInt(RIDDLES.length);
            w.riddle.put(p.getUUID(), idx);
            Riddle r = RIDDLES[idx];
            w.say(p, "Слушай: «" + r.question() + "»");
            String[] labels = new String[3];
            for (int i = 0; i < 3; i++) labels[i] = r.answers()[i];
            // ответы идут как варианты 10, 11, 12 — смещаем через пустые слоты
            String[] padded = new String[13];
            System.arraycopy(labels, 0, padded, 10, 3);
            w.options(p, padded);
            return;
        }
        if (option < 10 || option > 12) return;
        Riddle r = RIDDLES[current];
        w.riddle.remove(p.getUUID());
        w.done.add(p.getUUID());
        if (option - 10 == r.correct()) {
            w.say(p, "Верно! Ум твой остёр. Прими дар отшельника.");
            giveLoot(p, w, HERMIT_REWARD);
            p.serverLevel().addFreshEntity(new ExperienceOrb(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 30));
            Skills.addHonor(p, 2, "мудрость");
        } else {
            w.say(p, "Нет! Ответ был: «" + r.answers()[r.correct()] + "». Пусть слабость напомнит тебе о поспешности.");
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 60, 0));
        }
        w.leaveSoon(20 * 30);
    }

    private static void giveLoot(ServerPlayer p, WandererEntity w, ResourceKey<LootTable> key) {
        ServerLevel sl = p.serverLevel();
        LootTable table = sl.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(sl)
                .withParameter(LootContextParams.ORIGIN, w.position())
                .withParameter(LootContextParams.THIS_ENTITY, p)
                .create(LootContextParamSets.GIFT);
        for (ItemStack s : table.getRandomItems(params)) InvUtil.give(p, s);
    }
}
