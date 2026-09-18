/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.modelling;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

@SideOnly(Side.CLIENT)
public class ModellingToolRenderer implements ToolRenderer {

    public static final ModellingToolRenderer INSTANCE = new ModellingToolRenderer();

    private ModellingToolRenderer() {}

    @Override
    public boolean isBlockAffected(Minecraft mc, Vec3DInt wc) {
        return false;
    }

    @Override
    public boolean renderHover(MovingObjectPosition mop, Vec3DDouble camPos) {
        return true;
    }

    @Override
    public void renderOverlay(Minecraft mc, int mx, int my, int mx3d, int my3d) {
        ModellingToolState mts = ModellingToolState.INSTANCE;
        ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
        if (mSelPt == null) return;
        if (!mts.getAxisTranslationGizmo().isDragging()
                && !mts.getPlaneTranslationGizmo().isDragging()
                && mc.renderViewEntity != null) {
            Vec3DDouble gp = mSelPt.pos().toDouble().plus(0.5);
            mts.getAxisTranslationGizmo().updateHover(mx3d, my3d, mc.renderViewEntity, gp, Vec3DFloat.ZERO);
        }
    }
}
