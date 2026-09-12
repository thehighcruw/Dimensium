package github.thehighcruw.dimensium.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.render.panel.PanelDraw;
import github.thehighcruw.dimensium.render.panel.ToolOptionsPanel;
import github.thehighcruw.dimensium.render.panel.ToolPanel;
import github.thehighcruw.dimensium.render.popup.AnalyzeWindow;
import github.thehighcruw.dimensium.render.popup.AutoshadeWindow;
import github.thehighcruw.dimensium.render.popup.BlockInfoWindow;
import github.thehighcruw.dimensium.render.popup.BlockPickerPopup;
import github.thehighcruw.dimensium.render.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.render.popup.ClipboardWindow;
import github.thehighcruw.dimensium.render.popup.ColourFieldWindow;
import github.thehighcruw.dimensium.render.popup.ConflictPopup;
import github.thehighcruw.dimensium.render.popup.CreateBlueprintPopup;
import github.thehighcruw.dimensium.render.popup.DistortSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FillSelectionWindow;
import github.thehighcruw.dimensium.render.popup.FilterSelectionWindow;
import github.thehighcruw.dimensium.render.popup.OperationsWindow;
import github.thehighcruw.dimensium.render.popup.PaletteEditorWindow;
import github.thehighcruw.dimensium.render.popup.PaletteWindow;
import github.thehighcruw.dimensium.render.popup.ReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.popup.SelectionWindow;
import github.thehighcruw.dimensium.render.popup.SettingsModal;
import github.thehighcruw.dimensium.render.popup.SmoothSelectionWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskEditorWindow;
import github.thehighcruw.dimensium.render.popup.ToolMaskListWindow;
import github.thehighcruw.dimensium.render.popup.TypeReplaceSelectionWindow;
import github.thehighcruw.dimensium.render.sidebar.HistoryWindow;
import github.thehighcruw.dimensium.render.world.RotationGizmo;
import github.thehighcruw.dimensium.render.world.SelectionRenderer;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.math.ShapeMath;
import github.thehighcruw.dimensium.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.tool.state.LassoSelectToolState;
import github.thehighcruw.dimensium.tool.state.ModellingToolState;
import github.thehighcruw.dimensium.tool.state.MoveToolState;
import github.thehighcruw.dimensium.tool.state.PathToolState;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import github.thehighcruw.dimensium.tool.state.ShapePlacementState;
import imgui.ImGui;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;

@SideOnly(Side.CLIENT)
public class OverlayRenderer {

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    public static final ToolPanel toolPanel = new ToolPanel();
    public static final ToolOptionsPanel toolOptionsPanel = new ToolOptionsPanel(toolPanel);
    public static final BlockPickerPopup picker = new BlockPickerPopup();

    private int savedCurrentItem = 0;

    public static boolean cheatsAllowed() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode;
    }

    /**
     * Cancel all vanilla HUD elements while the Dimensium editor overlay is active.
     * The editor draws its own UI via ImGui; vanilla HUD would render on top of the viewport.
     * Note: the first-person arm (EntityRenderer.renderHand) has no Forge hook in 1.7.10
     * and cannot be suppressed here — it requires ASM.
     */
    @SubscribeEvent
    public void onHudPre(RenderGameOverlayEvent.Pre event) {
        if (!DimensiumMode.INSTANCE.isActive()) return;
        if (!cheatsAllowed()) return;
        RenderGameOverlayEvent.ElementType t = event.type;
        if (t != RenderGameOverlayEvent.ElementType.ALL && t != RenderGameOverlayEvent.ElementType.TEXT) {
            event.setCanceled(true);
        }
    }

    /**
     * Before the vanilla hotbar renders: push currentItem to -100 so the
     * selected-slot highlight draws off-screen, leaving slot 8 un-highlighted.
     * Only applies in builder-tools mode (not freecam — HOTBAR is cancelled there by onHudPre).
     */
    @SubscribeEvent
    public void onHotbarPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (DimensiumMode.INSTANCE.isActive()) return;
        if (!DimensiumMode.INSTANCE.isBuilderToolsActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !cheatsAllowed()) return;
        savedCurrentItem = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = -100;
    }

    /** After the vanilla hotbar renders: restore currentItem. */
    @SubscribeEvent
    public void onHotbarPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        if (DimensiumMode.INSTANCE.isActive()) return;
        if (!DimensiumMode.INSTANCE.isBuilderToolsActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !cheatsAllowed()) return;
        mc.thePlayer.inventory.currentItem = savedCurrentItem;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.isCancelable() || event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        // Restore itemRenderer swapped out in RenderTickEvent.START to suppress the arm.
        // renderHand already fired at this point, so restoration is safe.
        HandRenderer.INSTANCE.restore(mc);
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        if (!cheatsAllowed()) return;

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        if (DimensiumMode.INSTANCE.isActive()) {
            // Use software cursor position (tracked in TickHandler from mouse delta).
            FreecamState fs = FreecamState.INSTANCE;
            int mx = (int) fs.cursorX;
            int my = (int) fs.cursorY;
            int mx3d = (int) fs.cursorX3d;
            int my3d = (int) fs.cursorY3d;

            toolPanel.updateMouse(mx, my);

            ShapePlacementState ps = ShapePlacementState.INSTANCE;
            if (ps.active) {
                double cx = ps.centerX(), cy = ps.centerY(), cz = ps.centerZ();
                if (ps.gizmo.isDragging()) {
                    double[] anchor = ps.gizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        ps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                        ps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                        ps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                        ps.anchorX = (int) Math.floor(ps.anchorFX);
                        ps.anchorY = (int) Math.floor(ps.anchorFY);
                        ps.anchorZ = (int) Math.floor(ps.anchorFZ);
                    }
                } else if (ps.rotGizmo.isDragging()) {
                    float delta = ps.rotGizmo.updateDrag(mx3d, my3d);
                    RotationGizmo.Axis axis = ps.rotGizmo.getDragAxis();
                    float[] Rbase = ShapeMath.buildRotationMatrix(ps.rotDragBaseX, ps.rotDragBaseY, ps.rotDragBaseZ);
                    float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                        : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                            : ShapeMath.buildRotationMatrix(0, 0, delta);
                    float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                    float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                    if (Math.abs(angles[0] - ps.rotX) >= 0.5f || Math.abs(angles[1] - ps.rotY) >= 0.5f
                        || Math.abs(angles[2] - ps.rotZ) >= 0.5f) {
                        ps.rotX = angles[0];
                        ps.rotY = angles[1];
                        ps.rotZ = angles[2];
                        ps.invalidateGhost();
                    }
                } else if (ps.scaleGizmo.isDragging()) {
                    float[] scales = ps.scaleGizmo.updateDrag(mx3d, my3d);
                    if (scales != null) {
                        github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane plane = ps.scaleGizmo.getDragPlane();
                        if (plane == github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane.XY) {
                            ps.scaleX = scales[0];
                            ps.scaleY = scales[1];
                        } else if (plane == github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane.XZ) {
                            ps.scaleX = scales[0];
                            ps.scaleZ = scales[1];
                        } else {
                            ps.scaleY = scales[0];
                            ps.scaleZ = scales[1];
                        }
                        ps.invalidateGhost();
                    }
                } else if (ps.viewPlaneGizmo.isDragging()) {
                    double[] anchor = ps.viewPlaneGizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        ps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                        ps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                        ps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                        ps.anchorX = (int) Math.floor(ps.anchorFX);
                        ps.anchorY = (int) Math.floor(ps.anchorFY);
                        ps.anchorZ = (int) Math.floor(ps.anchorFZ);
                    }
                } else if (mc.renderViewEntity != null) {
                    net.minecraft.entity.EntityLivingBase eye = mc.renderViewEntity;
                    ps.viewPlaneGizmo.updateHover(mx3d, my3d, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                    if (!ps.viewPlaneGizmo.hovered) {
                        ps.gizmo.updateHover(mx3d, my3d, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                        if (ps.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                            ps.rotGizmo.updateHover(mx3d, my3d, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                            if (ps.rotGizmo.hoveredAxis == RotationGizmo.Axis.NONE) {
                                ps.scaleGizmo
                                    .updateHover(mx3d, my3d, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                            } else {
                                ps.scaleGizmo.hoveredPlane = github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane.NONE;
                            }
                        } else {
                            ps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                            ps.scaleGizmo.hoveredPlane = github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane.NONE;
                        }
                    } else {
                        ps.gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
                        ps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                        ps.scaleGizmo.hoveredPlane = github.thehighcruw.dimensium.render.world.ScaleGizmo.Plane.NONE;
                    }
                }
            }

            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            if (cps.active) {
                double ccx = cps.centerX(), ccy = cps.centerY(), ccz = cps.centerZ();
                if (cps.gizmo.isDragging()) {
                    double[] anchor = cps.gizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        cps.anchorFX = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                        cps.anchorFY = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                        cps.anchorFZ = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                        int newAX = (int) Math.floor(cps.anchorFX);
                        int newAY = (int) Math.floor(cps.anchorFY);
                        int newAZ = (int) Math.floor(cps.anchorFZ);
                        if (newAX != cps.anchorX || newAY != cps.anchorY || newAZ != cps.anchorZ) {
                            cps.anchorX = newAX;
                            cps.anchorY = newAY;
                            cps.anchorZ = newAZ;
                            cps.rebuildPreview();
                        }
                    }
                } else if (cps.rotGizmo.isDragging()) {
                    float delta = cps.rotGizmo.updateDrag(mx3d, my3d);
                    RotationGizmo.Axis axis = cps.rotGizmo.getDragAxis();
                    float[] Rbase = ShapeMath.buildRotationMatrix(cps.rotDragBaseX, cps.rotDragBaseY, cps.rotDragBaseZ);
                    float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                        : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                            : ShapeMath.buildRotationMatrix(0, 0, delta);
                    float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                    float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                    cps.rotX = angles[0];
                    cps.rotY = angles[1];
                    cps.rotZ = angles[2];
                    cps.rebuildPreview();
                } else if (mc.renderViewEntity != null) {
                    net.minecraft.entity.EntityLivingBase cEye = mc.renderViewEntity;
                    cps.gizmo.updateHover(mx3d, my3d, sw, sh, cEye, ccx, ccy, ccz, 0, 0, 0);
                    if (cps.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                        cps.rotGizmo.updateHover(mx3d, my3d, sw, sh, cEye, ccx, ccy, ccz, cps.rotX, cps.rotY, cps.rotZ);
                    } else {
                        cps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                    }
                }
            }

            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active) {
                double gx = ms.gizmoX(), gy = ms.gizmoY(), gz = ms.gizmoZ();
                if (ms.gizmo.isDragging()) {
                    double[] anchor = ms.gizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        float nx = snap ? (float) Math.floor(anchor[0] + 0.5) : (float) anchor[0];
                        float ny = snap ? (float) Math.floor(anchor[1] + 0.5) : (float) anchor[1];
                        float nz = snap ? (float) Math.floor(anchor[2] + 0.5) : (float) anchor[2];
                        ms.deltaFX = nx - ms.cmX;
                        ms.deltaFY = ny - ms.cmY;
                        ms.deltaFZ = nz - ms.cmZ;
                        ms.invalidateGhost();
                    }
                } else if (ms.rotGizmo.isDragging()) {
                    float delta = ms.rotGizmo.updateDrag(mx3d, my3d);
                    RotationGizmo.Axis axis = ms.rotGizmo.getDragAxis();
                    float[] Rbase = ShapeMath.buildRotationMatrix(ms.rotDragBaseX, ms.rotDragBaseY, ms.rotDragBaseZ);
                    float[] dR = axis == RotationGizmo.Axis.X ? ShapeMath.buildRotationMatrix(delta, 0, 0)
                        : axis == RotationGizmo.Axis.Y ? ShapeMath.buildRotationMatrix(0, delta, 0)
                            : ShapeMath.buildRotationMatrix(0, 0, delta);
                    float[] Rnew = ShapeMath.multiplyRotationMatrices(Rbase, dR);
                    float[] angles = ShapeMath.decomposeRotationMatrix(Rnew);
                    if (Math.abs(angles[0] - ms.rotX) >= 0.5f || Math.abs(angles[1] - ms.rotY) >= 0.5f
                        || Math.abs(angles[2] - ms.rotZ) >= 0.5f) {
                        ms.rotX = angles[0];
                        ms.rotY = angles[1];
                        ms.rotZ = angles[2];
                        ms.invalidateGhost();
                    }
                } else if (mc.renderViewEntity != null) {
                    net.minecraft.entity.EntityLivingBase eye = mc.renderViewEntity;
                    ms.gizmo.updateHover(mx3d, my3d, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
                    if (ms.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                        ms.rotGizmo.updateHover(mx3d, my3d, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
                    } else {
                        ms.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                    }
                }
            }

            // ── Path tool gizmo hover + drag ─────────────────────────────────
            PathToolState pathState = PathToolState.INSTANCE;
            if (DimensiumMode.INSTANCE.selectedTool == Tool.PATH && pathState.selectedIndex >= 0
                && !pathState.points.isEmpty()) {
                PathToolState.PathPoint selPt = pathState.selectedPoint();
                if (selPt != null) {
                    double pgx = selPt.x + 0.5, pgy = selPt.y + 0.5, pgz = selPt.z + 0.5;
                    if (pathState.gizmo.isDragging()) {
                        double[] anchor = pathState.gizmo.updateDrag(mx3d, my3d);
                        if (anchor != null) {
                            boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                            selPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                            selPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                            selPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                            pathState.invalidatePath();
                        }
                    } else if (mc.renderViewEntity != null) {
                        pathState.gizmo.updateHover(mx3d, my3d, sw, sh, mc.renderViewEntity, pgx, pgy, pgz, 0, 0, 0);
                    }
                }
            }

            // ── Modelling tool gizmo hover + drag ────────────────────────────
            ModellingToolState mts = ModellingToolState.INSTANCE;
            if (DimensiumMode.INSTANCE.selectedTool == Tool.MODELLING) {
                ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
                if (mSelPt != null) {
                    double mgx = mSelPt.x + 0.5, mgy = mSelPt.y + 0.5, mgz = mSelPt.z + 0.5;
                    if (mts.gizmo.isDragging()) {
                        double[] anchor = mts.gizmo.updateDrag(mx3d, my3d);
                        if (anchor != null) {
                            boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                            mSelPt.x = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                            mSelPt.y = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                            mSelPt.z = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                            mts.invalidate();
                        }
                    } else if (mc.renderViewEntity != null) {
                        mts.gizmo.updateHover(mx3d, my3d, sw, sh, mc.renderViewEntity, mgx, mgy, mgz, 0, 0, 0);
                    }
                }
            }

            // ── Box-select gizmos hover + drag ────────────────────────────────
            SelectionState bxSel = SelectionState.INSTANCE;
            if (bxSel.boxConfirmed && DimensiumMode.INSTANCE.selectedTool != Tool.SELECT) {
                GuiDimensiumOverlay.commitBoxSelection(bxSel, SelectToolState.INSTANCE);
            }
            if (bxSel.boxConfirmed && DimensiumMode.INSTANCE.selectedTool == Tool.SELECT
                && mc.renderViewEntity != null) {
                net.minecraft.entity.EntityLivingBase bxEye = mc.renderViewEntity;
                boolean snap = org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT);
                if (SelectionRenderer.boxPos1Gizmo.isDragging()) {
                    double[] anchor = SelectionRenderer.boxPos1Gizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        bxSel.pendingX = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                        bxSel.pendingY = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                        bxSel.pendingZ = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                    }
                } else if (SelectionRenderer.boxPos2Gizmo.isDragging()) {
                    double[] anchor = SelectionRenderer.boxPos2Gizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        bxSel.pendingX2 = (int) Math.floor(snap ? Math.floor(anchor[0] + 0.5) : anchor[0]);
                        bxSel.pendingY2 = (int) Math.floor(snap ? Math.floor(anchor[1] + 0.5) : anchor[1]);
                        bxSel.pendingZ2 = (int) Math.floor(snap ? Math.floor(anchor[2] + 0.5) : anchor[2]);
                    }
                } else if (SelectionRenderer.boxCenterViewPlaneGizmo.isDragging()) {
                    double[] anchor = SelectionRenderer.boxCenterViewPlaneGizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1X
                            + SelectionRenderer.INSTANCE.boxCenterDragP2X) / 2.0 + 0.5;
                        double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Y
                            + SelectionRenderer.INSTANCE.boxCenterDragP2Y) / 2.0 + 0.5;
                        double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Z
                            + SelectionRenderer.INSTANCE.boxCenterDragP2Z) / 2.0 + 0.5;
                        int dx = (int) Math.floor(snap ? Math.floor(anchor[0] - cx0 + 0.5) : anchor[0] - cx0);
                        int dy = (int) Math.floor(snap ? Math.floor(anchor[1] - cy0 + 0.5) : anchor[1] - cy0);
                        int dz = (int) Math.floor(snap ? Math.floor(anchor[2] - cz0 + 0.5) : anchor[2] - cz0);
                        bxSel.pendingX = SelectionRenderer.INSTANCE.boxCenterDragP1X + dx;
                        bxSel.pendingY = SelectionRenderer.INSTANCE.boxCenterDragP1Y + dy;
                        bxSel.pendingZ = SelectionRenderer.INSTANCE.boxCenterDragP1Z + dz;
                        bxSel.pendingX2 = SelectionRenderer.INSTANCE.boxCenterDragP2X + dx;
                        bxSel.pendingY2 = SelectionRenderer.INSTANCE.boxCenterDragP2Y + dy;
                        bxSel.pendingZ2 = SelectionRenderer.INSTANCE.boxCenterDragP2Z + dz;
                    }
                } else if (SelectionRenderer.boxCenterGizmo.isDragging()) {
                    double[] anchor = SelectionRenderer.boxCenterGizmo.updateDrag(mx3d, my3d);
                    if (anchor != null) {
                        double cx0 = (SelectionRenderer.INSTANCE.boxCenterDragP1X
                            + SelectionRenderer.INSTANCE.boxCenterDragP2X) / 2.0 + 0.5;
                        double cy0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Y
                            + SelectionRenderer.INSTANCE.boxCenterDragP2Y) / 2.0 + 0.5;
                        double cz0 = (SelectionRenderer.INSTANCE.boxCenterDragP1Z
                            + SelectionRenderer.INSTANCE.boxCenterDragP2Z) / 2.0 + 0.5;
                        int dx = (int) Math.floor(snap ? Math.floor(anchor[0] - cx0 + 0.5) : anchor[0] - cx0);
                        int dy = (int) Math.floor(snap ? Math.floor(anchor[1] - cy0 + 0.5) : anchor[1] - cy0);
                        int dz = (int) Math.floor(snap ? Math.floor(anchor[2] - cz0 + 0.5) : anchor[2] - cz0);
                        bxSel.pendingX = SelectionRenderer.INSTANCE.boxCenterDragP1X + dx;
                        bxSel.pendingY = SelectionRenderer.INSTANCE.boxCenterDragP1Y + dy;
                        bxSel.pendingZ = SelectionRenderer.INSTANCE.boxCenterDragP1Z + dz;
                        bxSel.pendingX2 = SelectionRenderer.INSTANCE.boxCenterDragP2X + dx;
                        bxSel.pendingY2 = SelectionRenderer.INSTANCE.boxCenterDragP2Y + dy;
                        bxSel.pendingZ2 = SelectionRenderer.INSTANCE.boxCenterDragP2Z + dz;
                    }
                } else {
                    SelectionRenderer.boxPos1Gizmo.updateHover(
                        mx,
                        my,
                        sw,
                        sh,
                        bxEye,
                        bxSel.pendingX + 0.5,
                        bxSel.pendingY + 0.5,
                        bxSel.pendingZ + 0.5,
                        0,
                        0,
                        0);
                    if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                        SelectionRenderer.boxPos2Gizmo.updateHover(
                            mx,
                            my,
                            sw,
                            sh,
                            bxEye,
                            bxSel.pendingX2 + 0.5,
                            bxSel.pendingY2 + 0.5,
                            bxSel.pendingZ2 + 0.5,
                            0,
                            0,
                            0);
                    } else {
                        SelectionRenderer.boxPos2Gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
                    }
                    if (SelectionRenderer.boxPos1Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE
                        && SelectionRenderer.boxPos2Gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                        double cxW = (bxSel.pendingX + bxSel.pendingX2) / 2.0 + 0.5;
                        double cyW = (bxSel.pendingY + bxSel.pendingY2) / 2.0 + 0.5;
                        double czW = (bxSel.pendingZ + bxSel.pendingZ2) / 2.0 + 0.5;
                        SelectionRenderer.boxCenterViewPlaneGizmo
                            .updateHover(mx, my, sw, sh, bxEye, cxW, cyW, czW, 0, 0, 0);
                        SelectionRenderer.boxCenterGizmo.updateHover(mx, my, sw, sh, bxEye, cxW, cyW, czW, 0, 0, 0);
                    } else {
                        SelectionRenderer.boxCenterViewPlaneGizmo.hovered = false;
                        SelectionRenderer.boxCenterGizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
                    }
                }
            }

            ViewportRegistry.INSTANCE.flushPendingDeletions();

            // Capture world render into the active viewport's texture before ImGui overdraw.
            // Inactive viewports keep their last captured frame — only the active tab renders.
            ViewportState activeVp = ViewportRegistry.INSTANCE.active();
            if (activeVp != null) ViewportCapture.capture(activeVp, mc.displayWidth, mc.displayHeight);

            // ImGui display space = physical pixels. Panels sized in physical px.
            int sf = sr.getScaleFactor();
            ImGuiManager.INSTANCE
                .newFrame(mc.displayWidth, mc.displayHeight, sf, (float) fs.cursorX, (float) fs.cursorY);
            MenuBar.INSTANCE.render();
            renderDockSpace(mc.displayWidth, mc.displayHeight);
            ViewportPanel.INSTANCE.render(mc.displayWidth, mc.displayHeight);
            toolPanel.render(mc.displayWidth, mc.displayHeight);
            toolOptionsPanel.render(mc.displayWidth, mc.displayHeight);
            picker.renderImGui(mc);
            ConflictPopup.INSTANCE.renderImGui();
            CreateBlueprintPopup.INSTANCE.renderImGui(mc);
            BlueprintBrowserPopup.INSTANCE.renderImGui(mc);
            SettingsModal.INSTANCE.renderImGui();
            FilterSelectionWindow.INSTANCE.renderImGui();
            DistortSelectionWindow.INSTANCE.renderImGui();
            SmoothSelectionWindow.INSTANCE.renderImGui();
            FillSelectionWindow.INSTANCE.renderImGui();
            ReplaceSelectionWindow.INSTANCE.renderImGui();
            TypeReplaceSelectionWindow.INSTANCE.renderImGui();
            ColourFieldWindow.INSTANCE.renderImGui();
            AnalyzeWindow.INSTANCE.renderImGui();
            AutoshadeWindow.INSTANCE.renderImGui();
            BlockInfoWindow.INSTANCE.renderImGui();
            SelectionWindow.INSTANCE.renderImGui();
            OperationsWindow.INSTANCE.renderImGui();
            ClipboardWindow.INSTANCE.renderImGui();
            ToolMaskListWindow.INSTANCE.renderImGui();
            ToolMaskEditorWindow.INSTANCE.renderImGui();
            PaletteWindow.INSTANCE.renderImGui(mc);
            PaletteEditorWindow.INSTANCE.renderImGui(mc);
            HistoryWindow.INSTANCE.renderImGui();
            StatusBar.INSTANCE.render(mc.displayWidth, mc.displayHeight);
            renderKeyPressLog(mc.displayWidth, mc.displayHeight);
            ImGuiManager.INSTANCE.endFrame();

            // Rebake clipboard FBO after ImGui has rendered — result used next frame.
            // Running before endFrame() risks corrupting GL state that renderDrawData() needs.
            ClipboardWindow.INSTANCE.prebake();

            // Non-ImGui GL overlays
            if (DimensiumMode.INSTANCE.selectedTool == Tool.LASSO_SELECT) {
                LassoSelectToolState lasso = LassoSelectToolState.INSTANCE;
                if (lasso.dragging && lasso.polygonPoints.size() >= 2) {
                    renderLassoPolygon(lasso);
                }
            }
            renderCursor(mx, my);
        }

        // 10th slot is hidden while the editor overlay is active (viewport owns the screen).
        if (!DimensiumMode.INSTANCE.isActive()) renderTenthSlot(mc, sr, sw, sh);

    }

    /**
     * Renders the 10th hotbar slot using the vanilla widgets.png texture so it
     * matches the native slot appearance. GL scissor limits the draw to a single
     * 20×22 slot cell; when builder tools mode is active the 24×24 selected-slot
     * highlight is drawn on top, identical to how GuiIngame highlights slot 0-8.
     *
     * Vanilla hotbar layout (widgets.png, 256×256):
     * - Background strip: (0,0) 182×22
     * - Slot i left edge in texture: 1 + i*20
     * - Selected highlight: (0,22) 24×24, positioned at (barX + i*20 - 1, barY - 1)
     *
     * Our 10th slot sits 4px to the right of the vanilla bar. We reuse the texture
     * for slot 8 (last slot) — its left edge is at texture x=161 — by drawing the
     * full strip at drawX = (slotX - 161) and scissoring to the slot area.
     */
    private void renderTenthSlot(Minecraft mc, ScaledResolution sr, int sw, int sh) {
        boolean active = DimensiumMode.INSTANCE.isBuilderToolsActive();
        BuilderToolState bts = BuilderToolState.INSTANCE;

        int barX = (sw - 182) / 2;
        int barY = sh - 22;

        // The slot is placed 4 scaled pixels to the right of the vanilla bar.
        // Composed from two texture regions:
        // - 1px left border: slot 0's outer left edge (texture x=0, w=1)
        // - 21px cell+right-border: slot 8 (texture x=161, w=21)
        // Total visual width: 22px. Cell starts at slotX+1.
        int slotX = barX + 182 + 4;
        int slotY = barY;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.renderEngine.bindTexture(WIDGETS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        // 1px left border from slot 0's outer left edge
        drawTexRect(slotX, slotY, 0, 0, 1, 22);
        // Slot 8 cell (20px) + bar's right border (1px) = 21px total
        drawTexRect(slotX + 1, slotY, 161, 0, 21, 22);

        // Selected-slot highlight: 24×24 at (0,22) in widgets.png.
        // Cell is at slotX+1, so highlight starts at slotX (1px left overhang).
        if (active) {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            drawTexRect(slotX, slotY - 1, 0, 22, 24, 24);
        }

        // Tool icon inside the slot cell (cell starts at slotX+1, 20×22)
        ItemStack icon = iconForTool(bts.activeTool);
        if (icon != null) {
            PanelDraw.renderItemIcon(mc, icon, slotX + 3, slotY + 3);
            GL11.glDisable(GL11.GL_LIGHTING);
        }

        // Phase-keyed label below the selected highlight (or inside the slot when inactive)
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);

        if (active) {
            String phaseSuffix = switch (bts.phase) {
                case SELECTING -> " \2476+\247r";
                case MANIPULATING -> " \247a>\247r";
                default -> "";
            };
            String extra = "";
            if (bts.phase == Phase.MANIPULATING && bts.activeTool.equals(BuilderTool.STACK)) {
                extra = " \247e" + bts.stackX + "," + bts.stackY + "," + bts.stackZ + "\247r";
            }
            // Draw tool name + phase above the bar (4px above it)
            mc.fontRenderer
                .drawStringWithShadow(bts.activeTool.label + phaseSuffix + extra, slotX + 1, slotY - 11, 0xFFFFFF);
        } else {
            // Hint text above slot when inactive
            mc.fontRenderer.drawStringWithShadow("\2477>\247r", slotX + 6, slotY - 10, 0x888888);
        }

        GL11.glPopAttrib();
    }

    /**
     * Draws a textured rectangle using the currently bound texture.
     * All coordinates are in scaled-pixel space.
     * The texture is assumed to be 256×256 (vanilla widgets.png).
     */
    private void drawTexRect(int x, int y, int u, int v, int w, int h) {
        float u0 = u / 256f, v0 = v / 256f;
        float u1 = (u + w) / 256f, v1 = (v + h) / 256f;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + h, 0, u0, v1);
        t.addVertexWithUV(x + w, y + h, 0, u1, v1);
        t.addVertexWithUV(x + w, y, 0, u1, v0);
        t.addVertexWithUV(x, y, 0, u0, v0);
        t.draw();
    }

    private static ItemStack iconForTool(BuilderTool tool) {
        return switch (tool) {
            case MOVE -> new ItemStack(Items.compass);
            case CLONE -> new ItemStack(Items.paper);
            case STACK -> new ItemStack(Blocks.brick_block);
            case SMEAR -> new ItemStack(Items.feather);
            case EXTRUDE -> new ItemStack(Blocks.piston);
            case ERASE -> new ItemStack(Items.flint_and_steel);
            case SETUP_SYMMETRY -> new ItemStack(Items.ender_pearl);
        };
    }

    private void renderLassoPolygon(LassoSelectToolState lasso) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.5f);

        Tessellator t = Tessellator.instance;

        // Drop shadow
        GL11.glColor4f(0f, 0f, 0f, 0.5f);
        t.startDrawing(GL11.GL_LINE_LOOP);
        for (float[] p : lasso.polygonPoints) t.addVertex(p[0] + 1, p[1] + 1, 0);
        t.draw();

        // Lasso outline in cyan-white
        GL11.glColor4f(0.4f, 0.9f, 0.8f, 0.9f);
        t.startDrawing(GL11.GL_LINE_LOOP);
        for (float[] p : lasso.polygonPoints) t.addVertex(p[0], p[1], 0);
        t.draw();

        // Fill tint
        GL11.glColor4f(0.4f, 0.9f, 0.8f, 0.07f);
        t.startDrawingQuads();
        float[] first = lasso.polygonPoints.get(0);
        for (int i = 1; i < lasso.polygonPoints.size() - 1; i++) {
            float[] a = lasso.polygonPoints.get(i);
            float[] b = lasso.polygonPoints.get(i + 1);
            t.addVertex(first[0], first[1], 0);
            t.addVertex(a[0], a[1], 0);
            t.addVertex(b[0], b[1], 0);
            t.addVertex(b[0], b[1], 0);
        }
        t.draw();

        GL11.glPopAttrib();
    }

    private void renderCursor(int cx, int cy) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.0f);

        Tessellator t = Tessellator.instance;

        if (ViewportPanel.INSTANCE.resizeCursorActive) {
            // E-W resize cursor: ←|→
            // Shadow
            GL11.glColor4f(0f, 0f, 0f, 0.6f);
            t.startDrawing(GL11.GL_LINES);
            // horizontal bar
            t.addVertex(cx - 8, cy + 1, 0);
            t.addVertex(cx + 9, cy + 1, 0);
            // vertical tick
            t.addVertex(cx + 1, cy - 3, 0);
            t.addVertex(cx + 1, cy + 4, 0);
            // left arrowhead
            t.addVertex(cx - 8, cy + 1, 0);
            t.addVertex(cx - 4, cy - 2, 0);
            t.addVertex(cx - 8, cy + 1, 0);
            t.addVertex(cx - 4, cy + 4, 0);
            // right arrowhead
            t.addVertex(cx + 9, cy + 1, 0);
            t.addVertex(cx + 5, cy - 2, 0);
            t.addVertex(cx + 9, cy + 1, 0);
            t.addVertex(cx + 5, cy + 4, 0);
            t.draw();
            // White
            GL11.glColor4f(1f, 1f, 1f, 0.95f);
            t.startDrawing(GL11.GL_LINES);
            t.addVertex(cx - 8, cy, 0);
            t.addVertex(cx + 9, cy, 0);
            t.addVertex(cx, cy - 3, 0);
            t.addVertex(cx, cy + 4, 0);
            t.addVertex(cx - 8, cy, 0);
            t.addVertex(cx - 4, cy - 3, 0);
            t.addVertex(cx - 8, cy, 0);
            t.addVertex(cx - 4, cy + 3, 0);
            t.addVertex(cx + 9, cy, 0);
            t.addVertex(cx + 5, cy - 3, 0);
            t.addVertex(cx + 9, cy, 0);
            t.addVertex(cx + 5, cy + 3, 0);
            t.draw();
        } else {
            // Drop shadow for visibility on any background.
            GL11.glColor4f(0f, 0f, 0f, 0.6f);
            t.startDrawing(GL11.GL_LINES);
            t.addVertex(cx - 7, cy + 1, 0);
            t.addVertex(cx + 7, cy + 1, 0);
            t.addVertex(cx + 1, cy - 7, 0);
            t.addVertex(cx + 1, cy + 7, 0);
            t.draw();

            // White crosshair.
            GL11.glColor4f(1f, 1f, 1f, 0.95f);
            t.startDrawing(GL11.GL_LINES);
            t.addVertex(cx - 7, cy, 0);
            t.addVertex(cx + 7, cy, 0);
            t.addVertex(cx, cy - 7, 0);
            t.addVertex(cx, cy + 7, 0);
            t.draw();

            // Centre dot — 3×3, symmetric around (cx, cy).
            GL11.glColor4f(1f, 1f, 1f, 1f);
            t.startDrawingQuads();
            t.addVertex(cx - 1, cy - 1, 0);
            t.addVertex(cx + 2, cy - 1, 0);
            t.addVertex(cx + 2, cy + 2, 0);
            t.addVertex(cx - 1, cy + 2, 0);
            t.draw();
        }

        GL11.glPopAttrib();
    }

    private static boolean defaultLayoutApplied = false;
    static boolean resetLayoutRequested = false;

    public static void requestResetLayout() {
        resetLayoutRequested = true;
    }

    private static void renderDockSpace(int sw, int sh) {
        float menuH = MenuBar.INSTANCE.height();
        float statusH = StatusBar.INSTANCE.height();
        ImGui.setNextWindowPos(0, menuH, imgui.flag.ImGuiCond.Always);
        ImGui.setNextWindowSize(sw, sh - menuH - statusH + 1, imgui.flag.ImGuiCond.Always);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.WindowBg, 0.13f, 0.13f, 0.13f, 1f);
        int dsFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
            | ImGuiWindowFlags.NoMove
            | ImGuiWindowFlags.NoBringToFrontOnFocus
            | ImGuiWindowFlags.NoFocusOnAppearing
            | ImGuiWindowFlags.NoNavFocus
            | ImGuiWindowFlags.NoScrollbar;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f);
        ImGui.begin("##dockspace_host", dsFlags);
        int dockspaceId = ImGui.getID("##main_dockspace");
        ImGui.dockSpace(dockspaceId, 0f, 0f, ImGuiDockNodeFlags.None);

        if (!defaultLayoutApplied && !new java.io.File("dimensium_layout.ini").exists()) {
            defaultLayoutApplied = true;
            DockDefaultLayout.apply(dockspaceId, sw, sh - menuH - statusH);
        }

        if (resetLayoutRequested) {
            resetLayoutRequested = false;
            ImGui.loadIniSettingsFromMemory("");
            new java.io.File("dimensium_layout.ini").delete(); // best-effort: ignored if missing
            DockDefaultLayout.apply(dockspaceId, sw, sh - menuH - statusH);
        }

        ImGui.end();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor();
    }

    private static void renderKeyPressLog(int sw, int sh) {
        ViewState vs = ViewState.INSTANCE;
        if (!vs.showKeyPresses) return;

        java.util.Deque<ViewState.KeyPressEntry> log = vs.keyLog();
        if (log.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Remove fully faded entries.
        while (!log.isEmpty() && now - log.peekFirst().timeMs > ViewState.FADE_MS) {
            log.pollFirst();
        }
        if (log.isEmpty()) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        float padH = 6f * scale;
        float lineH = ImGui.getTextLineHeight();
        float winW = 200f * scale;
        float winH = log.size() * (lineH + padH) + padH;

        ImGui.setNextWindowPos(sw - winW - 8f * scale, sh - winH - 8f * scale);
        ImGui.setNextWindowSize(winW, winH);
        ImGui.setNextWindowBgAlpha(0.55f);

        int winFlags = imgui.flag.ImGuiWindowFlags.NoDecoration | imgui.flag.ImGuiWindowFlags.NoInputs
            | imgui.flag.ImGuiWindowFlags.NoNav
            | imgui.flag.ImGuiWindowFlags.NoMove
            | imgui.flag.ImGuiWindowFlags.NoBringToFrontOnFocus
            | imgui.flag.ImGuiWindowFlags.NoFocusOnAppearing;

        ImGui.begin("##key_press_log", winFlags);
        for (ViewState.KeyPressEntry entry : log) {
            float age = (now - entry.timeMs) / (float) ViewState.FADE_MS;
            float alpha = 1.0f - age * age;
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 1.0f, 1.0f, alpha);
            ImGui.text(entry.label);
            ImGui.popStyleColor();
        }
        ImGui.end();
    }
}
