/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.util.Vec3;

public class BuilderToolState {

    public static final BuilderToolState INSTANCE = new BuilderToolState();

    // Active builder tool — cycled via Left Alt + scroll
    public BuilderTool activeTool = BuilderTool.MOVE;

    // ── Phase ─────────────────────────────────────────────────────────────────
    public enum Phase {
        /** No active operation. Clicks select corners. */
        IDLE,
        /** First corner set, waiting for second. */
        SELECTING,
        /** Selection confirmed; server is reading world state. Input ignored until done. */
        CAPTURING,
        /** Both corners set; scroll/middle-click repositions hologram. */
        MANIPULATING,
        /** AABB selected for erase; waiting for DELETE/BACKSPACE confirmation. */
        CONFIRMING
    }

    public Phase phase = Phase.IDLE;

    // ── Hologram offset from selection origin ─────────────────────────────────
    public Vec3DInt offset = Vec3DInt.ZERO;

    // ── Axis lock (held X / Y / Z key during manipulation) ────────────────────
    public enum AxisLock {
        NONE,
        X,
        Y,
        Z
    }

    public AxisLock axisLock = AxisLock.NONE;

    // ── Preview proposals ─────────────────────────────────────────────────────
    public ChangeProposal fillPreview = null;
    public ChangeProposal extrudePreview = null;
    public ChangeProposal smearPreview = null;
    public ChangeProposal magicPreview = null;

    // Signed repeat counts per axis. Positive = +axis direction, negative = -axis direction, 0 = none.
    // Step size along each axis is always the selection dimension (width/height/depth).
    public Vec3DInt stack = Vec3DInt.ZERO;

    public void resetPhase() {
        phase = Phase.IDLE;
        offset = Vec3DInt.ZERO;
        axisLock = AxisLock.NONE;
        stack = Vec3DInt.ZERO;
    }

    /**
     * Apply one scroll tick to the offset, constrained by axisLock and the
     * player's facing direction. direction > 0 = away from player, < 0 = toward.
     */
    public void nudgeOffset(int direction, Vec3 facing) {
        double ax = Math.abs(facing.xCoord);
        double ay = Math.abs(facing.yCoord);
        double az = Math.abs(facing.zCoord);

        int dx = 0, dy = 0, dz = 0;
        switch (axisLock) {
            case X:
                dx = direction;
                break;
            case Y:
                dy = direction;
                break;
            case Z:
                dz = direction;
                break;
            default:
                if (ax >= ay && ax >= az) dx = (int) Math.signum(facing.xCoord) * direction;
                else if (ay >= ax && ay >= az) dy = (int) Math.signum(facing.yCoord) * direction;
                else dz = (int) Math.signum(facing.zCoord) * direction;
        }
        offset = offset.plus(Vec3DInt.from(dx, dy, dz));
    }
}
