package wtf.uitems.client.feature.module.impl.visual;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import wtf.uitems.client.feature.module.Module;
import wtf.uitems.client.feature.module.ModuleCategory;
import wtf.uitems.event.impl.game.PostGameTickEvent;
import wtf.uitems.event.impl.game.player.interaction.AttackEvent;
import wtf.uitems.event.subscriber.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static wtf.uitems.client.Constants.mc;

public final class AttackEffectsModule extends Module {

    private static final int TRACK_TICKS = 400;

    private final List<TrackedKillTarget> trackedTargets = new ArrayList<>();
    private final List<PigLaunchEffect> activeEffects = new ArrayList<>();
    private int localEntityIdCounter = 2_000_000;

    public AttackEffectsModule() {
        super("Attack Effects", "Kill animation with spinning flying pig and flame burst.", ModuleCategory.VISUAL);
        setEnabled(true);
    }

    @Subscribe
    public void onAttack(final AttackEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        final Entity target = event.getTarget();
        if (!(target instanceof PlayerEntity player) || target == mc.player) {
            return;
        }

        final int expireTick = mc.player.age + TRACK_TICKS;
        trackedTargets.removeIf(t -> t.entityId == player.getId());
        trackedTargets.add(new TrackedKillTarget(player, expireTick));
    }

    @Subscribe
    public void onPostTick(final PostGameTickEvent event) {
        if (mc.player == null || mc.world == null) {
            clearAllEffects();
            return;
        }

        final int nowTick = mc.player.age;

        final Iterator<TrackedKillTarget> trackedIterator = trackedTargets.iterator();
        while (trackedIterator.hasNext()) {
            final TrackedKillTarget tracked = trackedIterator.next();
            final Entity entity = mc.world.getEntityById(tracked.entityId);
            if (entity != null) {
                tracked.lastPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                tracked.missingTicks = 0;
            } else {
                tracked.missingTicks++;
            }

            final boolean expired = nowTick > tracked.expireTick;
            final boolean killedByState = entity instanceof PlayerEntity p && (!p.isAlive() || p.deathTime > 0 || p.getHealth() <= 0F);
            final boolean killedByDisappear = entity == null && tracked.missingTicks >= 2;
            final boolean killed = killedByState || killedByDisappear;
            if (killed) {
                startPigEffect(tracked.lastPos);
                trackedIterator.remove();
            } else if (expired) {
                trackedIterator.remove();
            }
        }

        final Iterator<PigLaunchEffect> effectIterator = activeEffects.iterator();
        while (effectIterator.hasNext()) {
            final PigLaunchEffect effectState = effectIterator.next();
            if (tickEffect(effectState)) {
                effectIterator.remove();
            }
        }

    }

    @Override
    protected void onDisable() {
        clearAllEffects();
    }

    private void startPigEffect(final Vec3d origin) {
        if (mc.world == null) {
            return;
        }

        final PigEntity pig = new PigEntity(EntityType.PIG, mc.world);
        int id = localEntityIdCounter++;
        while (mc.world.getEntityById(id) != null) {
            id = localEntityIdCounter++;
        }
        pig.setId(id);
        pig.setPersistent();
        pig.noClip = true;
        pig.setNoGravity(true);
        pig.setInvulnerable(true);
        final float yaw = (float) (Math.random() * 360.0);
        pig.refreshPositionAndAngles(origin.x, origin.y + 0.1, origin.z, yaw, 0F);
        pig.setYaw(yaw);
        pig.setBodyYaw(pig.getYaw());
        pig.setHeadYaw(pig.getYaw());
        pig.setPitch(0F);
        pig.setSilent(true);
        pig.setCustomNameVisible(false);
        mc.world.addEntity(pig);

        if (activeEffects.size() >= 2) {
            final PigLaunchEffect oldest = activeEffects.remove(0);
            if (oldest.pig != null && !oldest.pig.isRemoved()) {
                oldest.pig.discard();
            }
        }
        activeEffects.add(new PigLaunchEffect(pig, origin, (float) (Math.random() * Math.PI * 2.0)));
    }

    private boolean tickEffect(final PigLaunchEffect effect) {
        if (mc.world == null) {
            return true;
        }

        effect.age++;

        if (!effect.exploded) {
            final float progress = Math.min(1F, effect.age / (float) effect.riseTicks);
            final double y = effect.origin.y + (effect.height * progress);

            final float spin = effect.age * 26F;
            effect.pig.setYaw(spin);
            effect.pig.setBodyYaw(spin);
            effect.pig.setHeadYaw(spin);
            effect.pig.setPos(effect.origin.x, y, effect.origin.z);
            effect.pig.age++;

            final float note = ((effect.age * 0.22F) % 1.0F + 1.0F) % 1.0F;
            mc.particleManager.addParticle(ParticleTypes.NOTE, effect.origin.x, y + 0.85, effect.origin.z, note, 0.0, 0.0);
            if (effect.age % 2 == 0) {
                final double side = (effect.age % 4 == 0) ? 0.22 : -0.22;
                mc.particleManager.addParticle(ParticleTypes.NOTE, effect.origin.x + side, y + 0.8, effect.origin.z, (note + 0.2F) % 1.0F, 0.0, 0.0);
            }

            mc.particleManager.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, effect.pig.getPickBlockStack()), effect.origin.x, y + 0.55, effect.origin.z, 0.0, 0.0, 0.0);

            spawnRisingFlameSpiral(effect.origin, y, progress, effect.phase);

            if (effect.age % 3 == 0) {
                mc.world.playSound(
                        mc.player,
                        effect.origin.x,
                        y,
                        effect.origin.z,
                        SoundEvents.BLOCK_NOTE_BLOCK_HAT,
                        SoundCategory.PLAYERS,
                        0.55F,
                        1.65F + (float) Math.sin(effect.age * 0.16F) * 0.13F,
                        System.nanoTime()
                );
            }

            if (effect.age >= effect.riseTicks) {
                effect.exploded = true;
                effect.explodeTick = effect.age;
                mc.world.playSound(mc.player, effect.origin.x, y, effect.origin.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F, System.nanoTime());
                spawnExplosionFlameBurst(effect.origin.x, y, effect.origin.z);
                effect.pig.discard();
            }
            return false;
        }

        final int postTicks = effect.age - effect.explodeTick;
        if (postTicks <= 10) {
            final double burstY = effect.origin.y + effect.height;
            for (int i = 0; i < 4; i++) {
                final double a = (effect.age * 0.45) + i * (Math.PI / 2);
                mc.particleManager.addParticle(
                        ParticleTypes.FLAME,
                        effect.origin.x + Math.cos(a) * 0.55,
                        burstY + (Math.random() - 0.5) * 0.22,
                        effect.origin.z + Math.sin(a) * 0.55,
                        Math.cos(a) * 0.03,
                        0.035 + Math.random() * 0.02,
                        Math.sin(a) * 0.03
                );
            }
        }

        return postTicks > 10;
    }

    private void spawnRisingFlameSpiral(final Vec3d center, final double y, final float progress, final float phase) {
        final float radius = 0.65F - progress * 0.25F;
        for (int i = 0; i < 3; i++) {
            final double angle = phase + (mc.player != null ? mc.player.age * 0.36 : 0) + i * (Math.PI * 2.0 / 3.0);
            final double px = center.x + Math.cos(angle) * radius;
            final double pz = center.z + Math.sin(angle) * radius;
            mc.particleManager.addParticle(ParticleTypes.FLAME, px, y - 0.2 + i * 0.15, pz, Math.cos(angle) * 0.01, 0.05, Math.sin(angle) * 0.01);
        }
    }

    private void spawnExplosionFlameBurst(final double x, final double y, final double z) {
        for (int i = 0; i < 24; i++) {
            final double theta = Math.random() * (Math.PI * 2.0);
            final double phi = (Math.random() - 0.5) * Math.PI;
            final double speed = 0.12 + Math.random() * 0.15;
            final double vx = Math.cos(theta) * Math.cos(phi) * speed;
            final double vy = Math.sin(phi) * speed + 0.04;
            final double vz = Math.sin(theta) * Math.cos(phi) * speed;
            mc.particleManager.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
        }
    }

    private void clearAllEffects() {
        trackedTargets.clear();

        for (final PigLaunchEffect effect : activeEffects) {
            if (effect.pig != null && !effect.pig.isRemoved()) {
                effect.pig.discard();
            }
        }
        activeEffects.clear();
    }

    private static final class TrackedKillTarget {
        private final int entityId;
        private int expireTick;
        private Vec3d lastPos;
        private int missingTicks;

        private TrackedKillTarget(final PlayerEntity target, final int expireTick) {
            this.entityId = target.getId();
            this.expireTick = expireTick;
            this.lastPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            this.missingTicks = 0;
        }
    }

    private static final class PigLaunchEffect {
        private final PigEntity pig;
        private final Vec3d origin;
        private final float phase;

        private final int riseTicks = 40;
        private final double height = 6.4;

        private int age;
        private boolean exploded;
        private int explodeTick;

        private PigLaunchEffect(final PigEntity pig, final Vec3d origin, final float phase) {
            this.pig = pig;
            this.origin = origin;
            this.phase = phase;
        }
    }
}
