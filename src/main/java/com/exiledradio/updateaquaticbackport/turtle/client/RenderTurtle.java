package com.exiledradio.updateaquaticbackport.turtle.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.turtle.EntityTurtle;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code TurtleRenderer}: shadow 0.7, half that for a baby. */
@SideOnly(Side.CLIENT)
public class RenderTurtle extends RenderLiving<EntityTurtle> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(UpdateAquaticBackport.MOD_ID, "textures/entity/turtle/big_sea_turtle.png");

    public RenderTurtle(RenderManager manager) {
        super(manager, new ModelTurtle(0.0F), 0.7F);
    }

    @Override
    public void doRender(EntityTurtle entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 1.14 halves the shadow on every frame a baby is drawn, shrinking it for good; set it each time instead.
        this.shadowSize = entity.isChild() ? 0.35F : 0.7F;
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTurtle entity) {
        return TEXTURE;
    }
}
