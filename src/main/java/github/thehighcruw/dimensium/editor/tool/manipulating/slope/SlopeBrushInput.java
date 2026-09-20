/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.slope;

import com.github.bsideup.jabel.Desugar;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.BrushApplicator;
import github.thehighcruw.dimensium.editor.tool.BrushInput;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class SlopeBrushInput implements BrushInput {

    public static final SlopeBrushInput INSTANCE = new SlopeBrushInput();

    private boolean dragActive = false;
    private Vec3DInt lastDragPos = null;

    private SlopeBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public void onMouseClick(int button, Minecraft mc, MovingObjectPosition mop) {
        if (button != KeyConstants.RMB) return;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        SlopeToolState state = SlopeToolState.INSTANCE;
        if (state.hasPos1) return;
        state.hasPos1 = true;
        state.pos1 = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
    }

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        if (fs.isMoving()) return true;

        SlopeToolState state = SlopeToolState.INSTANCE;

        if (!Mouse.isButtonDown(KeyConstants.RMB)) {
            if (dragActive) {
                List<int[]> ops = ChangeProposal.flush();
                if (!ops.isEmpty()) BlockSender.sendChunked(ops, I18n.format("dimensium.action.slope"));
                dragActive = false;
                lastDragPos = null;
            }
            state.hasPos2 = false;
            return true;
        }

        if (!state.hasPos1) return true;

        int sf = RenderUtils.scaleFactor();
        int mx = (int) fs.cursorX, my = (int) fs.cursorY;
        if (mx * sf < OverlayRenderer.TOOL_WINDOW.getWidth() || my * sf < (int) MenuBar.INSTANCE.height()) return true;

        Vec3DInt projected = projectOntoSlope(state, mc, mx, my, sw, sh);
        if (projected == null) return true;
        if (projected.equals(state.pos1)) return true;

        if (!state.hasPos2) {
            state.hasPos2 = true;
            state.pos2 = projected;
        }

        if (!dragActive) {
            ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
            dragActive = true;
        }

        if (projected.equals(lastDragPos)) return true;
        lastDragPos = projected;

        BrushApplicator.applyTool(mc.theWorld, projected);
        return true;
    }

    /**
     * Intersects the camera ray with the slope surface (plane or cone) and returns the block
     * coordinate at the intersection. Returns null if the ray misses or is parallel.
     */
    private static Vec3DInt projectOntoSlope(SlopeToolState state, Minecraft mc, int mx, int my, int sw, int sh) {
        if (!state.hasPos2) {
            // Pos2 not yet locked — use world raycast to find the natural drag-start position.
            MovingObjectPosition worldMop = GuiDimensiumOverlay.raycastFromMouse(mx, my, sw, sh);
            if (worldMop == null || worldMop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
            return Vec3DInt.from(worldMop.blockX, worldMop.blockY, worldMop.blockZ);
        }

        Vec3DInt pos1 = state.pos1;
        Vec3DInt pos2 = state.pos2;
        double axisX = pos2.x() - pos1.x();
        double axisZ = pos2.z() - pos1.z();
        double axisLen2 = axisX * axisX + axisZ * axisZ;
        double axisLen = Math.sqrt(axisLen2);
        double heightDelta = pos2.y() - pos1.y();

        Ray ray = cameraRay(mc, mx, my, sw, sh);

        if (state.slopeShape == SlopeToolState.SlopeShape.PLANE) {
            return intersectSlopePlane(ray.eyePos, ray.rayDir, pos1, axisX, axisZ, axisLen2, heightDelta);
        } else {
            return intersectSlopeCone(ray.eyePos, ray.rayDir, pos1, axisLen, heightDelta);
        }
    }

    /** Normal of the slope plane: N = (heightDelta*axisX, -axisLen2, heightDelta*axisZ). */
    private static Vec3DInt intersectSlopePlane(
            Vec3DDouble eyePos,
            Vec3DDouble rayDir,
            Vec3DInt pos1,
            double axisX,
            double axisZ,
            double axisLen2,
            double heightDelta) {
        Vec3DDouble normal = Vec3DDouble.from(heightDelta * axisX, -axisLen2, heightDelta * axisZ);
        double denom = normal.dot(rayDir);
        if (Math.abs(denom) < 1e-6) return null;

        double t = normal.dot(pos1.toDouble().minus(eyePos)) / denom;
        if (t <= 0) return null;

        Vec3DDouble hit = eyePos.plus(rayDir.times(t));
        return Vec3DInt.from((int) Math.floor(hit.x()), (int) Math.floor(hit.y()), (int) Math.floor(hit.z()));
    }

    /**
     * Intersects the ray with the cone by intersecting against horizontal planes iteratively via
     * Newton's method — finds t where (oy + t*dy - pos1.y)*axisLen/heightDelta = xzRadius(t).
     */
    private static Vec3DInt intersectSlopeCone(
            Vec3DDouble eyePos, Vec3DDouble rayDir, Vec3DInt pos1, double axisLen, double heightDelta) {
        if (Math.abs(heightDelta) < 0.001 || Math.abs(rayDir.y()) < 1e-9) return null;

        double t = 10.0;
        for (int i = 0; i < 16; i++) {
            double hx = eyePos.x() + t * rayDir.x() - pos1.x();
            double hz = eyePos.z() + t * rayDir.z() - pos1.z();
            double xzRadius = Math.sqrt(hx * hx + hz * hz);
            double coneY = pos1.y() + xzRadius / axisLen * heightDelta;
            t = (coneY - eyePos.y()) / rayDir.y();
            if (t <= 0) return null;
        }

        Vec3DDouble hit = eyePos.plus(rayDir.times(t));
        return Vec3DInt.from((int) Math.floor(hit.x()), (int) Math.floor(hit.y()), (int) Math.floor(hit.z()));
    }

    private static Ray cameraRay(Minecraft mc, int mx, int my, int sw, int sh) {
        EntityLivingBase eye = mc.renderViewEntity;
        Vec3DDouble eyePos = Vec3DDouble.from(eye.posX, eye.posY + eye.getEyeHeight(), eye.posZ);

        FreecamState fs = FreecamState.INSTANCE;
        double tanHX = fs.projTanHX;
        double tanHY = fs.projTanHY;

        ViewportState vp = ViewportRegistry.INSTANCE.active();
        double ndcX = vp != null ? vp.cursorToNdcX(mx, sw) : 1.0 - (2.0 * mx / sw);
        double ndcY = vp != null ? vp.cursorToNdcY(my, sh) : 1.0 - (2.0 * my / sh);

        Vec3DDouble[] basis = FreecamUtils.cameraBasis(eye.rotationYaw, eye.rotationPitch);
        Vec3DDouble fwd = basis[0], rgt = basis[1], up = basis[2];

        Vec3DDouble rayDir = Vec3DDouble.from(
                        fwd.x() + rgt.x() * ndcX * tanHX + up.x() * ndcY * tanHY,
                        fwd.y() + up.y() * ndcY * tanHY,
                        fwd.z() + rgt.z() * ndcX * tanHX + up.z() * ndcY * tanHY)
                .normalize();

        return new Ray(eyePos, rayDir);
    }

    @Desugar
    private record Ray(Vec3DDouble eyePos, Vec3DDouble rayDir) {}
}
