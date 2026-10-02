package com.alkimor.regnum.core.command;

import com.alkimor.regnum.core.Text;
import com.alkimor.regnum.kingdom.Campaign;
import com.alkimor.regnum.kingdom.City;
import com.alkimor.regnum.kingdom.CityActions;
import com.alkimor.regnum.kingdom.KingdomData;
import com.alkimor.regnum.kingdom.KingdomManager;
import com.alkimor.regnum.survival.Skill;
import com.alkimor.regnum.survival.Skills;
import com.alkimor.regnum.survival.SurvivorData;
import com.alkimor.regnum.wanderers.WandererDialogs;
import com.alkimor.regnum.wanderers.WandererEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/** Команды мода: /regnum ... */
public final class RegnumCommands {
    private RegnumCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("regnum")
                // ответ в диалоге со странником (кликабельные варианты в чате)
                .then(Commands.literal("talk")
                        .then(Commands.argument("entity", IntegerArgumentType.integer())
                                .then(Commands.argument("option", IntegerArgumentType.integer(0, 32))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            Entity e = p.serverLevel().getEntity(IntegerArgumentType.getInteger(ctx, "entity"));
                                            if (e instanceof WandererEntity w) {
                                                WandererDialogs.handle(w, p, IntegerArgumentType.getInteger(ctx, "option"));
                                            }
                                            return 1;
                                        }))))
                // продолжить игру за наследника
                .then(Commands.literal("heir").executes(ctx -> {
                    com.alkimor.regnum.dynasty.Dynasty.continueAsHeir(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                // навыки и честь
                .then(com.alkimor.regnum.kingdom.CommanderCommands.node())
                .then(Commands.literal("quest").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Campaign.check(p);
                    Text.gold(p, Campaign.status(p));
                    return 1;
                }))
                .then(Commands.literal("skills").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SurvivorData data = Skills.data(p);
                    p.sendSystemMessage(Text.of("— Герой: уровень " + data.charLevel() + (data.cls() != null ? ", " + data.cls().title : "") + " —", ChatFormatting.GOLD));
                    for (com.alkimor.regnum.survival.Attr a : com.alkimor.regnum.survival.Attr.values()) {
                        StringBuilder sb = new StringBuilder("  " + a.title + " " + data.attr(a) + ": ");
                        for (Skill s : a.skills()) sb.append(s.title).append(" ").append(data.level(s)).append(" [").append("●".repeat(data.focus(s))).append("]  ");
                        p.sendSystemMessage(Text.of(sb.toString(), a.color));
                    }
                    p.sendSystemMessage(Text.of("  Честь: " + data.honor() + " — «" + SurvivorData.honorTitle(data.honor()) + "»", ChatFormatting.YELLOW));
                    return 1;
                }))
                // создание героя / дневник
                .then(Commands.literal("hero").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.alkimor.regnum.survival.Skills.sync(p, Skills.data(p).created()
                            ? com.alkimor.regnum.core.network.SkillSyncPayload.OPEN_JOURNAL : com.alkimor.regnum.core.network.SkillSyncPayload.OPEN_CREATION);
                    return 1;
                }).then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.setData(com.alkimor.regnum.survival.SurvivalModule.SURVIVOR, new SurvivorData());
                    com.alkimor.regnum.survival.PlayerStats.apply(p);
                    Skills.sync(p, com.alkimor.regnum.core.network.SkillSyncPayload.OPEN_CREATION);
                    return 1;
                })).then(Commands.literal("points").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    SurvivorData hd = Skills.data(p);
                    hd.freeAttr += 3;
                    hd.freeFocus += 6;
                    Skills.sync(p, false);
                    return 1;
                })))
                // список городов
                .then(Commands.literal("cities").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    KingdomData data = KingdomData.get(p.server);
                    List<City> cities = data.ownedBy(p.getUUID());
                    p.sendSystemMessage(Text.of("— Ваше королевство (" + CityActions.rulerTitle(data, p.getUUID()) + ") —", ChatFormatting.GOLD));
                    if (cities.isEmpty()) Text.info(p, "  Городов пока нет. Поставьте ратушу, чтобы основать первый.");
                    for (City c : cities) {
                        p.sendSystemMessage(Text.of("  «" + c.name + "» ур." + c.level + " — " + c.hall.getX() + ", " + c.hall.getY() + ", " + c.hall.getZ()
                                + " | казна " + c.treasury + ", слава " + c.glory + ", армия " + c.soldiers.size(), ChatFormatting.GRAY));
                    }
                    return 1;
                }))
                // отладка: начать набег на ближайший свой город
                .then(Commands.literal("raid").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    City c = KingdomData.get(p.server).nearestOwned(p.getUUID(), p.blockPosition());
                    if (c == null || !KingdomManager.forceRaid(p.server.overworld(), c)) {
                        Text.bad(p, "Нет подходящего города или набег уже идёт.");
                    }
                    return 1;
                }))
                // отладка: выставить уровень навыка
                .then(Commands.literal("setskill").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    for (Skill s : Skill.values()) b.suggest(s.key);
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, Skill.MAX_LEVEL))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            Skill s = Skill.byKey(StringArgumentType.getString(ctx, "skill"));
                                            if (s == null) {
                                                Text.bad(p, "Нет такого навыка.");
                                                return 0;
                                            }
                                            SurvivorData data = Skills.data(p);
                                            int target = Skill.xpFor(IntegerArgumentType.getInteger(ctx, "level"));
                                            data.setXp(s, target);
                                            com.alkimor.regnum.survival.PlayerStats.apply(p);
                                            Skills.sync(p, false);
                                            Text.good(p, s.title + " → " + data.level(s) + " ур.");
                                            return 1;
                                        }))))
        );
    }
}
