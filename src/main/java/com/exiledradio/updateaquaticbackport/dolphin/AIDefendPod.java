package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.monster.EntityGuardian;

/**
 * Neutral with group defense: hit, a dolphin targets its attacker and every dolphin near it that has no
 * target joins in (1.14.4's {@code HurtByTargetGoal(...).setCallsForHelp()}).
 *
 * <p>1.14.4 passes the guardian as a class it never retaliates against; 1.12.2's constructor reads the
 * same argument as classes that are never called for help, so the guardian rule is kept here instead.
 */
final class AIDefendPod extends EntityAIHurtByTarget {

    private final EntityDolphin dolphin;

    AIDefendPod(EntityDolphin dolphin) {
        super(dolphin, true);
        this.dolphin = dolphin;
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase attacker = this.dolphin.getRevengeTarget();
        return !(attacker instanceof EntityGuardian) && super.shouldExecute();
    }
}
