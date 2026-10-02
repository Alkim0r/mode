package com.alkimor.regnum.survival;

import com.alkimor.regnum.core.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Медицинские предметы. Время применения и сила зависят от навыка «Медицина». */
public class MedicalItem extends Item {
    public enum Kind {
        BANDAGE(50, "Останавливает кровотечение, немного лечит. ПКМ по союзнику — перевязать его."),
        SPLINT(80, "Фиксирует перелом, после чего он срастается."),
        DECOCTION(32, "Борется с инфекцией. Шанс излечения растёт с Медициной."),
        MEDKIT(70, "Снимает все травмы и хорошо лечит."),
        ANTIDOTE(40, "Гарантированно излечивает инфекцию, яд и иссушение.");

        final int baseTicks;
        final String hint;

        Kind(int baseTicks, String hint) {
            this.baseTicks = baseTicks;
            this.hint = hint;
        }
    }

    private final Kind kind;

    public MedicalItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int lvl = entity instanceof Player p ? Skills.level(p, Skill.MEDICINE) : 0;
        return Math.max(10, Math.round(kind.baseTicks * (1.0f - 0.006f * lvl)));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.DECOCTION || kind == Kind.ANTIDOTE ? UseAnim.DRINK : UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isUseful(player)) {
            if (!level.isClientSide) Text.bar(player, "Сейчас это лечение вам не нужно", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private boolean isUseful(Player p) {
        return switch (kind) {
            case BANDAGE -> p.hasEffect(SurvivalModule.BLEEDING) || p.getHealth() < p.getMaxHealth();
            case SPLINT -> p.hasEffect(SurvivalModule.FRACTURE);
            case DECOCTION -> p.hasEffect(SurvivalModule.INFECTION) || p.getHealth() < p.getMaxHealth();
            case MEDKIT -> true;
            case ANTIDOTE -> p.hasEffect(SurvivalModule.INFECTION) || p.hasEffect(MobEffects.POISON) || p.hasEffect(MobEffects.WITHER);
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer p)) return stack;
        int lvl = Skills.level(p, Skill.MEDICINE);
        float f = Skills.medicineFactor(p);
        boolean treated = false;
        switch (kind) {
            case BANDAGE -> {
                if (p.hasEffect(SurvivalModule.BLEEDING)) {
                    p.removeEffect(SurvivalModule.BLEEDING);
                    treated = true;
                    Text.good(p, "Рана перевязана, кровотечение остановлено.");
                }
                p.heal((1.5f + (Skills.has(p, Perk.MD_BANDAGE) ? 2f : 0f)) * f);
            }
            case SPLINT -> {
                if (Injuries.splint(p, lvl)) {
                    treated = true;
                    Text.good(p, "Шина наложена. Кость срастётся, если не нагружать ногу.");
                }
            }
            case DECOCTION -> {
                MobEffectInstance inf = p.getEffect(SurvivalModule.INFECTION);
                if (inf != null) {
                    treated = true;
                    float chance = 0.55f + 0.005f * lvl - 0.15f * inf.getAmplifier();
                    p.removeEffect(SurvivalModule.INFECTION);
                    if (p.getRandom().nextFloat() < chance) {
                        Text.good(p, "Жар спадает. Инфекция отступила.");
                    } else if (inf.getAmplifier() > 0) {
                        p.addEffect(new MobEffectInstance(SurvivalModule.INFECTION, Injuries.INFECTION_STAGE_TICKS, inf.getAmplifier() - 1));
                        Text.info(p, "Стало немного легче, но болезнь ещё не ушла.");
                    } else {
                        Text.good(p, "Отвар помог — инфекция побеждена.");
                    }
                }
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (80 * f), Skills.has(p, Perk.MD_HERBAL) ? 1 : 0));
            }
            case ANTIDOTE -> {
                treated = true;
                p.removeEffect(SurvivalModule.INFECTION);
                p.removeEffect(MobEffects.POISON);
                p.removeEffect(MobEffects.WITHER);
                Text.good(p, "Противоядие подействовало: яд и зараза покидают тело.");
            }
            case MEDKIT -> {
                treated = p.hasEffect(SurvivalModule.BLEEDING) || p.hasEffect(SurvivalModule.FRACTURE) || p.hasEffect(SurvivalModule.INFECTION);
                p.removeEffect(SurvivalModule.BLEEDING);
                p.removeEffect(SurvivalModule.FRACTURE);
                p.removeEffect(SurvivalModule.INFECTION);
                p.heal(6.0f * f);
                Text.good(p, "Аптечка использована: все травмы обработаны.");
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        Skills.addXp(p, Skill.MEDICINE, treated ? 12 : 3);
        boolean saved = Skills.has(p, Perk.MD_THRIFT) && p.getRandom().nextFloat() < 0.3f;
        if (saved) Text.bar(p, "Вы сэкономили медикамент", ChatFormatting.LIGHT_PURPLE);
        if (!p.getAbilities().instabuild && !saved) stack.shrink(1);
        return stack;
    }

    /** Перевязка союзника (игрока, солдата, жителя). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.BANDAGE && kind != Kind.MEDKIT) return InteractionResult.PASS;
        if (target.getHealth() >= target.getMaxHealth() && !target.hasEffect(SurvivalModule.BLEEDING)) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            float f = Skills.medicineFactor(player);
            if (target instanceof com.alkimor.regnum.kingdom.SoldierEntity && Skills.has(player, Perk.MD_FIELD)) f *= 2f;
            target.removeEffect(SurvivalModule.BLEEDING);
            target.heal((kind == Kind.MEDKIT ? 10f : 4f) * f);
            if (kind == Kind.MEDKIT) {
                target.removeEffect(SurvivalModule.FRACTURE);
                target.removeEffect(SurvivalModule.INFECTION);
            }
            player.level().playSound(null, target.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
            Text.bar(player, "Вы перевязали: " + target.getName().getString(), ChatFormatting.GREEN);
            Skills.addXp(player, Skill.MEDICINE, 8);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(kind.hint).withStyle(ChatFormatting.GRAY));
    }
}
