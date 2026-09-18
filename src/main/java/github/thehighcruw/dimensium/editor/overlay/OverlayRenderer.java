/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.ToolRegistry;
import github.thehighcruw.dimensium.editor.tool.ToolRenderer;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithAxisTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithPlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.tool.gizmo.WithRotationGizmo;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.AnalyzeWindow;
import github.thehighcruw.dimensium.editor.window.AutoshadeWindow;
import github.thehighcruw.dimensium.editor.window.BlockInfoWindow;
import github.thehighcruw.dimensium.editor.window.ClipboardWindow;
import github.thehighcruw.dimensium.editor.window.ColourFieldWindow;
import github.thehighcruw.dimensium.editor.window.DistortSelectionWindow;
import github.thehighcruw.dimensium.editor.window.FillSelectionWindow;
import github.thehighcruw.dimensium.editor.window.FilterSelectionWindow;
import github.thehighcruw.dimensium.editor.window.HistoryWindow;
import github.thehighcruw.dimensium.editor.window.LayoutPresetManageWindow;
import github.thehighcruw.dimensium.editor.window.OperationsWindow;
import github.thehighcruw.dimensium.editor.window.PaletteEditorWindow;
import github.thehighcruw.dimensium.editor.window.PaletteWindow;
import github.thehighcruw.dimensium.editor.window.ReplaceSelectionWindow;
import github.thehighcruw.dimensium.editor.window.SelectionWindow;
import github.thehighcruw.dimensium.editor.window.SmoothSelectionWindow;
import github.thehighcruw.dimensium.editor.window.ToolMaskEditorWindow;
import github.thehighcruw.dimensium.editor.window.ToolMaskListWindow;
import github.thehighcruw.dimensium.editor.window.ToolOptionsWindow;
import github.thehighcruw.dimensium.editor.window.ToolWindow;
import github.thehighcruw.dimensium.editor.window.TypeReplaceSelectionWindow;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.panel.PanelDraw;
import github.thehighcruw.dimensium.editor.window.popup.BlockPickerPopup;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.editor.window.popup.ConflictPopup;
import github.thehighcruw.dimensium.editor.window.popup.CreateBlueprintPopup;
import github.thehighcruw.dimensium.editor.window.popup.SettingsModal;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportCapture;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportPanel;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportRegistry;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportState;
import github.thehighcruw.dimensium.editor.window.viewport.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.RotationGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.ScalingGizmo;
import github.thehighcruw.dimensium.editor.window.viewport.world.TranslationGizmo;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import java.io.File;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class OverlayRenderer {

    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    public static final ToolWindow TOOL_WINDOW = new ToolWindow();
    public static final ToolOptionsWindow TOOL_OPTIONS_WINDOW = new ToolOptionsWindow(TOOL_WINDOW);
    public static final BlockPickerPopup picker = new BlockPickerPopup();

    private int savedCurrentItem = 0;

    public static boolean isNotCreative() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer == null || !mc.thePlayer.capabilities.isCreativeMode;
    }

    /**
     * Prevent the local player entity from rendering while the editor is active.
     * setInvisible(true) reduces body opacity but renderEquippedItems (held item) ignores it.
     * Cancelling Pre entirely hides both the body and the held item from the freecam viewport.
     */
    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (FreecamState.INSTANCE.active) event.setCanceled(true);
    }

    /**
     * Cancel all vanilla HUD elements while the Dimensium editor overlay is active.
     * The editor draws its own UI via ImGui; vanilla HUD would render on top of the viewport.
     */
    @SubscribeEvent
    public void onHudPre(RenderGameOverlayEvent.Pre event) {
        if (!DimensiumEditorMode.INSTANCE.isActive()) return;
        if (isNotCreative()) return;
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
        if (areBuilderToolsInactive(event)) return;

        Minecraft mc = Minecraft.getMinecraft();
        savedCurrentItem = mc.thePlayer.inventory.currentItem;
        mc.thePlayer.inventory.currentItem = -100;
    }

    /** After the vanilla hotbar renders: restore currentItem. */
    @SubscribeEvent
    public void onHotbarPost(RenderGameOverlayEvent.Post event) {
        if (areBuilderToolsInactive(event)) return;

        Minecraft mc = Minecraft.getMinecraft();
        mc.thePlayer.inventory.currentItem = savedCurrentItem;
    }

    private boolean areBuilderToolsInactive(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return true;
        if (DimensiumEditorMode.INSTANCE.isActive()) return true;
        if (!DimensiumEditorMode.INSTANCE.isBuilderToolsActive()) return true;

        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer == null || isNotCreative();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent event) {
        if (event.isCancelable() || event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        // Restore hideGUI set in RenderTickEvent.START to suppress the first-person arm.
        // renderHand already fired at this point, so restoration is safe.
        if (FreecamState.INSTANCE.active) mc.gameSettings.hideGUI = false;
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        if (isNotCreative()) return;

        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        if (DimensiumEditorMode.INSTANCE.isActive()) {
            // Use software cursor position (tracked in TickHandler from mouse delta).
            FreecamState fs = FreecamState.INSTANCE;
            int mx = (int) fs.cursorX;
            int my = (int) fs.cursorY;

            ShapePlacementState ps = ShapePlacementState.INSTANCE;
            if (ps.active && !ps.isAnyGizmoDragging() && mc.renderViewEntity != null) {
                Vec3DDouble psCenter = ps.center();
                updateShapeGizmoHover(ps, mx, my, mc.renderViewEntity, psCenter);
            }

            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            if (cps.active && !cps.isAnyGizmoDragging() && mc.renderViewEntity != null) {
                EntityLivingBase cEye = mc.renderViewEntity;
                Vec3DDouble cpsCenter = cps.center();
                cps.getAxisTranslationGizmo().updateHover(mx, my, cEye, cpsCenter, Vec3DFloat.ZERO);
                handleGizmoHover(cps, mx, my, cEye, cpsCenter, cps.rot);
            }

            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active && !ms.isAnyGizmoDragging() && mc.renderViewEntity != null) {
                EntityLivingBase eye = mc.renderViewEntity;
                Vec3DDouble gizmoPos = ms.gizmoPos();
                ms.getAxisTranslationGizmo().updateHover(mx, my, eye, gizmoPos, ms.rot);
                handleGizmoHover(ms, mx, my, eye, gizmoPos, ms.rot);
            }

            // ── Box-select commit on tool change ─────────────────────────────
            SelectionState bxSel = SelectionState.INSTANCE;
            if (bxSel.boxConfirmed && DimensiumEditorMode.INSTANCE.selectedTool != Tool.SELECT) {
                GuiDimensiumOverlay.commitBoxSelection(bxSel, BoxSelectToolState.INSTANCE);
            }

            ViewportRegistry.INSTANCE.flushPendingDeletions();

            // Capture world render into the active viewport's texture before ImGui overdraw.
            // Inactive viewports keep their last captured frame — only the active tab renders.
            ViewportState activeVp = ViewportRegistry.INSTANCE.active();
            if (activeVp != null) ViewportCapture.capture(activeVp, mc.displayWidth, mc.displayHeight);

            // ImGui display space = physical pixels. Panels sized in physical px.
            int sf = sr.getScaleFactor();
            ImGuiManager.INSTANCE.newFrame(mc.displayWidth, mc.displayHeight, sf, fs.cursorX, fs.cursorY);
            MenuBar.INSTANCE.render();
            renderDockSpace(mc.displayWidth, mc.displayHeight);
            ViewportPanel.INSTANCE.render(mc.displayWidth, mc.displayHeight);
            TOOL_WINDOW.render(mc.displayHeight);
            TOOL_OPTIONS_WINDOW.render(mc.displayHeight);
            picker.renderImGui();
            ConflictPopup.INSTANCE.renderImGui();
            CreateBlueprintPopup.INSTANCE.renderImGui();
            BlueprintBrowserPopup.INSTANCE.renderImGui();
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
            PaletteWindow.INSTANCE.renderImGui();
            PaletteEditorWindow.INSTANCE.renderImGui();
            HistoryWindow.INSTANCE.renderImGui();
            LayoutPresetManageWindow.INSTANCE.renderImGui();
            MenuBar.INSTANCE.renderPopups();
            StatusBar.INSTANCE.render(mc.displayWidth, mc.displayHeight);
            renderKeyPressLog(mc.displayWidth, mc.displayHeight);
            ImGuiManager.INSTANCE.endFrame();

            // Rebake clipboard FBO after ImGui has rendered — result used next frame.
            // Running before endFrame() risks corrupting GL state that renderDrawData() needs.
            ClipboardWindow.INSTANCE.prebake();

            // ── Per-tool overlay (gizmos, 2D overlays) ────────────────────────
            // Rendered after ImGui so the overlay is not captured into the viewport texture.
            Tool activeTool = DimensiumEditorMode.INSTANCE.selectedTool;
            ToolRenderer toolRenderer = ToolRegistry.toolRenderer(activeTool);
            toolRenderer.renderOverlay(mc, mx, my, mx, my);

            renderCursor(mx, my);
        }

        // 10th slot is hidden while the editor overlay is active (viewport owns the screen).
        if (!DimensiumEditorMode.INSTANCE.isActive()) renderTenthSlot(mc, sw, sh);
    }

    private static void updateShapeGizmoHover(
            ShapePlacementState ps, int mx, int my, EntityLivingBase eye, Vec3DDouble pos) {
        ps.viewPlaneGizmo.updateHover(mx, my, eye, pos);
        if (ps.viewPlaneGizmo.hovered) {
            ps.getAxisTranslationGizmo().hoveredAxis = TranslationGizmo.Axis.NONE;
            ps.getScalingGizmo().hoveredAxis = ScalingGizmo.Axis.NONE;
            ps.getRotationGizmo().hoveredAxis = RotationGizmo.Axis.NONE;
            ps.getPlaneTranslationGizmo().hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
            return;
        }
        ps.getAxisTranslationGizmo().updateHover(mx, my, eye, pos, ps.rot);
        if (ps.getAxisTranslationGizmo().hoveredAxis != TranslationGizmo.Axis.NONE) {
            ps.getScalingGizmo().hoveredAxis = ScalingGizmo.Axis.NONE;
            ps.getRotationGizmo().hoveredAxis = RotationGizmo.Axis.NONE;
            ps.getPlaneTranslationGizmo().hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
            return;
        }
        ps.getScalingGizmo().updateHover(mx, my, eye, pos, ps.rot);
        if (ps.getScalingGizmo().hoveredAxis != ScalingGizmo.Axis.NONE) {
            ps.getRotationGizmo().hoveredAxis = RotationGizmo.Axis.NONE;
            ps.getPlaneTranslationGizmo().hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
            return;
        }
        ps.getRotationGizmo().updateHover(mx, my, eye, pos, ps.rot);
        if (ps.getRotationGizmo().hoveredAxis != RotationGizmo.Axis.NONE) {
            ps.getPlaneTranslationGizmo().hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
            return;
        }
        ps.getPlaneTranslationGizmo().updateHover(mx, my, eye, pos, ps.rot);
    }

    private static <T extends WithAxisTranslationGizmo & WithPlaneTranslationGizmo & WithRotationGizmo>
            void handleGizmoHover(T ms, int mx, int my, EntityLivingBase eye, Vec3DDouble pos, Vec3DFloat rot) {
        if (ms.getAxisTranslationGizmo().hoveredAxis == TranslationGizmo.Axis.NONE) {
            ms.getPlaneTranslationGizmo().updateHover(mx, my, eye, pos, rot);
            if (ms.getPlaneTranslationGizmo().hoveredPlane == PlaneTranslationGizmo.Plane.NONE) {
                ms.getRotationGizmo().updateHover(mx, my, eye, pos, rot);
            } else {
                ms.getRotationGizmo().hoveredAxis = RotationGizmo.Axis.NONE;
            }
        } else {
            ms.getPlaneTranslationGizmo().hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
            ms.getRotationGizmo().hoveredAxis = RotationGizmo.Axis.NONE;
        }
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
    private void renderTenthSlot(Minecraft mc, int sw, int sh) {
        boolean active = DimensiumEditorMode.INSTANCE.isBuilderToolsActive();
        BuilderToolState bts = BuilderToolState.INSTANCE;

        int barX = (sw - 182) / 2;
        int barY = sh - 22;

        // The slot is placed 4 scaled pixels to the right of the vanilla bar.
        // Composed from two texture regions:
        // - 1px left border: slot 0's outer left edge (texture x=0, w=1)
        // - 21px cell+right-border: slot 8 (texture x=161, w=21)
        // Total visual width: 22px. Cell starts at slotX+1.
        int slotX = barX + 182 + 4;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.renderEngine.bindTexture(WIDGETS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        // 1px left border from slot 0's outer left edge
        drawTexRect(slotX, barY, 0, 0, 1, 22);
        // Slot 8 cell (20px) + bar's right border (1px) = 21px total
        drawTexRect(slotX + 1, barY, 161, 0, 21, 22);

        // Selected-slot highlight: 24×24 at (0,22) in widgets.png.
        // Cell is at slotX+1, so highlight starts at slotX (1px left overhang).
        if (active) {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            drawTexRect(slotX, barY - 1, 0, 22, 24, 24);
        }

        // Tool icon inside the slot cell (cell starts at slotX+1, 20×22)
        ItemStack icon = iconForTool(bts.activeTool);
        PanelDraw.renderItemIcon(mc, icon, slotX + 3, barY + 3);
        GL11.glDisable(GL11.GL_LIGHTING);

        // Phase-keyed label below the selected highlight (or inside the slot when inactive)
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);

        if (active) {
            String phaseSuffix =
                    switch (bts.phase) {
                        case SELECTING -> " \2476+\247r";
                        case MANIPULATING -> " \247a>\247r";
                        case CONFIRMING -> " \247c✘?\247r";
                        default -> "";
                    };
            String extra = "";
            if (bts.phase == Phase.MANIPULATING && bts.activeTool.equals(BuilderTool.STACK)) {
                extra = " \247e" + bts.stack.x() + "," + bts.stack.y() + "," + bts.stack.z() + "\247r";
            }
            // Draw tool name + phase above the bar (4px above it)
            mc.fontRenderer.drawStringWithShadow(
                    bts.activeTool.label + phaseSuffix + extra, slotX + 1, barY - 11, 0xFFFFFF);
        } else {
            // Hint text above slot when inactive
            mc.fontRenderer.drawStringWithShadow("\2477>\247r", slotX + 6, barY - 10, 0x888888);
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
        ImGui.setNextWindowPos(0, menuH, ImGuiCond.Always);
        ImGui.setNextWindowSize(sw, sh - menuH - statusH + 1, ImGuiCond.Always);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.13f, 0.13f, 0.13f, 1f);
        int dsFlags = ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoResize
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

        if (!defaultLayoutApplied && !new File("dimensium_layout.ini").exists()) {
            defaultLayoutApplied = true;
            DockDefaultLayout.apply(dockspaceId, sw, sh - menuH - statusH);
        }

        if (resetLayoutRequested) {
            resetLayoutRequested = false;
            ImGui.loadIniSettingsFromMemory("");
            boolean ignored = new File("dimensium_layout.ini").delete(); // best-effort: ignored if missing
            DockDefaultLayout.apply(dockspaceId, sw, sh - menuH - statusH);
        }

        ImGui.end();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor();
    }

    private static void renderKeyPressLog(int sw, int sh) {
        ViewState vs = ViewState.INSTANCE;
        if (!vs.showKeyPresses) return;

        Deque<ViewState.KeyPressEntry> log = vs.keyLog();
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

        int winFlags = ImGuiWindowFlags.NoDecoration
                | ImGuiWindowFlags.NoInputs
                | ImGuiWindowFlags.NoNav
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoFocusOnAppearing;

        ImGui.begin("##key_press_log", winFlags);
        for (ViewState.KeyPressEntry entry : log) {
            float age = (now - entry.timeMs) / (float) ViewState.FADE_MS;
            float alpha = 1.0f - age * age;
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, alpha);
            ImGui.text(entry.label);
            ImGui.popStyleColor();
        }
        ImGui.end();
    }
}
