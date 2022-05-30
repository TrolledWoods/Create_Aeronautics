package com.eriksonn.createaeronautics.mixins;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionMatrices;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static com.eriksonn.createaeronautics.contraptions.SubcontraptionMatrixTransformer.setupTransforms;

@Mixin(value= ContraptionMatrices.class)
public class ContraptionMatriciesMixin {

    @Shadow(remap = false) @Final
    private MatrixStack model;

    @Inject(locals = LocalCapture.CAPTURE_FAILHARD,remap=false,method = "setup", at = @At(remap=false,value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/AbstractContraptionEntity;doLocalTransforms(F[Lcom/mojang/blaze3d/matrix/MatrixStack;)V"))
    private void onSetupTransforms(MatrixStack viewProjection, AbstractContraptionEntity entity, CallbackInfo ci)
    {

        setupTransforms(entity, model);

    }
    //@Inject(locals = LocalCapture.CAPTURE_FAILHARD,remap=false,method = "setup", at = @At("HEAD"))
    //public void onSetupStart(MatrixStack viewProjection, AbstractContraptionEntity entity, CallbackInfo ci)
    //{
    //    if(entity instanceof ControlledContraptionEntity && entity.level.dimension() == AirshipDimensionManager.WORLD_ID)
    //    {
    //        int plotId = AirshipManager.getIdFromPlotPos(entity.blockPosition());
    //        AirshipContraptionEntity airshipEntity = AirshipManager.INSTANCE.AllAirships.get(plotId);
    //        if(airshipEntity!=null) {
    //            BlockPos anchorPos = AirshipManager.getPlotPosFromId(plotId);
    //            viewProjection.popPose();
    //            viewProjection.pushPose();
    //            Vector3d v = airshipEntity .position().add(entity.position()).subtract(new Vector3d(anchorPos.getX(),anchorPos.getY(),anchorPos.getZ()));
    //            viewProjection.translate(v.x,v.y,v.z);
    //        }
    //    }
    //}
    /**
     * @author Eriksonn
     */
    //@Overwrite(remap = false)
    //public static void translateToEntity(Matrix4f matrix, Entity entity, float partialTicks) {
    //    double x = MathHelper.lerp(partialTicks, entity.xOld, entity.getX());
    //    double y = MathHelper.lerp(partialTicks, entity.yOld, entity.getY());
    //    double z = MathHelper.lerp(partialTicks, entity.zOld, entity.getZ());
//
    //    if(entity instanceof ControlledContraptionEntity && entity.level.dimension() == AirshipDimensionManager.WORLD_ID) {
    //        int plotId = AirshipManager.getIdFromPlotPos(entity.blockPosition());
    //        AirshipContraptionEntity airshipEntity = AirshipManager.INSTANCE.AllAirships.get(plotId);
    //        if(airshipEntity!=null) {
    //            BlockPos anchorPos = AirshipManager.getPlotPosFromId(plotId);
    //            //viewProjection.popPose();
    //            //viewProjection.pushPose();
    //            Vector3d v = airshipEntity.position().add(entity.position()).subtract(new Vector3d(anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()));
    //            //viewProjection.translate(v.x-camX,v.y-camY,v.z-camZ);
    //            x = v.x;
    //            y = v.y;
    //            z = v.z;
    //        }
    //    }
//
//
    //    matrix.setTranslation((float) x, (float) y, (float) z);
    //}
}
