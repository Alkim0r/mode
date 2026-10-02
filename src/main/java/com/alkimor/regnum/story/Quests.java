package com.alkimor.regnum.story;

import com.alkimor.regnum.Regnum;
import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.BuildingType;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.story.QuestDef.Kind;
import com.alkimor.regnum.story.QuestDef.Line;
import com.alkimor.regnum.survival.Skills;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Квесты и сюжет: главная линия «Печать Пепла» (тёмное фэнтези), побочные поручения и задания фракций.
 * Прогресс хранится в постоянных данных игрока. Главные квесты начинаются сами, остальные предлагаются, когда выполнены условия.
 */
public final class Quests {
    private Quests() {}

    public static final String ROOT = "regnum_story";

    public static final Map<String, String> FACTIONS = new LinkedHashMap<>();
    static {
        FACTIONS.put("keep", "Хранители печати");
        FACTIONS.put("guard", "Стража границ");
        FACTIONS.put("trade", "Вольные торговцы");
        FACTIONS.put("hunt", "Охотники склепов");
    }

    public static final String[] REP_TITLES = {"Незнакомец", "Знакомый", "Друг", "Союзник", "Почётный гость"};
    public static final int[] REP_TIERS = {0, 15, 40, 90, 180};

    public static final Map<String, QuestDef> DEFS = new LinkedHashMap<>();

    private static QuestDef q(QuestDef d) {
        DEFS.put(d.id, d);
        return d;
    }

    private static final String CRYPTS = "regnum:crypt|regnum:dt/undead_crypt|regnum:dt/creeping_crypt";
    private static final String SWAMPS = "regnum:sunken_shrine|regnum:dt/toxic_lair|regnum:dt/witch_villa";
    private static final String FORGES = "regnum:forge_fortress|regnum:dt/lone_citadel|regnum:dt/bunker|regnum:dt/stray_fort";
    private static final String TOMBS = "regnum:sand_tomb|regnum:dt/desert_ruins";
    private static final String VILLAGES = "minecraft:village_plains|minecraft:village_desert|minecraft:village_savanna|minecraft:village_snowy|minecraft:village_taiga";

    static {
        // ---------- Главная линия: «Печать Пепла»
        q(new QuestDef("m1", Line.MAIN, "Пепельный сон", "Хранитель Кассиан", "keep", 0,
                "Мне снится пепел, герой. Старая печать трескается — и мёртвые встают раньше срока. Остановите хотя бы тех, что бродят у ваших границ.",
                "Вы чувствуете — это только начало. Придите ко мне, когда у вас будет свой очаг.")
                .kill("#undead", 3, "Упокойте 3 мертвеца").reward("item:bandage:4", "xp:30", "rep:keep:10"));
        q(new QuestDef("m2", Line.MAIN, "Кров для живых", "Хранитель Кассиан", "keep", 0,
                "Печать держали четыре стража, но люди забыли их. Чтобы вернуть печать, вам нужна опора — город, люди, казна. Основайте поселение.",
                "Теперь у вас есть голос. Сильный голос будет услышан даже в склепе.")
                .pre("m1").city(1, "Основать город (Ратуша)").reward("gold:40", "xp:40", "rep:keep:10"));
        q(new QuestDef("m3", Line.MAIN, "Склеп под холмом", "Хранитель Кассиан", "keep", 0,
                "Первый страж — Морграт, повелитель склепа. Найдите его обитель. /regnum story locate покажет направление.",
                "Вы видели вход. Эхо ещё долго будет звать вас обратно.")
                .pre("m2").visit(CRYPTS, "Найти склеп").reward("item:star_iron:1", "xp:40"));
        q(new QuestDef("m4", Line.MAIN, "Первая печать: Морграт", "Хранитель Кассиан", "keep", 0,
                "Морграт не злодей — он страж, которого сломали. Освободите его последним ударом. Возьмите с собой людей.",
                "Первая печать пала… и треснула заново. Вы сделали больше, чем думаете.")
                .pre("m3").kill("regnum:crypt_lord", 1, "Победить Морграта")
                .reward("item:star_iron:3", "xp:150", "rep:keep:20", "buff:minecraft:strength:900:0", "flag:seal1"));
        q(new QuestDef("m5", Line.MAIN, "Болотный шёпот", "Хранитель Кассиан", "keep", 0,
                "Топь поёт во сне. Матушка Топь осталась верна печати дольше всех — и сильнее всех сломалась. Найдите её святилище.",
                "Вода холодна, как пепел. Но вы дошли.")
                .pre("m4").visit(SWAMPS, "Найти болотное святилище").reward("item:healing_herb:8", "xp:60"));
        q(new QuestDef("m6", Line.MAIN, "Вторая печать: Матушка Топь", "Хранитель Кассиан", "keep", 0,
                "Её яд — слёзы по тем, кого она не уберегла. Принесите ей покой, но берегитесь тумана.",
                "Топь затихла. Но печать чует — осталось двое.")
                .pre("m5").kill("regnum:mire_mother", 1, "Победить Матушку Топь")
                .reward("item:star_iron:3", "xp:180", "rep:keep:20", "buff:minecraft:regeneration:600:1", "flag:seal2"));
        q(new QuestDef("m7", Line.MAIN, "Погасший горн", "Хранитель Кассиан", "keep", 0,
                "Горн Горнового когда-то ковал оружие против Безымянного. Теперь он остыл. Отыщите кузницу-крепость.",
                "Холодный металл помнит каждый удар молота.")
                .pre("m6").visit(FORGES, "Найти кузню-крепость").reward("item:iron_ingot:12", "xp:70"));
        q(new QuestDef("m8", Line.MAIN, "Третья печать: Горновой", "Хранитель Кассиан", "keep", 0,
                "Горновой не слышит слов — только удары. Говорите с ним железом. И не стойте в огне.",
                "Горн погас — и разгорелся снова, уже в вашем сердце.")
                .pre("m7").kill("regnum:forgemaster", 1, "Победить Горнового")
                .reward("item:star_iron:4", "xp:220", "rep:keep:20", "buff:minecraft:fire_resistance:1800:0", "flag:seal3"));
        q(new QuestDef("m9", Line.MAIN, "Песок хранит имена", "Хранитель Кассиан", "keep", 0,
                "Последняя печать лежит под песками. Сехмет-ра помнит имя Безымянного — и поэтому её терзает больше всех.",
                "Ветер заметает следы, но вы нашли тропу.")
                .pre("m8").visit(TOMBS, "Найти гробницу в песках").reward("item:gold_ingot:8", "xp:80"));
        q(new QuestDef("m10", Line.MAIN, "Четвёртая печать: Сехмет-ра", "Хранитель Кассиан", "keep", 0,
                "Царица скарабеев призовёт ваших врагов из-под земли. Держите строй, не бегите — песок быстрее.",
                "Все четыре печати говорят с вами. И что-то огромное услышало.")
                .pre("m9").kill("regnum:scarab_queen", 1, "Победить Сехмет-ра")
                .reward("item:star_iron:5", "xp:300", "rep:keep:30", "buff:minecraft:resistance:1200:1", "flag:seal4"));
        q(new QuestDef("m11", Line.MAIN, "Кузня печатей", "Хранитель Кассиан", "keep", 0,
                "Теперь печать нужно перековать. Нужно звёздное железо — много. И город, достойный столицы. Безымянный ждёт. (Конец первой арки.)",
                "Вы сделали всё, что мог один человек. Следующая глава впереди — Безымянный ещё не назвал своего имени.")
                .pre("m10").collect("regnum:star_iron", 8, "Принести 8 звёздного железа").city(3, "Довести город до 3 уровня")
                .reward("gold:200", "xp:500", "rep:keep:40", "flag:arc1_done"));

        // ---------- Вторая арка: «Имя Безымянного»
        q(new QuestDef("m12", Line.MAIN, "Эхо в глубине", "Хранитель Кассиан", "keep", 0,
                "Печати перекованы — и то, что спало под ними, зашевелилось. В шахтах появились ползуны: они чуют пустоту там, где была печать. Проредите стаи, держа факелы под рукой.",
                "Они боятся света, но голод сильнее страха. Глубже — то, что их ведёт.")
                .pre("m11").flag("arc1_done", "Завершить первую арку").kill("regnum:crawler", 10, "Убить 10 шахтных ползунов")
                .reward("item:regnum:chitin_plate:3", "xp:120", "rep:hunt:15"));
        q(new QuestDef("m13", Line.MAIN, "Королева ползунов", "Хранитель Кассиан", "keep", 0,
                "Стаи ведёт королева. Она просыпается там, где убито достаточно её детей — глубже двадцатого уровня. Возьмите щиты, факелы и людей: её рывок оглушает её саму.",
                "Гнездо молчит. Но то, что она берегла, вам ещё предстоит понять.")
                .pre("m12").kill("regnum:crawler_queen", 1, "Победить Королеву ползунов")
                .reward("item:star_iron:4", "xp:260", "rep:keep:20", "rep:hunt:20", "buff:minecraft:night_vision:1800:0"));
        q(new QuestDef("m14", Line.MAIN, "Друзья на границе", "Хранитель Кассиан", "keep", 0,
                "Безымянный придёт не один. Заручитесь союзником среди соседних королевств и подготовьте армию — хотя бы двадцать бойцов.",
                "Союз скреплён. Теперь вам есть на кого опереться.")
                .pre("m13").state("ally", 1, "Заключить союз с королевством").state("army", 20, "Собрать армию в 20 бойцов")
                .reward("gold:150", "xp:180", "rep:guard:20"));
        q(new QuestDef("m15", Line.MAIN, "Клинок для печати", "Хранитель Кассиан", "keep", 0,
                "Печать скована, но ей нужен хранитель с именным клинком. Выкуйте легендарный клинок в своей кузнице.",
                "Клинок поёт. Безымянный услышит его первым.")
                .pre("m14").state("legend", 1, "Выковать легендарный клинок (/regnum legend)")
                .reward("item:star_iron:3", "xp:200", "rep:keep:20"));
        q(new QuestDef("m16", Line.MAIN, "Дозоры печати", "Хранитель Кассиан", "keep", 0,
                "Печать слабеет вдали от городов. Возьмите под защиту деревни и назначьте им дозорную цель — аванпосты увидят беду первыми.",
                "Огни дозоров горят на всех холмах. Вас узнают по ним.")
                .pre("m15").state("vassals", 2, "Взять под защиту 2 деревни").state("outpost", 1, "Назначить аванпосту цель (/regnum village purpose)")
                .reward("gold:120", "xp:200", "rep:guard:20", "rep:trade:10"));
        q(new QuestDef("m17", Line.MAIN, "Знание древних", "Хранитель Кассиан", "keep", 0,
                "Безымянный забыт не случайно: летописи стёрты. Только наука вернёт имя. Изучите двенадцать технологий.",
                "Свитки сложились в слово. Осталось его произнести.")
                .pre("m16").state("techs", 12, "Изучить 12 технологий").reward("xp:280", "rep:keep:25", "item:star_iron:3"));
        q(new QuestDef("m18", Line.MAIN, "Имя", "Хранитель Кассиан", "keep", 0,
                "Теперь всё готово. Нужны хитин ползунов, звёздное железо и город — столица, достойная имени. Принесите всё это, и печать назовёт того, кого вы ищете. (Конец второй арки.)",
                "Имя произнесено. Безымянный теперь знает, что его ждут. Третья глава — впереди.")
                .pre("m17").collect("regnum:chitin_plate", 6, "Принести 6 хитиновых пластин").collect("regnum:star_iron", 12, "Принести 12 звёздного железа")
                .city(5, "Довести город до 5 уровня")
                .reward("gold:400", "xp:800", "rep:keep:50", "buff:minecraft:strength:3600:1", "flag:arc2_done"));

        // ---------- Третья арка: «Война имён»
        q(new QuestDef("m19", Line.MAIN, "Тень на границе", "Хранитель Кассиан", "keep", 0,
                "Имя названо, и Безымянный ответил: на дорогах стало больше разбойников, их кто-то ведёт. Очистите границы и поднимите город до шестого уровня — слабому городу печать не доверят.",
                "Дороги затихли, но тень лишь отступила. Она ждёт на севере.")
                .pre("m18").flag("arc2_done", "Завершить вторую арку").kill("regnum:bandit", 20, "Убить 20 разбойников").city(6, "Довести город до 6 уровня")
                .reward("gold:300", "xp:400", "rep:guard:30"));
        q(new QuestDef("m20", Line.MAIN, "Дань мёртвых", "Хранитель Кассиан", "keep", 0,
                "Безымянный поднимает павших у старых печатей. Упокойте мертвецов и принесите звёздное железо — из него выкуем серебряные клинки для стражи.",
                "Железо звенит. Мертвецы ещё придут, но у вас есть чем встретить их.")
                .pre("m19").kill("#undead", 40, "Упокоить 40 мертвецов").collect("regnum:star_iron", 16, "Принести 16 звёздного железа")
                .reward("xp:450", "rep:keep:30", "rep:hunt:25", "buff:minecraft:strength:2400:1"));
        q(new QuestDef("m21", Line.MAIN, "Союзы и караваны", "Хранитель Кассиан", "keep", 0,
                "Одного города мало. Нужны союзник, торговый путь и четыре деревни под вашей рукой — тогда весть о печати дойдёт до каждого берега.",
                "Огни зажжены от моря до гор. Безымянный увидит: вы не одни.")
                .pre("m20").state("ally", 1, "Иметь союзника").state("trade", 1, "Открыть торговый путь").state("vassals", 4, "Взять под защиту 4 деревни")
                .reward("gold:400", "xp:420", "rep:guard:30", "rep:trade:30"));
        q(new QuestDef("m22", Line.MAIN, "Клятва легенд", "Хранитель Кассиан", "keep", 0,
                "Для последнего похода нужны два именных клинка и тридцать бойцов в строю. Клятва связывает их с печатью.",
                "Клинки дали клятву. Строй стоит.")
                .pre("m21").state("legend", 2, "Иметь 2 легендарных клинка").state("army", 30, "Собрать армию в 30 бойцов")
                .reward("item:star_iron:6", "xp:500", "rep:keep:30", "buff:minecraft:resistance:2400:1"));
        q(new QuestDef("m23", Line.MAIN, "Слуги Безымянного", "Хранитель Кассиан", "keep", 0,
                "Три хранителя старых печатей служат ему: Морграт, Матушка Топь и Сехмет-ра. Их надо победить ещё раз — теперь они сильны его силой.",
                "Слуги пали. Остался он сам.")
                .pre("m22").kill("regnum:crypt_lord", 1, "Победить Морграта").kill("regnum:mire_mother", 1, "Победить Матушку Топь").kill("regnum:scarab_queen", 1, "Победить Сехмет-ра")
                .reward("gold:500", "xp:900", "rep:keep:40", "rep:hunt:40"));
        q(new QuestDef("m24", Line.MAIN, "Конец безымянной ночи", "Хранитель Кассиан", "keep", 0,
                "Последняя печать — это вы. Изучите двадцать технологий, поднимите столицу до седьмого уровня и принесите двадцать звёздного железа и восемь хитиновых пластин: из них отольют ключ. (Финал кампании.)",
                "Ключ повёрнут. Ночь кончилась — и началась ваша хроника. Дальше история пишется вами и вашими друзьями.")
                .pre("m23").state("techs", 20, "Изучить 20 технологий").city(7, "Довести город до 7 уровня")
                .collect("regnum:star_iron", 20, "Принести 20 звёздного железа").collect("regnum:chitin_plate", 8, "Принести 8 хитиновых пластин")
                .reward("gold:1000", "xp:2000", "rep:keep:100", "buff:minecraft:strength:7200:2", "flag:campaign_done"));

        // ---------- Побочные
        q(new QuestDef("s1", Line.SIDE, "Травница с хутора", "Травница Ольга", "keep", 0,
                "Раненых много, бинтов нет. Принесите лечебных трав — отплачу чем смогу.",
                "Вот вам снадобья в дорогу. Береги вас богиня-мать.")
                .collect("regnum:healing_herb", 8, "Собрать 8 лечебных трав").reward("item:bandage:6", "xp:30", "rep:keep:5"));
        q(new QuestDef("s2", Line.SIDE, "Дорога без разбойников", "Капитан Верна", "guard", 0,
                "Разбойники режут обозы. Покажите им, что дорога охраняется.",
                "Хорошая работа. Стража ценит тех, кто не прячется за стенами.")
                .kill("regnum:bandit", 5, "Убить 5 разбойников").reward("gold:30", "xp:40", "rep:guard:15"));
        q(new QuestDef("s3", Line.SIDE, "Лагерь на холме", "Капитан Верна", "guard", 15,
                "Разбойничий лагерь — гнездо. Разорите его, и дороги дышат свободнее.",
                "Лагерь пуст. Люди будут помнить это имя.")
                .pre("s2").visit("regnum:bandit_camp", "Найти разбойничий лагерь").kill("regnum:bandit", 6, "Убить 6 разбойников")
                .reward("item:iron_ingot:10", "xp:70", "rep:guard:20", "buff:minecraft:speed:900:0"));
        q(new QuestDef("s4", Line.SIDE, "Голодная деревня", "Староста Борис", "trade", 0,
                "У нас неурожай, люди голодают. Привезите хлеба — отблагодарим.",
                "Вы спасли целую зиму. Купцы это запомнят.")
                .visit(VILLAGES, "Добраться до деревни").collect("minecraft:bread", 12, "Принести 12 хлебов")
                .reward("item:emerald:6", "xp:40", "rep:trade:15"));
        q(new QuestDef("s5", Line.SIDE, "Охотник за костями", "Охотник Гаррен", "hunt", 0,
                "Кости мертвецов — лучшее оружие против мертвецов. Принесите дюжину.",
                "Годится. Из этого сделаем наконечники.")
                .collect("minecraft:bone", 12, "Собрать 12 костей").reward("item:iron_ingot:4", "xp:30", "rep:hunt:15"));
        q(new QuestDef("s6", Line.SIDE, "Мёртвый дозор", "Охотник Гаррен", "hunt", 15,
                "Мёртвые ходят дозором. Сломайте строй дозора — десятью ударами.",
                "Теперь ночью спокойнее. Вы — один из нас.")
                .pre("s5").kill("#undead", 10, "Упокоить 10 мертвецов").reward("item:star_iron:1", "xp:60", "rep:hunt:20"));
        q(new QuestDef("s7", Line.SIDE, "Кузнечный долг", "Торговка Мирра", "trade", 0,
                "Мне нужен кузнец и железо. Постройте кузницу в городе и принесите металл — оплачу золотом.",
                "Звенит! Вот ваши монеты.")
                .build("SMITHY", "Построить кузницу").collect("minecraft:iron_ingot", 10, "Принести 10 железных слитков")
                .reward("gold:50", "xp:40", "rep:trade:15"));
        q(new QuestDef("s8", Line.SIDE, "Лазарет для хутора", "Хранитель Кассиан", "keep", 0,
                "Раны гноятся. Постройте лазарет — иначе болезнь пойдёт по дорогам.",
                "Чистые бинты и чистая совесть. Спасибо.")
                .build("INFIRMARY", "Построить лазарет").reward("item:healing_herb:6", "xp:40", "rep:keep:10"));
        q(new QuestDef("s9", Line.SIDE, "Шёлковый путь", "Торговка Мирра", "trade", 15,
                "Хороший рынок привлекает караваны. Поднимите город и откройте рынок.",
                "Теперь караваны будут знать дорогу к вам.")
                .pre("s4").build("MARKET", "Построить рынок").city(2, "Довести город до 2 уровня")
                .reward("gold:80", "xp:60", "rep:trade:20"));
        q(new QuestDef("s10", Line.SIDE, "Слава короны", "Капитан Верна", "guard", 20,
                "Стража верит в тех, кто держит землю. Укрепите город и очистите дороги.",
                "Ваше имя вписано в книгу стражи.")
                .pre("s2").city(3, "Город 3 уровня").kill("regnum:bandit", 8, "Убить 8 разбойников")
                .reward("gold:100", "xp:90", "rep:guard:25"));
        q(new QuestDef("s11", Line.SIDE, "Пауки у реки", "Охотник Гаррен", "hunt", 0,
                "По ночам из-за реки лезут пауки. Перебейте шестерых.",
                "Паутина больше не мешает ловушкам.")
                .kill("minecraft:spider", 6, "Убить 6 пауков").reward("item:string:12", "xp:30", "rep:hunt:10"));
        q(new QuestDef("s12", Line.SIDE, "Ночной гость", "Хранитель Кассиан", "keep", 0,
                "Фантомы кружат над башней — их притягивает беспокойный сон. Сбейте двоих.",
                "Сон стал тише.")
                .pre("m1").kill("minecraft:phantom", 2, "Сбить 2 фантомов").reward("item:phantom_membrane:2", "xp:50", "rep:keep:10"));

        // ---------- Побочные: рост королевства
        q(new QuestDef("s13", Line.SIDE, "Свет знаний", "Хранитель Кассиан", "keep", 0,
                "Печать — это знание. Научитесь письму, и мои свитки станут вашими.", "Первые буквы — первые победы.")
                .state("tech:WRITING", 1, "Изучить «Письменность»").reward("item:healing_herb:4", "xp:40", "rep:keep:10"));
        q(new QuestDef("s14", Line.SIDE, "Шесть наук", "Хранитель Кассиан", "keep", 15,
                "Королевство без учёных — как меч без руки. Изучите шесть технологий.", "Вы ведёте за собой не только мечи, но и умы.")
                .pre("s13").state("techs", 6, "Изучить 6 технологий").reward("gold:90", "xp:80", "rep:keep:15"));
        q(new QuestDef("s15", Line.SIDE, "Под рукой короля", "Капитан Верна", "guard", 15,
                "Одинокие деревни — лёгкая добыча. Возьмите одну под защиту (/regnum village claim).", "Крестьяне будут кормить вашу стражу — и вашу славу.")
                .pre("s2").state("vassals", 1, "Взять под защиту деревню").reward("gold:60", "xp:60", "rep:guard:15"));
        q(new QuestDef("s16", Line.SIDE, "Печати торговли", "Торговка Мирра", "trade", 15,
                "Хочу заключить с вами договор, но мне нужен знак доверия: торговый договор с королевством.", "Я лично прослежу, чтобы вас знали в каждой гавани.")
                .pre("s4").state("trade", 1, "Заключить торговый договор").reward("gold:100", "xp:70", "rep:trade:20"));
        q(new QuestDef("s17", Line.SIDE, "Союз клинков", "Капитан Верна", "guard", 40,
                "Одинокий король — мёртвый король. Найдите союзника среди королевств.", "Два плеча вместо одного.")
                .pre("s15").state("ally", 1, "Заключить союз").reward("gold:150", "xp:100", "rep:guard:25"));
        q(new QuestDef("s18", Line.SIDE, "Именной клинок", "Торговка Мирра", "trade", 40,
                "Легенда, выкованная в вашей кузнице, станет лучшим товаром. Выкуйте именной клинок (/regnum legend).", "Такое оружие переживёт нас всех.")
                .pre("s7").state("legend", 1, "Перековать легендарный клинок").reward("gold:200", "xp:120", "rep:trade:25", "buff:minecraft:strength:1200:0"));
        q(new QuestDef("s19", Line.SIDE, "Форпост на тракте", "Охотник Гаррен", "hunt", 15,
                "Мёртвые идут по дорогам. Назначьте деревне особую цель — аванпост (/regnum village purpose).", "Теперь там есть, кому светить факелом.")
                .pre("s15").state("outpost", 1, "Назначить деревне цель-аванпост").reward("item:iron_ingot:8", "xp:80", "rep:hunt:15"));
        q(new QuestDef("s20", Line.SIDE, "Армия Печати", "Капитан Верна", "guard", 40,
                "Мечи нужны не только против разбойников. Соберите армию из двадцати бойцов.", "Это уже не отряд — это войско.")
                .pre("s10").state("army", 20, "Собрать армию из 20 бойцов").reward("gold:200", "xp:140", "rep:guard:30", "buff:minecraft:resistance:1800:1"));

        // ---------- Задания фракций
        q(new QuestDef("f1", Line.FACTION, "Печать верности", "Хранитель Кассиан", "keep", 40,
                "Для обряда нужно звёздное железо. Те, кому я доверяю, приносят мне четыре слитка.",
                "Ваша клятва принята. Печать ответит вам когда-нибудь.")
                .collect("regnum:star_iron", 4, "Принести 4 звёздного железа").reward("gold:120", "xp:150", "rep:keep:30", "buff:minecraft:luck:1800:1"));
        q(new QuestDef("f2", Line.FACTION, "Клятва стражи", "Капитан Верна", "guard", 40,
                "Стража берёт в союзники лишь проверенных. Сокрушите пятнадцать разбойников.",
                "Вы больше, чем союзник. Вы — щит границы.")
                .kill("regnum:bandit", 15, "Убить 15 разбойников").reward("gold:150", "xp:150", "rep:guard:30", "buff:minecraft:resistance:1800:0"));
        q(new QuestDef("f3", Line.FACTION, "Торговая лицензия", "Торговка Мирра", "trade", 40,
                "Лицензия стоит десять золотых слитков. Докажите, что вам можно доверять деньги.",
                "Теперь у вас право торговать с любым караваном. Не обижайте меня.")
                .collect("minecraft:gold_ingot", 10, "Принести 10 золотых слитков").reward("gold:250", "xp:120", "rep:trade:30"));
        q(new QuestDef("s21", Line.SIDE, "Тихие шахты", "Шахтёр Борин", "hunt", 0,
                "Раньше мы работали в темноте и не боялись. Теперь в забоях скрежещет что-то многоногое. Выжгите хоть несколько тварей — люди вернутся к кирке.",
                "Забой снова звенит. Спасибо — вот вам за труды.")
                .kill("regnum:crawler", 6, "Убить 6 шахтных ползунов").reward("item:iron_ingot:10", "xp:70", "rep:hunt:10"));
        q(new QuestDef("s22", Line.SIDE, "Хитин для кузнеца", "Кузнец Торгрим", "trade", 0,
                "Говорят, пластины с шахтных ползунов крепче закалённой кожи. Принесите мне четыре — сделаю вам броню получше.",
                "Лёгкая, прочная и злая, как сама тварь. Берегите её.")
                .collect("regnum:chitin_plate", 4, "Собрать 4 хитиновые пластины").reward("gold:60", "xp:80", "rep:trade:10", "item:diamond:1"));
        q(new QuestDef("f4", Line.FACTION, "Орден охотников", "Охотник Гаррен", "hunt", 40,
                "Чтобы стать охотником, нужно двадцать пять упокоенных. Начинайте.",
                "Вы — охотник. Идите и не оглядывайтесь.")
                .kill("#undead", 25, "Упокоить 25 мертвецов").reward("item:star_iron:2", "xp:150", "rep:hunt:30", "buff:minecraft:strength:1800:1"));
    }

    // ------------------------------------------------------------------ данные игрока

    private static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(ROOT)) pd.put(ROOT, new CompoundTag());
        CompoundTag r = pd.getCompound(ROOT);
        for (String k : new String[]{"active", "rep", "flags"}) if (!r.contains(k)) r.put(k, new CompoundTag());
        if (!r.contains("done")) r.put("done", new ListTag());
        return r;
    }

    public static boolean done(Player p, String id) {
        for (Tag t : root(p).getList("done", Tag.TAG_STRING)) if (t.getAsString().equals(id)) return true;
        return false;
    }

    public static boolean active(Player p, String id) {
        return root(p).getCompound("active").contains(id);
    }

    public static int rep(Player p, String faction) {
        return root(p).getCompound("rep").getInt(faction);
    }

    public static String repTitle(int rep) {
        int t = 0;
        for (int i = 0; i < REP_TIERS.length; i++) if (rep >= REP_TIERS[i]) t = i;
        return REP_TITLES[t];
    }

    public static boolean flag(Player p, String name) {
        return root(p).getCompound("flags").getBoolean(name);
    }

    public static void setFlag(Player p, String name) {
        root(p).getCompound("flags").putBoolean(name, true);
    }

    public static int[] progress(Player p, QuestDef d) {
        CompoundTag a = root(p).getCompound("active");
        int[] arr = a.getIntArray(d.id);
        if (arr.length != d.objs.size()) arr = new int[d.objs.size()];
        return arr;
    }

    private static void save(Player p, QuestDef d, int[] arr) {
        root(p).getCompound("active").putIntArray(d.id, arr);
    }

    // ------------------------------------------------------------------ жизненный цикл

    public static boolean available(Player p, QuestDef d) {
        if (done(p, d.id) || active(p, d.id)) return false;
        for (String r : d.prereq) if (!done(p, r)) return false;
        return d.faction == null || d.repReq <= 0 || rep(p, d.faction) >= d.repReq;
    }

    public static boolean accept(ServerPlayer p, String id) {
        QuestDef d = DEFS.get(id);
        if (d == null || !available(p, d)) return false;
        save(p, d, new int[d.objs.size()]);
        if (root(p).getString("track").isEmpty() || d.line == Line.MAIN) root(p).putString("track", id);
        p.sendSystemMessage(Text.of("❖ Новое задание: " + d.title, ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Text.of(d.giver + ": ", ChatFormatting.AQUA).append(Text.of("«" + d.intro + "»", ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        for (QuestDef.Obj o : d.objs) Text.info(p, "  ◇ " + o.text());
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        refresh(p, d);
        sendSnapshot(p, false);
        return true;
    }

    public static void abandon(ServerPlayer p, String id) {
        root(p).getCompound("active").remove(id);
    }

    private static boolean objDone(QuestDef.Obj o, int v) {
        return switch (o.kind()) {
            case VISIT, FLAG, BUILD -> v >= 1;
            default -> v >= o.count();
        };
    }

    public static boolean complete(Player p, QuestDef d) {
        int[] pr = progress(p, d);
        for (int i = 0; i < d.objs.size(); i++) if (!objDone(d.objs.get(i), pr[i])) return false;
        return true;
    }

    /** Пересчитать прогресс «наблюдаемых» целей (предметы, город, постройки, структуры) и завершить квест при готовности. */
    public static void refresh(ServerPlayer p, QuestDef d) {
        if (!active(p, d.id)) return;
        int[] pr = progress(p, d);
        for (int i = 0; i < d.objs.size(); i++) {
            QuestDef.Obj o = d.objs.get(i);
            switch (o.kind()) {
                case COLLECT -> pr[i] = Math.min(o.count(), countItem(p, o.target()));
                case CITY_LEVEL -> pr[i] = Math.min(o.count(), bestLevel(p));
                case BUILD -> pr[i] = hasBuilding(p, o.target()) ? 1 : 0;
                case FLAG -> pr[i] = flag(p, o.target()) ? 1 : 0;
                case STATE -> pr[i] = Math.min(o.count(), stateValue(p, o.target()));
                case VISIT -> { if (pr[i] == 0 && visiting(p, o.target())) pr[i] = 1; }
                default -> { }
            }
        }
        save(p, d, pr);
        if (complete(p, d)) finish(p, d);
    }

    private static int bestLevel(Player p) {
        int b = 0;
        if (p.level().getServer() == null) return 0;
        for (City c : KingdomData.get(p.level().getServer()).ownedBy(p.getUUID())) b = Math.max(b, c.level);
        return b;
    }

    private static int stateValue(Player p, String t) {
        if (p.level().getServer() == null) return 0;
        var server = p.level().getServer();
        KingdomData data = KingdomData.get(server);
        if (t.startsWith("tech:")) {
            try {
                return com.alkimor.regnum.kingdom.Science.has(server, p.getUUID(), com.alkimor.regnum.kingdom.Science.Tech.valueOf(t.substring(5))) ? 1 : 0;
            } catch (IllegalArgumentException ex) { return 0; }
        }
        switch (t) {
            case "techs": return com.alkimor.regnum.kingdom.Science.get(server).of(p.getUUID()).done.size();
            case "vassals": { int n = 0; for (var v : data.villages()) if (p.getUUID().equals(v.owner)) n++; return n; }
            case "outpost": { int n = 0; for (var v : data.villages()) if (p.getUUID().equals(v.owner) && v.purpose > 0) n++; return n; }
            case "trade": { for (var r : data.realms()) if (r.trade) return 1; return 0; }
            case "ally": { for (var r : data.realms()) if (r.ally) return 1; return 0; }
            case "legend": { int n = 0; for (City c : data.ownedBy(p.getUUID())) n += c.legends; return n; }
            case "army": { int n = 0; for (City c : data.ownedBy(p.getUUID())) n = Math.max(n, c.armyUsed()); return n; }
            default: return 0;
        }
    }

    private static boolean hasBuilding(Player p, String type) {
        if (p.level().getServer() == null) return false;
        for (City c : KingdomData.get(p.level().getServer()).ownedBy(p.getUUID())) {
            try {
                if (c.count(BuildingType.valueOf(type)) > 0) return true;
            } catch (IllegalArgumentException ignored) { }
        }
        return false;
    }

    private static net.minecraft.world.item.Item item(String id) {
        ResourceLocation rl = id.contains(":") ? ResourceLocation.parse(id) : Regnum.id(id);
        return BuiltInRegistries.ITEM.get(rl);
    }

    private static int countItem(Player p, String id) {
        var it = item(id);
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    private static void takeItem(Player p, String id, int n) {
        var it = item(id);
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) return;
            if (!s.is(it)) continue;
            int t = Math.min(n, s.getCount());
            s.shrink(t);
            n -= t;
        }
    }

    private static boolean visiting(ServerPlayer p, String targets) {
        if (!(p.level() instanceof ServerLevel sl)) return false;
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String t : targets.split("\\|")) {
            Structure st = reg.get(ResourceLocation.parse(t));
            if (st == null) continue;
            if (sl.structureManager().getStructureAt(p.blockPosition(), st).isValid()) return true;
            // «рядом» — в пределах 24 блоков от стартовой точки структуры
            BlockPos q = p.blockPosition();
            for (int dx = -24; dx <= 24; dx += 12)
                for (int dz = -24; dz <= 24; dz += 12)
                    if (sl.isLoaded(q.offset(dx, 0, dz)) && sl.structureManager().getStructureAt(q.offset(dx, 0, dz), st).isValid()) return true;
        }
        return false;
    }

    private static void finish(ServerPlayer p, QuestDef d) {
        root(p).getCompound("active").remove(d.id);
        root(p).getList("done", Tag.TAG_STRING).add(StringTag.valueOf(d.id));
        for (QuestDef.Obj o : d.objs) if (o.kind() == Kind.COLLECT) takeItem(p, o.target(), o.count());
        p.sendSystemMessage(Text.of("✔ Задание выполнено: " + d.title, ChatFormatting.GREEN, ChatFormatting.BOLD));
        p.sendSystemMessage(Text.of(d.giver + ": ", ChatFormatting.AQUA).append(Text.of("«" + d.outro + "»", ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        for (String r : d.rewards) grant(p, r);
        Skills.chronicle(p, "Задание: " + d.title);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);
        if (root(p).getString("track").equals(d.id)) root(p).putString("track", "");
        // следующий главный квест начнётся сам
        autoStart(p);
        sendSnapshot(p, false);
    }

    private static void grant(ServerPlayer p, String spec) {
        String[] a = spec.split(":");
        switch (a[0]) {
            case "xp" -> Skills.data(p).addCharXp(Integer.parseInt(a[1]));
            case "gold" -> {
                City best = null;
                for (City c : KingdomData.get(p.server).ownedBy(p.getUUID())) if (best == null || c.level > best.level) best = c;
                int g = Integer.parseInt(a[1]);
                if (best != null) {
                    best.treasury += g;
                    KingdomData.get(p.server).setDirty();
                    Text.gold(p, "  + " + g + " монет в казну «" + best.name + "»");
                } else {
                    p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, Math.max(1, g / 4)));
                    Text.gold(p, "  + золото (нет города — выдано самородками)");
                }
            }
            case "rep" -> addRep(p, a[1], Integer.parseInt(a[2]));
            case "flag" -> setFlag(p, a[1]);
            case "buff" -> {
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(a[1], a[2]);
                Optional<Holder.Reference<net.minecraft.world.effect.MobEffect>> h = BuiltInRegistries.MOB_EFFECT.getHolder(rl);
                h.ifPresent(e -> p.addEffect(new MobEffectInstance(e, Integer.parseInt(a[3]) * 20, Integer.parseInt(a[4]))));
                Text.gold(p, "  ✦ Благословение: " + a[2] + " на " + Integer.parseInt(a[3]) / 60 + " мин.");
            }
            case "item" -> {
                String id = a.length == 4 ? a[1] + ":" + a[2] : a[1];
                int n = Integer.parseInt(a[a.length - 1]);
                var it = item(id);
                if (it == net.minecraft.world.item.Items.AIR) return;
                ItemStack st = new ItemStack(it, n);
                if (!p.getInventory().add(st)) p.drop(st, false);
                Text.gold(p, "  + " + st.getHoverName().getString() + " ×" + n);
            }
            default -> { }
        }
    }

    public static void addRep(ServerPlayer p, String faction, int delta) {
        CompoundTag r = root(p).getCompound("rep");
        int before = r.getInt(faction);
        int now = before + delta;
        r.putInt(faction, now);
        String name = FACTIONS.getOrDefault(faction, faction);
        Text.gold(p, "  ★ " + name + ": " + (delta >= 0 ? "+" : "") + delta + " (" + repTitle(now) + ")");
        if (!repTitle(before).equals(repTitle(now))) {
            p.sendSystemMessage(Text.of("«" + name + "» теперь видит в вас: " + repTitle(now), ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            Skills.chronicle(p, name + ": " + repTitle(now));
        }
    }

    /** Предлагает доступные побочные квесты (однократно) и запускает главные. */
    public static void autoStart(ServerPlayer p) {
        CompoundTag fl = root(p).getCompound("flags");
        for (QuestDef d : DEFS.values()) {
            if (!available(p, d)) continue;
            if (d.line == Line.MAIN) {
                accept(p, d.id);
                return;
            }
            String key = "offered_" + d.id;
            if (fl.getBoolean(key)) continue;
            fl.putBoolean(key, true);
            p.sendSystemMessage(Text.of("❖ " + d.giver + " ищет помощника: «" + d.title + "» ", ChatFormatting.AQUA)
                    .append(Text.button("Взяться", "/regnum story accept " + d.id, d.intro)));
        }
    }

    /** Странники знают о поручениях: проводник — стража, купцы — торговцы, отшельник — хранители, раненый — охотники. */
    public static void offerFromWanderer(ServerPlayer p, String persona) {
        String faction = switch (persona) {
            case "GUIDE" -> "guard";
            case "MERCHANT", "TRICKSTER" -> "trade";
            case "HERMIT" -> "keep";
            case "WOUNDED" -> "hunt";
            default -> null;
        };
        if (faction == null) return;
        for (QuestDef d : DEFS.values()) {
            if (d.line == Line.MAIN || !faction.equals(d.faction) || !available(p, d)) continue;
            p.sendSystemMessage(Text.of("  Странник слышал о деле: «" + d.title + "» — " + d.giver + " ", ChatFormatting.AQUA)
                    .append(Text.button("Взяться", "/regnum story accept " + d.id, d.intro)));
            return;
        }
    }

    // ------------------------------------------------------------------ события

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 11) return;
        if (p.tickCount < 100) return;
        for (String id : new ArrayList<>(root(p).getCompound("active").getAllKeys())) {
            QuestDef d = DEFS.get(id);
            if (d != null) refresh(p, d);
        }
        if (p.tickCount % 200 == 11) autoStart(p);
        sendSnapshot(p, false);
        String tr = root(p).getString("track");
        if (!tr.isEmpty() && p.tickCount % 200 == 51) {
            QuestDef d = DEFS.get(tr);
            if (d != null && active(p, tr)) Text.bar(p, "❖ " + d.title + ": " + objectiveLine(p, d), ChatFormatting.GOLD);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        var type = e.getEntity().getType();
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        boolean undead = type.is(EntityTypeTags.UNDEAD);
        for (String id : new ArrayList<>(root(p).getCompound("active").getAllKeys())) {
            QuestDef d = DEFS.get(id);
            if (d == null) continue;
            int[] pr = progress(p, d);
            boolean changed = false;
            for (int i = 0; i < d.objs.size(); i++) {
                QuestDef.Obj o = d.objs.get(i);
                if (o.kind() != Kind.KILL || pr[i] >= o.count()) continue;
                boolean match = o.target().equals("#undead") ? undead : o.target().equals(key);
                if (match) {
                    pr[i]++;
                    changed = true;
                }
            }
            if (changed) {
                save(p, d, pr);
                if (complete(p, d)) finish(p, d);
                else Text.bar(p, "❖ " + d.title + ": " + objectiveLine(p, d), ChatFormatting.GOLD);
            }
        }
    }

    // ------------------------------------------------------------------ снимок для клиента

    private static final Map<java.util.UUID, Integer> SENT = new java.util.HashMap<>();

    private static String rewardText(String spec) {
        String[] a = spec.split(":");
        return switch (a[0]) {
            case "xp" -> "Опыт героя +" + a[1];
            case "gold" -> "Казна +" + a[1] + " монет";
            case "rep" -> "Репутация «" + FACTIONS.getOrDefault(a[1], a[1]) + "» +" + a[2];
            case "buff" -> "Благословение: " + a[2] + ", " + Integer.parseInt(a[3]) / 60 + " мин.";
            case "item" -> {
                var it = item(a.length == 4 ? a[1] + ":" + a[2] : a[1]);
                yield new ItemStack(it).getHoverName().getString() + " ×" + a[a.length - 1];
            }
            default -> "";
        };
    }

    private static String lockReason(Player p, QuestDef d) {
        StringBuilder why = new StringBuilder();
        for (String r : d.prereq) if (!done(p, r)) why.append(why.length() > 0 ? ", " : "").append("сначала «").append(DEFS.get(r).title).append("»");
        if (d.faction != null && d.repReq > 0 && rep(p, d.faction) < d.repReq)
            why.append(why.length() > 0 ? ", " : "").append("репутация «").append(FACTIONS.get(d.faction)).append("» ").append(rep(p, d.faction)).append("/").append(d.repReq);
        return why.toString();
    }

    public static com.alkimor.regnum.core.network.QuestSnapshotPayload snapshot(Player p, boolean open) {
        List<com.alkimor.regnum.core.network.QuestSnapshotPayload.Q> qs = new ArrayList<>();
        for (QuestDef d : DEFS.values()) {
            int state = done(p, d.id) ? 3 : active(p, d.id) ? 0 : available(p, d) ? 1 : 2;
            if (state == 2 && d.line == Line.MAIN && !d.prereq.isEmpty() && !done(p, d.prereq.get(d.prereq.size() - 1))) continue; // будущие главы не спойлерим
            int[] pr = progress(p, d);
            List<String> ot = new ArrayList<>();
            List<Integer> op = new ArrayList<>(), orq = new ArrayList<>();
            List<Boolean> od = new ArrayList<>();
            boolean show = state != 2;
            if (show) for (int i = 0; i < d.objs.size(); i++) {
                QuestDef.Obj o = d.objs.get(i);
                boolean ok = state == 3 || objDone(o, pr[i]);
                ot.add(o.text());
                op.add(state == 0 || state == 3 ? (ok ? Math.max(1, o.count()) : pr[i]) : 0);
                orq.add(Math.max(1, o.count()));
                od.add(ok);
            }
            List<String> rw = new ArrayList<>();
            for (String r : d.rewards) { String t = rewardText(r); if (!t.isEmpty()) rw.add(t); }
            qs.add(new com.alkimor.regnum.core.network.QuestSnapshotPayload.Q(d.id, d.line.ordinal(), state, d.title, d.giver,
                    d.faction == null ? "" : d.faction, state == 2 ? "" : d.intro, ot, op, orq, od, rw, state == 2 ? lockReason(p, d) : ""));
        }
        List<String> rf = new ArrayList<>();
        List<Integer> rv = new ArrayList<>();
        for (String f : FACTIONS.keySet()) { rf.add(f); rv.add(rep(p, f)); }
        return new com.alkimor.regnum.core.network.QuestSnapshotPayload(qs, root(p).getString("track"), rf, rv, open);
    }

    /** Отправить снимок клиенту; без open — только если что-то изменилось (для трекера на экране). */
    public static void sendSnapshot(ServerPlayer p, boolean open) {
        if (p.connection == null) return;
        var pl = snapshot(p, open);
        int h = pl.hashCode() * 31 + pl.quests().toString().hashCode();
        if (!open && SENT.getOrDefault(p.getUUID(), 0) == h) return;
        SENT.put(p.getUUID(), h);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, pl);
    }

    // ------------------------------------------------------------------ журнал

    public static String objectiveLine(Player p, QuestDef d) {
        int[] pr = progress(p, d);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.objs.size(); i++) {
            QuestDef.Obj o = d.objs.get(i);
            if (objDone(o, pr[i])) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(o.text());
            if (o.count() > 1) sb.append(" (").append(pr[i]).append("/").append(o.count()).append(")");
        }
        return sb.length() == 0 ? "выполнено" : sb.toString();
    }

    public static List<String> journal(Player p) {
        List<String> out = new ArrayList<>();
        for (Line ln : Line.values()) {
            List<String> lines = new ArrayList<>();
            for (QuestDef d : DEFS.values()) {
                if (d.line != ln) continue;
                if (active(p, d.id)) lines.add("▶ " + d.title + " — " + objectiveLine(p, d));
                else if (available(p, d)) lines.add("◇ " + d.title + " (доступно: /regnum story accept " + d.id + ")");
            }
            if (!lines.isEmpty()) {
                out.add("— " + ln.title + " —");
                out.addAll(lines);
            }
        }
        if (out.isEmpty()) out.add("Заданий пока нет. Живите, исследуйте — люди найдут вас сами.");
        return out;
    }

    /** Ближайшие цели: что делать сейчас и почему закрыто остальное (идея «журнал ближайших целей»). */
    public static List<String> goals(Player p) {
        List<String> out = new ArrayList<>();
        int shown = 0;
        for (QuestDef d : DEFS.values()) {
            if (active(p, d.id) && shown < 3) {
                out.add("▶ " + d.title + ": " + objectiveLine(p, d));
                shown++;
            }
        }
        for (QuestDef d : DEFS.values()) {
            if (shown >= 3) break;
            if (available(p, d)) {
                out.add("◇ " + d.title + " — можно взять (/regnum story accept " + d.id + ")");
                shown++;
            }
        }
        int blocked = 0;
        for (QuestDef d : DEFS.values()) {
            if (blocked >= 3) break;
            if (done(p, d.id) || active(p, d.id) || available(p, d)) continue;
            StringBuilder why = new StringBuilder();
            for (String r : d.prereq) if (!done(p, r)) why.append(why.length() > 0 ? ", " : "").append("сначала «").append(DEFS.get(r).title).append("»");
            if (d.faction != null && d.repReq > 0 && rep(p, d.faction) < d.repReq)
                why.append(why.length() > 0 ? ", " : "").append("репутация «").append(FACTIONS.get(d.faction)).append("» ").append(rep(p, d.faction)).append("/").append(d.repReq);
            out.add("✖ " + d.title + " — закрыто: " + why);
            blocked++;
        }
        if (out.isEmpty()) out.add("Пока целей нет — исследуйте мир, основывайте город, общайтесь с торговцами.");
        return out;
    }

    // ------------------------------------------------------------------ команды

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("story")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Журнал заданий ══");
                    for (String s : journal(p)) Text.info(p, s);
                    return 1;
                })
                .then(Commands.literal("gui").executes(ctx -> {
                    sendSnapshot(ctx.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("accept").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!accept(p, StringArgumentType.getString(ctx, "id"))) { Text.bad(p, "Это задание сейчас недоступно."); return 0; }
                    return 1;
                })))
                .then(Commands.literal("track").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    if (!active(p, id)) { Text.bad(p, "Задание не взято."); return 0; }
                    root(p).putString("track", id);
                    sendSnapshot(p, false);
                    Text.good(p, "Отслеживается: " + DEFS.get(id).title);
                    return 1;
                })))
                .then(Commands.literal("abandon").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(ctx, "id");
                    QuestDef d = DEFS.get(id);
                    if (d == null || d.line == Line.MAIN || !active(p, id)) { Text.bad(p, "Можно отказаться только от побочного задания."); return 0; }
                    abandon(p, id);
                    Text.info(p, "Вы отказались от задания «" + d.title + "».");
                    sendSnapshot(p, false);
                    return 1;
                })))
                .then(Commands.literal("goals").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Ближайшие цели ══");
                    for (String g : goals(p)) Text.info(p, g);
                    return 1;
                }))
                .then(Commands.literal("rep").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Text.gold(p, "══ Отношения ══");
                    for (var f : FACTIONS.entrySet()) Text.info(p, f.getValue() + ": " + repTitle(rep(p, f.getKey())) + " (" + rep(p, f.getKey()) + ")");
                    return 1;
                }))
                .then(Commands.literal("locate").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return locate(p);
                }))));
    }

    private static int locate(ServerPlayer p) {
        String tr = root(p).getString("track");
        QuestDef d = DEFS.get(tr);
        if (d == null || !active(p, tr) || !(p.level() instanceof ServerLevel sl)) { Text.bad(p, "Нет отслеживаемого задания."); return 0; }
        int[] pr = progress(p, d);
        var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (int i = 0; i < d.objs.size(); i++) {
            QuestDef.Obj o = d.objs.get(i);
            if (o.kind() != Kind.VISIT || pr[i] > 0) continue;
            List<Holder<Structure>> hs = new ArrayList<>();
            for (String t : o.target().split("\\|")) reg.getHolder(ResourceLocation.parse(t)).ifPresent(hs::add);
            if (hs.isEmpty()) continue;
            var found = sl.getChunkSource().getGenerator().findNearestMapStructure(sl, HolderSet.direct(hs), p.blockPosition(), 60, false);
            if (found == null) { Text.info(p, "Поблизости ничего не найдено — исследуйте дальше."); return 0; }
            BlockPos b = found.getFirst();
            double dx = b.getX() - p.getX(), dz = b.getZ() - p.getZ();
            String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "востоку" : "западу") : (dz > 0 ? "югу" : "северу");
            Text.gold(p, "Ваш путь лежит к " + dir + ", примерно " + (int) Math.hypot(dx, dz) / 50 * 50 + " блоков.");
            return 1;
        }
        Text.info(p, "У задания нет мест для поиска.");
        return 0;
    }
}
