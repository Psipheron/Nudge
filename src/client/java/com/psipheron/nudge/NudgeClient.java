package com.psipheron.nudge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class NudgeClient implements ClientModInitializer {
    private final MouseGridQuantizer yawQuantizer = new MouseGridQuantizer();
    private final MouseGridQuantizer pitchQuantizer = new MouseGridQuantizer();
    private Vec3 currentRandomPoint = null;
    private Player lastTarget = null;
    private int ticksSinceOffsetUpdate = 0;

    @Override
    public void onInitializeClient() {
        ModConfig.get();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            ModConfig config = ModConfig.get();

            Player target = AimAssistLogic.findBestTarget(client, config.maxRange, config.fovThreshold);
            if (target == null) {
                lastTarget = null;
                currentRandomPoint = null;
                yawQuantizer.reset();
                pitchQuantizer.reset();
                return;
            }

            if (target != lastTarget || ticksSinceOffsetUpdate > 10 || currentRandomPoint == null) {
                currentRandomPoint = AimAssistLogic.getRandomHitboxPoint(target.getBoundingBox());
                if (target != lastTarget) {
                    yawQuantizer.reset();
                    pitchQuantizer.reset();
                }
                lastTarget = target;
                ticksSinceOffsetUpdate = 0;
            }
            ticksSinceOffsetUpdate++;

            Vec3 eyePos = client.player.getEyePosition(1.0F);
            float[] desiredAngles = AimAssistLogic.calculateRotations(eyePos, currentRandomPoint);

            float nudgeYaw = 0f;
            float nudgePitch = 0f;

            if (config.aimMode == ModConfig.AimMode.LOG) {
                nudgeYaw = AimAssistLogic.applyLogNudge(client, yawQuantizer, client.player.getYRot(), desiredAngles[0], config.strength);
                nudgePitch = AimAssistLogic.applyLogNudge(client, pitchQuantizer, client.player.getXRot(), desiredAngles[1], config.strength);
            } else if (config.aimMode == ModConfig.AimMode.NUDGE) {
                nudgeYaw = AimAssistLogic.applyAggressiveNudge(client, yawQuantizer, client.player.getYRot(), desiredAngles[0], config.strength, config.nudgeJerkPower);
                nudgePitch = AimAssistLogic.applyAggressiveNudge(client, pitchQuantizer, client.player.getXRot(), desiredAngles[1], config.strength, config.nudgeJerkPower);
            }

            client.player.setYRot(client.player.getYRot() + nudgeYaw);
            client.player.setXRot(Mth.clamp(client.player.getXRot() + nudgePitch, -90.0F, 90.0F));
        });
    }
}
