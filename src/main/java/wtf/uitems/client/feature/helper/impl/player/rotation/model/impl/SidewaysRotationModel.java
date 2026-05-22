package wtf.uitems.client.feature.helper.impl.player.rotation.model.impl;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.EnumRotationModel;
import wtf.uitems.client.feature.helper.impl.player.rotation.model.IRotationModel;
import wtf.uitems.utility.player.RotationUtility;

public class SidewaysRotationModel implements IRotationModel {

    private final float speed;

    public SidewaysRotationModel(float speed) {
        this.speed = speed;
    }

    @Override
    public Vec2f tick(Vec2f from, Vec2f to, float timeDelta) {
        float targetYaw = to.x;
        float targetPitch = to.y;
        float lastYaw = from.x;
        float lastPitch = from.y;

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float deltaPitch = targetPitch - lastPitch;

        double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
        if (distance <= 1.0E-6D) {
            return from;
        }

        double distributionYaw = Math.abs(deltaYaw / distance);
        double distributionPitch = Math.abs(deltaPitch / distance);

        double maxYaw = speed * distributionYaw;
        double maxPitch = speed * distributionPitch;

        float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
        float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

        float newYaw = lastYaw + moveYaw;
        float newPitch = lastPitch + movePitch;

        Vec2f rotation = new Vec2f(newYaw, MathHelper.clamp(newPitch, -90, 90));
        return RotationUtility.patchConstantRotation(rotation, from);
    }

    @Override
    public EnumRotationModel getEnum() {
        return EnumRotationModel.SIDEWAYS;
    }
}
