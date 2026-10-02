package com.alkimor.regnum.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

/** Короткие помощники для сообщений в чат. */
public final class Text {
    private Text() {}

    public static MutableComponent of(String s, ChatFormatting... f) {
        return Component.literal(s).withStyle(f);
    }

    public static void info(Player p, String s) {
        p.sendSystemMessage(of(s, ChatFormatting.GRAY));
    }

    public static void good(Player p, String s) {
        p.sendSystemMessage(of(s, ChatFormatting.GREEN));
    }

    public static void bad(Player p, String s) {
        p.sendSystemMessage(of(s, ChatFormatting.RED));
    }

    public static void gold(Player p, String s) {
        p.sendSystemMessage(of(s, ChatFormatting.GOLD));
    }

    /** Подсказка над хотбаром. */
    public static void bar(Player p, String s, ChatFormatting f) {
        p.displayClientMessage(of(s, f), true);
    }

    /** Кликабельный вариант ответа, который выполняет команду. */
    public static MutableComponent button(String label, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }
}
