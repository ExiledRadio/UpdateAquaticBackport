package com.exiledradio.updateaquaticbackport.turtle.client;

import com.exiledradio.updateaquaticbackport.babies.client.BabyModels;
import com.exiledradio.updateaquaticbackport.babies.client.model.BabyTurtleModel;
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
    private static final ResourceLocation BABY_TEXTURE =
            new ResourceLocation(UpdateAquaticBackport.MOD_ID, "textures/entity/turtle/turtle_baby.png");

    private final ModelTurtle adultModel;
    private final BabyTurtleModel babyModel = new BabyTurtleModel();

    public RenderTurtle(RenderManager manager) {
        super(manager, new ModelTurtle(0.0F), 0.7F);
        this.adultModel = (ModelTurtle) this.mainModel;
    }

    /** @return true when this turtle draws 26.1's baby model (while Game Drops Backport's babies are on) */
    static boolean newBaby(EntityTurtle turtle) {
        return turtle.isChild() && BabyModels.active();
    }

    @Override
    public void doRender(EntityTurtle entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 1.14 halves the shadow on every frame a baby is drawn, shrinking it for good; set it each time instead.
        this.shadowSize = entity.isChild() ? 0.35F : 0.7F;
        boolean baby = newBaby(entity);
        this.mainModel = baby ? this.babyModel : this.adultModel;
        if (baby) {
            // 26.3's TurtleRenderer: the baby's shadow is 0.83 of the adult's, which 1.12 then halves for a child.
            this.shadowSize = 0.7F * 0.83F;
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTurtle entity) {
        return newBaby(entity) ? BABY_TEXTURE : TEXTURE;
    }
}
