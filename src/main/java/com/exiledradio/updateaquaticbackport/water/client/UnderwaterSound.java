package com.exiledradio.updateaquaticbackport.water.client;

import com.exiledradio.updateaquaticbackport.water.WaterContent;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * 1.14.4's underwater sounds ({@code UnderwaterAmbientSounds}): the loop, which fades in over 2 seconds
 * under water and out twice as fast above it, and the one-off additions, which stop when the head leaves
 * the water. Both are heard everywhere at once (no position), as 1.14.4's "global" sounds.
 */
abstract class UnderwaterSound extends MovingSound {

    protected final EntityPlayerSP player;

    UnderwaterSound(EntityPlayerSP player, SoundEvent event) {
        super(event, SoundCategory.AMBIENT);
        this.player = player;
        this.repeatDelay = 0;
        this.volume = 1.0F;
        this.attenuationType = ISound.AttenuationType.NONE;
    }

    /** The loop: {@code UnderWaterSound}. */
    static final class Loop extends UnderwaterSound {
        private int ticksInWater;

        Loop(EntityPlayerSP player) {
            super(player, WaterContent.UNDERWATER_LOOP);
            this.repeat = true;
        }

        @Override
        public void update() {
            if (!this.player.isDead && this.ticksInWater >= 0 && WaterClient.ambienceOn()) {
                if (WaterClient.eyesInWater(this.player)) {
                    this.ticksInWater++;
                } else {
                    this.ticksInWater -= 2;
                }
                this.ticksInWater = Math.min(this.ticksInWater, 40);
                this.volume = Math.max(0.0F, Math.min(this.ticksInWater / 40.0F, 1.0F));
            } else {
                this.donePlaying = true;
            }
        }
    }

    /** An addition: {@code SubSound}. */
    static final class Addition extends UnderwaterSound {
        Addition(EntityPlayerSP player, SoundEvent event) {
            super(player, event);
            this.repeat = false;
        }

        @Override
        public void update() {
            if (this.player.isDead || !WaterClient.eyesInWater(this.player)) {
                this.donePlaying = true;
            }
        }
    }
}
