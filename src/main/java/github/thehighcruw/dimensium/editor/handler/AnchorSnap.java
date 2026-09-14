/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeMath;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;

@SideOnly(Side.CLIENT)
public class AnchorSnap {

    private AnchorSnap() {}

    /** Snaps or passes through a gizmo-drag coordinate to a float anchor. */
    public static float toFloat(double v, boolean snap) {
        return snap ? (float) Math.floor(v + 0.5) : (float) v;
    }

    /** Snaps or passes through a gizmo-drag coordinate to an integer block position. */
    public static int toInt(double v, boolean snap) {
        return (int) Math.floor(snap ? Math.floor(v + 0.5) : v);
    }

    /**
     * Applies a rotation gizmo drag to a base rotation and returns the updated Euler angles as Vec3DFloat(rotX, rotY,
     * rotZ).
     */
    public static Vec3DFloat applyRotGizmo(RotationGizmo gizmo, float baseX, float baseY, float baseZ, int mx, int my) {
        float delta = gizmo.updateDrag(mx, my);
        RotationGizmo.Axis axis = gizmo.getDragAxis();
        Mat3DFloat rBase = ShapeMath.buildRotationMatrix(baseX, baseY, baseZ);
        Mat3DFloat dR = axis == RotationGizmo.Axis.X
                ? Mat3DFloat.fromEulerDeg(delta, 0, 0)
                : axis == RotationGizmo.Axis.Y
                        ? Mat3DFloat.fromEulerDeg(0, delta, 0)
                        : Mat3DFloat.fromEulerDeg(0, 0, delta);
        return rBase.mul(dR).toEulerDeg();
    }
}
