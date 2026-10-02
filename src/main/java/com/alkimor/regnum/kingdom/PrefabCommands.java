package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Операторские команды для проверки готовых построек: /regnum prefab place|kit|has. */
public final class PrefabCommands {
    private PrefabCommands() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("regnum").then(Commands.literal("prefab").requires(s -> s.hasPermission(2))
                .then(Commands.literal("place")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> place(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"), 0))
                                .then(Commands.argument("quarter", IntegerArgumentType.integer(0, 3))
                                        .executes(ctx -> place(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"),
                                                IntegerArgumentType.getInteger(ctx, "quarter"))))))
                .then(Commands.literal("kit")
                        .then(Commands.argument("culture", IntegerArgumentType.integer(0, 5))
                                .executes(ctx -> kit(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "culture"), 60))
                                .then(Commands.argument("len", IntegerArgumentType.integer(20, 200))
                                        .executes(ctx -> kit(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "culture"),
                                                IntegerArgumentType.getInteger(ctx, "len"))))))
                .then(Commands.literal("has")
                        .then(Commands.argument("id", StringArgumentType.string()).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(ctx, "id");
                            var sz = Prefab.size(p.serverLevel(), id, net.minecraft.world.level.block.Rotation.NONE);
                            Text.info(p, id + ": " + (Prefab.exists(p.serverLevel(), id) ? "есть " + sz.toShortString() + ", блоков " + Prefab.blocks(p.serverLevel(), id) : "нет"));
                            return 1;
                        })))));
    }

    private static int place(ServerPlayer p, String id, int q) {
        ServerLevel l = p.serverLevel();
        if (!Prefab.exists(l, id)) {
            Text.bad(p, "Нет схемы " + id);
            return 0;
        }
        BlockPos c = p.blockPosition();
        var size = Prefab.size(l, id, net.minecraft.world.level.block.Rotation.NONE);
        int g = Prefab.groundAt(l, c.getX(), c.getZ(), Math.max(1, size.getX() / 2), Math.max(1, size.getZ() / 2));
        Prefab.place(l, id, c.getX(), g, c.getZ(), Prefab.rotFor(q), Blocks.STONE_BRICKS, Integer.MAX_VALUE);
        Text.good(p, "Поставлено: " + id + " " + size.toShortString());
        return 1;
    }

    private static int kit(ServerPlayer p, int culture, int len) {
        ServerLevel l = p.serverLevel();
        BlockPos c = p.blockPosition();
        int n = WallKit.of(culture).showcase(l, c.getX(), c.getZ(), len, true);
        Text.good(p, "Образец стены (" + Culture.byId(culture).title + "): " + n + " кусков, на восток от вас.");
        return 1;
    }
}
