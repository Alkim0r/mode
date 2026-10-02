package com.alkimor.regnum.survival;

import com.alkimor.regnum.kingdom.CultureWeaponItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;

/** Классы оружия для навыков: по предмету в руке или по снаряду. */
public final class Weapons {
    private Weapons() {}

    public enum Kind { ONE_HANDED, TWO_HANDED, POLEARM, BOW, CROSSBOW, THROWING }

    public static Kind melee(ItemStack s) {
        if (s.getItem() instanceof CultureWeaponItem w) {
            return switch (w.kind) {
                case SPEAR, PILUM -> Kind.POLEARM;
                case CLAYMORE, LONG, AXE -> Kind.TWO_HANDED;
                default -> Kind.ONE_HANDED;
            };
        }
        if (s.getItem() instanceof TridentItem) return Kind.POLEARM;
        if (s.getItem() instanceof AxeItem || s.getItem() instanceof MaceItem) return Kind.TWO_HANDED;
        return Kind.ONE_HANDED;
    }

    /** Класс по источнику урона; null — не оружие игрока. */
    public static Kind of(DamageSource src, Player p) {
        Entity direct = src.getDirectEntity();
        if (direct == p) return melee(p.getMainHandItem());
        if (direct instanceof ThrownTrident || direct instanceof com.alkimor.regnum.survival.kit.ThrownWeaponEntity) return Kind.THROWING;
        if (direct instanceof AbstractArrow a) {
            ItemStack w = a.getWeaponItem();
            return w != null && w.getItem() instanceof CrossbowItem ? Kind.CROSSBOW : Kind.BOW;
        }
        if (direct instanceof Projectile) return Kind.THROWING;
        return null;
    }

    public static Skill skill(Kind k) {
        return switch (k) {
            case ONE_HANDED -> Skill.ONE_HANDED;
            case TWO_HANDED -> Skill.TWO_HANDED;
            case POLEARM -> Skill.POLEARM;
            case BOW -> Skill.BOW;
            case CROSSBOW -> Skill.CROSSBOW;
            case THROWING -> Skill.THROWING;
        };
    }

    public static boolean ranged(Kind k) {
        return k == Kind.BOW || k == Kind.CROSSBOW || k == Kind.THROWING;
    }
}
