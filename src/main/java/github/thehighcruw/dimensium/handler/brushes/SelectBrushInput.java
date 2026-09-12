package github.thehighcruw.dimensium.handler.brushes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.world.SelectionRenderer;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class SelectBrushInput implements BrushInput {

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        FreecamState fs = FreecamState.INSTANCE;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        int mouseX = (int) fs.cursorX, mouseY = (int) fs.cursorY;

        SelectionState sel = SelectionState.INSTANCE;
        SelectToolState ts = SelectToolState.INSTANCE;

        if (sel.boxConfirmed) {
            if (button == KeyConstants.LMB) {
                EntityLivingBase eye = mc.renderViewEntity;
                if (eye != null) {
                    if (SelectionRenderer.boxPos1Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingX + 0.5, gy = sel.pendingY + 0.5, gz = sel.pendingZ + 0.5;
                        SelectionRenderer.boxPos1Gizmo
                            .startDrag(mouseX, mouseY, sw, sh, eye, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxPos2Gizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double gx = sel.pendingX2 + 0.5, gy = sel.pendingY2 + 0.5, gz = sel.pendingZ2 + 0.5;
                        SelectionRenderer.boxPos2Gizmo
                            .startDrag(mouseX, mouseY, sw, sh, eye, gx, gy, gz, gx, gy, gz, 0, 0, 0);
                        return true;
                    }
                    if (SelectionRenderer.boxCenterViewPlaneGizmo.hovered) {
                        double cxW = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
                        double cyW = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
                        double czW = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1X = sel.pendingX;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Y = sel.pendingY;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Z = sel.pendingZ;
                        SelectionRenderer.INSTANCE.boxCenterDragP2X = sel.pendingX2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Y = sel.pendingY2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Z = sel.pendingZ2;
                        SelectionRenderer.boxCenterViewPlaneGizmo
                            .startDrag(mouseX, mouseY, sw, sh, eye, cxW, cyW, czW, cxW, cyW, czW);
                        return true;
                    }
                    if (SelectionRenderer.boxCenterGizmo.hoveredAxis != TranslationGizmo.Axis.NONE) {
                        double cxW = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
                        double cyW = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
                        double czW = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.INSTANCE.boxCenterDragP1X = sel.pendingX;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Y = sel.pendingY;
                        SelectionRenderer.INSTANCE.boxCenterDragP1Z = sel.pendingZ;
                        SelectionRenderer.INSTANCE.boxCenterDragP2X = sel.pendingX2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Y = sel.pendingY2;
                        SelectionRenderer.INSTANCE.boxCenterDragP2Z = sel.pendingZ2;
                        SelectionRenderer.boxCenterGizmo
                            .startDrag(mouseX, mouseY, sw, sh, eye, cxW, cyW, czW, cxW, cyW, czW, 0, 0, 0);
                        return true;
                    }
                }
                GuiDimensiumOverlay.commitBoxSelection(sel, ts);
            }
            return true;
        }

        MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sw, sh);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;

        if (button == KeyConstants.RMB) {
            sel.pendingPos1 = true;
            sel.boxConfirmed = false;
            sel.pendingX = mop.blockX;
            sel.pendingY = mop.blockY;
            sel.pendingZ = mop.blockZ;
        }
        return true;
    }
}
