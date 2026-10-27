package com.psipheron.nudge;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class AimAssistLogic {

    public static Player findBestTarget(Minecraft client, double maxRange, double fovThreshold) {
        if (client.player == null || client.level == null) return null;

        Vec3 eyePos = client.player.getEyePosition(1.0F);
        Vec3 lookVec = client.player.getViewVector(1.0F);

        Player bestTarget = null;
        double smallestAngle = Double.MAX_VALUE;

        for (Player other : client.level.players()) {
            if (other == client.player || !other.isAlive() || other.isInvisible()) continue;

            double dist = eyePos.distanceTo(other.position());
            if (dist > maxRange) continue;

            Vec3 toTarget = other.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = Mth.clamp(lookVec.dot(toTarget), -1.0, 1.0);
            double angleDeg = Math.toDegrees(Math.acos(dot));

            if (angleDeg < fovThreshold && angleDeg < smallestAngle) {
                smallestAngle = angleDeg;
                bestTarget = other;
            }
        }
        return bestTarget;
    }

    public static Vec3 getRandomHitboxPoint(AABB box) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Find the absolute center of the hitbox
        double centerX = box.minX + (box.maxX - box.minX) * 0.5;
        double centerZ = box.minZ + (box.maxZ - box.minZ) * 0.5;

        // Use a Gaussian curve scaled to the player's width (typically 0.6 blocks)
        // A standard deviation of width * 0.2 keeps most points comfortably inside center mass
        double widthX = box.maxX - box.minX;
        double widthZ = box.maxZ - box.minZ;

        double xOffset = rng.nextGaussian() * (widthX * 0.2);
        double zOffset = rng.nextGaussian() * (widthZ * 0.2);

        // Clamp to ensure extreme outliers don't aim entirely off the character model
        double x = Mth.clamp(centerX + xOffset, box.minX + 0.05, box.maxX - 0.05);
        double z = Mth.clamp(centerZ + zOffset, box.minZ + 0.05, box.maxZ - 0.05);

        // Keep your excellent vertical Gaussian logic
        double height = box.maxY - box.minY;
        double midY = box.minY + height * 0.55;
        double yOffset = rng.nextGaussian() * (height * 0.15);
        double y = Mth.clamp(midY + yOffset, box.minY + 0.1, box.maxY - 0.1);

        return new Vec3(x, y, z);
    }

    public static float quantizeToMouseGrid(Minecraft client, float desiredDelta) {
        if (client.options == null) return desiredDelta;

        // Get the player's current in-game sensitivity (0.0 to 1.0)
        double sensitivity = client.options.sensitivity().get();

        // Minecraft's internal multiplier calculation
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 8.0 * 0.15;

        // Convert the desired angle change into raw mouse "ticks" (integers)
        long mouseTicks = Math.round((double) desiredDelta / gcd);

        // Re-multiply back to find the exact legal angle closest to your target
        return (float) (mouseTicks * gcd);
    }

    public static float[] calculateRotations(Vec3 eyePos, Vec3 target) {
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        return new float[] { targetYaw, targetPitch };
    }

    public static float applyLogNudge(Minecraft client, float currentAngle, float targetAngle, double strength) {
        float delta = Mth.wrapDegrees(targetAngle - currentAngle);
        float sign = Math.signum(delta);
        float absDelta = Math.abs(delta);

        if (absDelta < 0.05f) return 0f;

        double correction = Math.log(1.0 + absDelta) * strength;
        // Introduce a slight human error factor (e.g., +/- 2% micro-variance)
        double humanNoise = 0.98 + (ThreadLocalRandom.current().nextDouble() * 0.04);
        correction *= humanNoise;
        correction = Math.min(correction, absDelta);
        float rawNudge = (float) (sign * correction);
        return quantizeToMouseGrid(client, rawNudge);
    }
}
