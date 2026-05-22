package wtf.uitems.utility.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Predicate;

import static wtf.uitems.client.Constants.mc;

public final class RaycastUtility {

    private RaycastUtility() {
    }

    public static HitResult raycastBlock(final double maxDistance, final float tickDelta, final boolean includeFluids, final float yaw, final float pitch) {
        final Vec3d start = RaycastUtility.getCameraPosVec(tickDelta, mc.player);
        return raycastBlock(maxDistance, includeFluids, yaw, pitch, start);
    }

    public static HitResult raycastBlock(final double maxDistance, final boolean includeFluids, final float yaw, final float pitch, final Vec3d start) {
        final Vec3d rotationVector = RotationUtility.getRotationVector(pitch, yaw);

        final Vec3d end = start.add(rotationVector.x * maxDistance, rotationVector.y * maxDistance, rotationVector.z * maxDistance);

        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static EntityHitResult raycastEntity(final double maxDistance, final float tickDelta, final float yaw, final float pitch, final Predicate<Entity> predicate) {
        return raycastEntity(maxDistance, RaycastUtility.getCameraPosVec(tickDelta, mc.player), yaw, pitch, predicate);
    }

    public static EntityHitResult raycastEntity(final double maxDistance, final Vec3d start, final float yaw, final float pitch, final Predicate<Entity> predicate) {
        final Vec3d rotationVector = RotationUtility.getRotationVector(pitch, yaw);

        final Vec3d end = start.add(rotationVector.x * maxDistance, rotationVector.y * maxDistance, rotationVector.z * maxDistance);

        final Box box = mc.player.getBoundingBox().stretch(rotationVector.multiply(maxDistance)).expand(1, 1, 1);

        return ProjectileUtil.raycast(mc.player, start, end, box, predicate, MathHelper.square(maxDistance));
    }

    public static SimpleRayCastResult rayCastBoxDirectly(
            Vec3d origin,
            float yaw, float pitch,
            Vec3d min,
            Vec3d max
    ) {
        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;

        Vec3d direction = RotationUtility.getRotationVector(
                pitch,
                yaw
        );

        // X
        if (Math.abs(direction.x) < 1e-8) {
            if (origin.x < min.x || origin.x > max.x)
                return new SimpleRayCastResult(false, null);
        } else {
            double inv = 1.0 / direction.x;
            double t1 = (min.x - origin.x) * inv;
            double t2 = (max.x - origin.x) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return new SimpleRayCastResult(false, null);
        }

        // Y
        if (Math.abs(direction.y) < 1e-8) {
            if (origin.y < min.y || origin.y > max.y)
                return new SimpleRayCastResult(false, null);
        } else {
            double inv = 1.0 / direction.y;
            double t1 = (min.y - origin.y) * inv;
            double t2 = (max.y - origin.y) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return new SimpleRayCastResult(false, null);
        }

        // Z
        if (Math.abs(direction.z) < 1e-8) {
            if (origin.z < min.z || origin.z > max.z)
                return new SimpleRayCastResult(false, null);
        } else {
            double inv = 1.0 / direction.z;
            double t1 = (min.z - origin.z) * inv;
            double t2 = (max.z - origin.z) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return new SimpleRayCastResult(false, null);
        }

        Vec3d hit = new Vec3d(
                origin.x + direction.x * tMin,
                origin.y + direction.y * tMin,
                origin.z + direction.z * tMin
        );

        return new SimpleRayCastResult(true, hit);
    }

    public static Vec3d getCameraPosVec(final float tickDelta, final Entity entity) {
        final double x = MathHelper.lerp(tickDelta, entity.lastX, entity.getX());
        final double y = MathHelper.lerp(tickDelta, entity.lastY, entity.getY()) + (double) entity.getStandingEyeHeight();
        final double z = MathHelper.lerp(tickDelta, entity.lastZ, entity.getZ());
        return new Vec3d(x, y, z);
    }


    public record SimpleRayCastResult(boolean hit, Vec3d hitVec) { }
}
