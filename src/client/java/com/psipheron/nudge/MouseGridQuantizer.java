package com.psipheron.nudge;

import net.minecraft.client.Minecraft;

/**
 * Rounds desired angular corrections onto Minecraft's mouse sensitivity grid.
 *
 * <p>One unit of raw mouse movement rotates the camera by
 * {@code (sensitivity * 0.6 + 0.2)^3 * 8 * 0.15} degrees. Rounding a correction to
 * that grid on its own throws away every nudge smaller than half a step, which is
 * most of them at low strength. This class carries the sub-step remainder into the
 * next call instead: repeated small nudges accumulate until they are worth a whole
 * step, so each returned delta is an exact multiple of the grid step while the
 * long-run total rotation keeps tracking the unquantized input.
 *
 * <p>Instances are single-axis. Yaw and pitch share the same step size but keep
 * independent remainders.
 */
public final class MouseGridQuantizer {
    private double remainder;

    /** Number of degrees one raw mouse unit turns the camera, or 0 if unavailable. */
    public static double gridStepDegrees(Minecraft client) {
        if (client.options == null) return 0.0;
        double sensitivity = client.options.sensitivity().get();
        double f = sensitivity * 0.6 + 0.2;
        return f * f * f * 8.0 * 0.15;
    }

    /** Drops any carried remainder, e.g. when the target or world changes. */
    public void reset() {
        remainder = 0.0;
    }

    public float quantize(Minecraft client, float desiredDelta) {
        double step = gridStepDegrees(client);
        if (step <= 0.0) {
            remainder = 0.0;
            return desiredDelta;
        }

        double pendingSteps = remainder + desiredDelta / step;
        double steps = Math.rint(pendingSteps);
        remainder = pendingSteps - steps;

        // The returned nudge can exceed the remaining error by up to half a step
        // (~0.08 degrees at default sensitivity). The remainder subtracts that
        // excess on the following tick, and the caller's dead zone stops the
        // overshoot from turning into a limit cycle at rest.
        return (float) (steps * step);
    }
}
