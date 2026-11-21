package com.exiledradio.updateaquaticbackport.babies.client.model;

import com.exiledradio.updateaquaticbackport.turtle.EntityTurtle;

/** 26.3's {@code BabyTurtleModel} with {@code TurtleModel.setupAnim} (laying an egg is 1.13's digging). */
public final class BabyTurtleModel extends BabyModel<EntityTurtle> {

    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public BabyTurtleModel() {
        super(createBodyLayer());
        head = root.getChild("head");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 22.9F, 1.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 6).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 22.9F, -1.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(-1, 0).addBox(-2.0F, 0.0F, -0.5F, 2.0F, 0.0F, 1.0F),
                PartPose.offset(-2.0F, 23.9F, 2.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(-1, 1).addBox(0.0F, 0.0F, -0.5F, 2.0F, 0.0F, 1.0F),
                PartPose.offset(2.0F, 23.9F, 2.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(8, 6).addBox(-2.0F, 0.0F, -0.5F, 2.0F, 0.0F, 1.0F),
                PartPose.offset(-2.0F, 23.9F, -0.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(8, 7).addBox(0.0F, 0.0F, -0.5F, 2.0F, 0.0F, 1.0F),
                PartPose.offset(2.0F, 23.9F, -0.5F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    protected void setupAnim(EntityTurtle turtle, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        quadruped(head, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, pos, speed, yRot, xRot);
        if (!turtle.isInWater() && turtle.onGround) {
            boolean layingEgg = turtle.isDigging();
            float layEgg = layingEgg ? 4.0F : 1.0F;
            float amplitude = layingEgg ? 2.0F : 1.0F;
            float swingPos = pos * 5.0F;
            float frontSwing = cos(layEgg * swingPos);
            float hindSwing = cos(swingPos);
            rightFrontLeg.yRot = -frontSwing * 8.0F * speed * amplitude;
            leftFrontLeg.yRot = frontSwing * 8.0F * speed * amplitude;
            rightHindLeg.yRot = -hindSwing * 3.0F * speed;
            leftHindLeg.yRot = hindSwing * 3.0F * speed;
        } else {
            float swing = cos(pos * 0.6662F * 0.6F) * 0.5F * speed;
            rightHindLeg.xRot = swing;
            leftHindLeg.xRot = -swing;
            rightFrontLeg.zRot = -swing;
            leftFrontLeg.zRot = swing;
        }
    }
}
