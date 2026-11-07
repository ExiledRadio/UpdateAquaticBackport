package com.exiledradio.updateaquaticbackport.trident;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * Loyalty, Riptide, Impaling and Channeling: the four trident-only enchantments.
 *
 * <p>Levels, rarities and enchanting costs are 1.14.4's. Their effects live where they happen: Loyalty
 * and Channeling in {@link EntityTrident}, Riptide in {@link ItemTrident} and {@link Riptide}, Impaling in
 * {@link Impaling} (1.12.2 has no aquatic creature attribute, so the bonus cannot go through
 * {@code calcDamageByCreature}).
 */
public final class EnchantmentTrident extends Enchantment {

    public enum Kind {
        LOYALTY(Rarity.UNCOMMON, 3),
        RIPTIDE(Rarity.RARE, 3),
        IMPALING(Rarity.RARE, 5),
        CHANNELING(Rarity.VERY_RARE, 1);

        final Rarity rarity;
        final int maxLevel;

        Kind(Rarity rarity, int maxLevel) {
            this.rarity = rarity;
            this.maxLevel = maxLevel;
        }
    }

    public final Kind kind;

    EnchantmentTrident(Kind kind, EnumEnchantmentType type) {
        super(kind.rarity, type, new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        this.kind = kind;
    }

    @Override
    public int getMaxLevel() {
        return this.kind.maxLevel;
    }

    @Override
    public int getMinEnchantability(int level) {
        switch (this.kind) {
            case LOYALTY:
                return 5 + level * 7;
            case RIPTIDE:
                return 10 + level * 7;
            case IMPALING:
                return 1 + (level - 1) * 8;
            default:
                return 25;
        }
    }

    @Override
    public int getMaxEnchantability(int level) {
        return this.kind == Kind.IMPALING ? this.getMinEnchantability(level) + 20 : 50;
    }

    /** Riptide goes with neither Loyalty nor Channeling (checked both ways by vanilla). */
    @Override
    protected boolean canApplyTogether(Enchantment other) {
        if (!super.canApplyTogether(other)) {
            return false;
        }
        if (this.kind == Kind.RIPTIDE && other instanceof EnchantmentTrident) {
            Kind k = ((EnchantmentTrident) other).kind;
            return k != Kind.LOYALTY && k != Kind.CHANNELING;
        }
        return true;
    }
}
