package com.alkimor.regnum.client;

import com.alkimor.regnum.client.screen.CityScreen;
import com.alkimor.regnum.client.screen.CommandScreen;
import com.alkimor.regnum.client.screen.JournalScreen;
import com.alkimor.regnum.core.network.ArmyInfoPayload;
import com.alkimor.regnum.core.network.CityInfoPayload;
import com.alkimor.regnum.core.network.SkillSyncPayload;
import com.alkimor.regnum.survival.Skills;
import net.minecraft.client.Minecraft;

/** Обработка пакетов на клиенте. Загружается только на клиенте. */
public final class ClientAccess {
    private ClientAccess() {}

    public static void onSkillSync(SkillSyncPayload p) {
        Skills.clientXp = p.xp();
        Skills.clientAttrs = p.attrs();
        Skills.clientFocus = p.focus();
        Skills.clientFreeAttr = p.freeAttr();
        Skills.clientFreeFocus = p.freeFocus();
        Skills.clientCharXp = p.charXp();
        java.util.EnumSet<com.alkimor.regnum.survival.Perk> perks = java.util.EnumSet.noneOf(com.alkimor.regnum.survival.Perk.class);
        for (String s : p.perks()) {
            var pk = com.alkimor.regnum.survival.Perk.byName(s);
            if (pk != null) perks.add(pk);
        }
        Skills.clientPerks = perks;
        java.util.EnumSet<com.alkimor.regnum.survival.Trait> traits = java.util.EnumSet.noneOf(com.alkimor.regnum.survival.Trait.class);
        for (String s : p.traits()) {
            var t = com.alkimor.regnum.survival.Trait.byName(s);
            if (t != null) traits.add(t);
        }
        Skills.clientTraits = traits;
        Skills.clientClass = p.cls();
        Skills.clientHonor = p.honor();
        Skills.clientChronicle = p.chronicle();
        Skills.clientFamily = p.family();
        var mc = Minecraft.getInstance();
        if (p.open() == SkillSyncPayload.OPEN_JOURNAL) mc.setScreen(new JournalScreen());
        else if (p.open() == SkillSyncPayload.OPEN_CREATION) mc.setScreen(new com.alkimor.regnum.client.screen.CreationScreen());
        else if (mc.screen instanceof JournalScreen js) js.refresh();
    }

    public static void onCityInfo(CityInfoPayload p) {
        Minecraft.getInstance().setScreen(new CityScreen(p));
    }

    public static void onTradeInfo(com.alkimor.regnum.core.network.TradeInfoPayload p) {
        Minecraft.getInstance().setScreen(new com.alkimor.regnum.client.screen.TradeScreen(p));
    }

    public static void onArmyInfo(ArmyInfoPayload p) {
        Minecraft.getInstance().setScreen(new CommandScreen(p));
    }

    public static void onScienceInfo(com.alkimor.regnum.core.network.ScienceInfoPayload p) {
        com.alkimor.regnum.client.screen.ScienceScreen.open(p);
    }

    public static void onCommanders(com.alkimor.regnum.core.network.CommandersPayload p) {
        com.alkimor.regnum.client.screen.CommandersScreen.open(p);
    }
}
