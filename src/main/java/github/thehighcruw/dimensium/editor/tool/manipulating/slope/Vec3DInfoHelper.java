/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import imgui.ImGui;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
class Vec3DInfoHelper {

    private Vec3DInfoHelper() {}

    static void renderSlopeStats(Vec3DInt pos1, Vec3DInt pos2) {
        int heightDelta = pos2.y() - pos1.y();
        int dx = pos2.x() - pos1.x();
        int dz = pos2.z() - pos1.z();
        double xzDist = Math.sqrt(dx * dx + dz * dz);
        double euclidean = Math.sqrt(xzDist * xzDist + heightDelta * heightDelta);
        double angleDeg = Math.toDegrees(Math.atan2(Math.abs(heightDelta), xzDist));

        ImGui.text(I18n.format("dimensium.ui.slope.height_delta", heightDelta));
        ImGui.text(I18n.format("dimensium.ui.slope.xz_distance", String.format("%.1f", xzDist)));
        ImGui.text(I18n.format("dimensium.ui.slope.euclidean", String.format("%.1f", euclidean)));
        ImGui.text(I18n.format("dimensium.ui.slope.angle", String.format("%.1f", angleDeg)));
    }
}
