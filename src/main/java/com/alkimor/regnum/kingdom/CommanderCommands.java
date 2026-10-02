package com.alkimor.regnum.kingdom;

import com.alkimor.regnum.core.Text;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** /regnum commander ... — назначение военачальников (друзей-игроков) на роды войск. */
public final class CommanderCommands {
    private CommanderCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("commander")
                .then(Commands.literal("appoint")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("branch", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(Commissions.Branch.values()).map(x -> x.name().toLowerCase()), b))
                                        .executes(ctx -> appoint(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "branch"))))))
                .then(Commands.literal("assign")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> assign(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), 999))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> assign(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("release")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> release(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), false))))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> release(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), true))))
                .then(Commands.literal("gui").executes(ctx -> sendGui(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("rally").executes(ctx -> rally(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("me").executes(ctx -> me(ctx.getSource().getPlayerOrException())));
    }

    private static int appoint(ServerPlayer king, ServerPlayer target, String branch) {
        Commissions.Commission ex = Commissions.get(king.server).of(target.getUUID());
        if (ex != null && !ex.king.equals(king.getUUID())) {
            Text.bad(king, target.getName().getString() + " уже служит другому королю.");
            return 0;
        }
        if (KingdomData.get(king.server).ownedBy(king.getUUID()).isEmpty()) {
            Text.bad(king, "Назначать военачальников может только правитель города.");
            return 0;
        }
        if (target == king) {
            Text.bad(king, "Король и так главнокомандующий.");
            return 0;
        }
        Commissions.Branch b = Commissions.Branch.byName(branch);
        if (b == null) {
            Text.bad(king, "Неизвестный род войск. Доступно: пехота (infantry), конница (cavalry), стрелки (archers), воздух (air), осада (siege).");
            return 0;
        }
        Commissions.Commission c = Commissions.get(king.server).appoint(king.getUUID(), target.getUUID(), b);
        Text.gold(king, target.getName().getString() + " назначен военачальником: " + b.title + " (" + c.rank() + ", лимит " + c.cap() + " бойцов). "
                + "Передайте ему бойцов: /regnum commander assign " + target.getName().getString());
        Text.gold(target, "Король " + king.getName().getString() + " назначил вас военачальником рода войск «" + b.title + "»! Звание: " + c.rank()
                + ". Берите жезл командира: ваши бойцы слушаются ваших приказов, а побеждая, вы растёте в авторитете.");
        ItemStack baton = new ItemStack(KingdomModule.COMMANDER_BATON.get());
        if (!target.getInventory().add(baton)) target.drop(baton, false);
        return 1;
    }

    private static List<SoldierEntity> units(ServerPlayer king, java.util.function.Predicate<SoldierEntity> f) {
        List<SoldierEntity> l = new ArrayList<>(king.serverLevel().getEntities(KingdomModule.SOLDIER.get(), s -> s.isAlive() && f.test(s)));
        l.sort(Comparator.comparingDouble(s -> s.distanceToSqr(king)));
        return l;
    }

    private static int assign(ServerPlayer king, ServerPlayer target, int count) {
        Commissions cm = Commissions.get(king.server);
        Commissions.Commission c = cm.of(target.getUUID());
        if (c == null || !c.king.equals(king.getUUID())) {
            Text.bad(king, "Сначала назначьте игрока военачальником: /regnum commander appoint");
            return 0;
        }
        int have = units(king, s -> target.getUUID().equals(s.getCommander())).size();
        int room = c.cap() - have;
        if (room <= 0) {
            Text.bad(king, "У военачальника уже " + have + " бойцов — предел его звания (" + c.rank() + ", " + c.cap() + "). Победы повышают звание.");
            return 0;
        }
        int moved = 0;
        for (SoldierEntity s : units(king, s -> king.getUUID().equals(s.getOwnerId()) && s.getCommander() == null && c.branch.accepts(s.getSoldierType()))) {
            if (moved >= Math.min(count, room)) break;
            s.setCommander(target.getUUID());
            moved++;
        }
        Text.good(king, "Передано бойцов: " + moved + " (" + c.branch.title + ") → " + target.getName().getString() + ".");
        if (moved > 0) Text.good(target, "Вам переданы бойцы: " + moved + ". Теперь под вашим началом " + (have + moved) + "/" + c.cap() + ".");
        return moved;
    }

    private static int release(ServerPlayer king, ServerPlayer target, boolean dismiss) {
        Commissions cm = Commissions.get(king.server);
        Commissions.Commission c = cm.of(target.getUUID());
        if (c == null || !c.king.equals(king.getUUID())) {
            Text.bad(king, "Этот игрок не ваш военачальник.");
            return 0;
        }
        int n = 0;
        for (SoldierEntity s : units(king, s -> target.getUUID().equals(s.getCommander()))) {
            s.setCommander(null);
            n++;
        }
        if (dismiss) cm.dismiss(target.getUUID());
        Text.good(king, "Бойцов возвращено под ваше командование: " + n + (dismiss ? "; " + target.getName().getString() + " снят с должности." : "."));
        Text.info(target, dismiss ? "Вы сняты с должности военачальника." : "Ваши бойцы возвращены королю.");
        return n;
    }

    /** Данные для экрана военачальников: для короля — все его командиры, для командира — он сам. */
    public static int sendGui(ServerPlayer p) {
        Commissions cm = Commissions.get(p.server);
        List<String> names = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        boolean isKing = !KingdomData.get(p.server).ownedBy(p.getUUID()).isEmpty();
        for (Commissions.Commission c : cm.all()) {
            boolean mine = c.king.equals(p.getUUID());
            if (!mine && !c.commander.equals(p.getUUID())) continue;
            ServerPlayer cp = p.server.getPlayerList().getPlayer(c.commander);
            names.add(cp != null ? cp.getName().getString() : "(не в сети)");
            int n = units(p, s -> c.commander.equals(s.getCommander())).size();
            data.add(c.branch.ordinal());
            data.add(c.tier());
            data.add(c.rep);
            data.add(c.victories);
            data.add(c.cap());
            data.add(n);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.alkimor.regnum.core.network.CommandersPayload(
                names.toArray(new String[0]), data.stream().mapToInt(Integer::intValue).toArray(), isKing));
        return 1;
    }

    private static int list(ServerPlayer king) {
        Commissions cm = Commissions.get(king.server);
        boolean any = false;
        king.sendSystemMessage(Text.of("— Военачальники королевства —", ChatFormatting.GOLD));
        for (Commissions.Commission c : cm.all()) {
            if (!c.king.equals(king.getUUID())) continue;
            any = true;
            ServerPlayer p = king.server.getPlayerList().getPlayer(c.commander);
            int n = units(king, s -> c.commander.equals(s.getCommander())).size();
            king.sendSystemMessage(Text.of("  " + (p != null ? p.getName().getString() : "(не в сети)") + " — " + c.branch.title + ", " + c.rank()
                    + " (авторитет " + c.rep + "), бойцов " + n + "/" + c.cap() + ", побед " + c.victories, ChatFormatting.GRAY));
        }
        if (!any) Text.info(king, "  Никого нет. /regnum commander appoint <игрок> <род войск>");
        return 1;
    }

    private static final java.util.Map<java.util.UUID, Long> RALLY_CD = new java.util.HashMap<>();

    /** Клич военачальника: бойцы в 40 блоках получают дух и силы. Перезарядка 5 мин минус 30 с за звание. */
    public static int rally(ServerPlayer p) {
        Commissions.Commission c = Commissions.get(p.server).of(p.getUUID());
        if (c == null) {
            Text.info(p, "Клич доступен военачальникам.");
            return 0;
        }
        long now = p.serverLevel().getGameTime();
        long cd = Math.max(1200, 6000 - c.tier() * 600L);
        Long last = RALLY_CD.get(p.getUUID());
        if (last != null && last > now) last = null;
        if (last != null && now - last < cd) {
            Text.bad(p, "Горн ещё не остыл: " + (cd - (now - last)) / 20 + " с.");
            return 0;
        }
        RALLY_CD.put(p.getUUID(), now);
        int n = 0;
        for (SoldierEntity s : p.serverLevel().getEntitiesOfClass(SoldierEntity.class, p.getBoundingBox().inflate(40),
                s -> s.isAlive() && p.getUUID().equals(s.getCommander()))) {
            s.addMorale(25f + 4f * c.tier());
            s.heal(s.getMaxHealth() * (0.15f + 0.03f * c.tier()));
            s.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 0, true, true));
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1.8, s.getZ(), 4, 0.3, 0.2, 0.3, 0);
            n++;
        }
        p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.RAID_HORN.value(), net.minecraft.sounds.SoundSource.PLAYERS, 3f, 1f);
        Text.gold(p, "Клич «" + c.rank() + "»! Воодушевлено бойцов: " + n + ".");
        return 1;
    }

    private static int me(ServerPlayer p) {
        Commissions.Commission c = Commissions.get(p.server).of(p.getUUID());
        if (c == null) {
            Text.info(p, "Вы не военачальник.");
            return 0;
        }
        int next = c.tier() + 1 < Commissions.REP_TIERS.length ? Commissions.REP_TIERS[c.tier() + 1] : -1;
        int n = units(p, s -> p.getUUID().equals(s.getCommander())).size();
        Text.gold(p, c.branch.title + ": " + c.rank() + ", авторитет " + c.rep + (next > 0 ? "/" + next : " (максимум)")
                + ", бойцов " + n + "/" + c.cap() + ", дух отряда +" + (int) c.moraleBonus());
        return 1;
    }
}
