/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
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
import github.thehighcruw.dimensium.render.brushes.ToolRenderer;
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
import github.thehighcruw.dimensium.render.popup.LayoutPresetManageWindow;
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
import github.thehighcruw.dimensium.render.world.PlaneTranslationGizmo;
import github.thehighcruw.dimensium.render.world.RotationGizmo;
import github.thehighcruw.dimensium.render.world.ScaleGizmo;
import github.thehighcruw.dimensium.render.world.TranslationGizmo;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.ToolRegistry;
import github.thehighcruw.dimensium.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.tool.state.MoveToolState;
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

            toolPanel.updateMouse(mx, my);

            ShapePlacementState ps = ShapePlacementState.INSTANCE;
            if (ps.active && !ps.gizmo.isDragging()
                && !ps.rotGizmo.isDragging()
                && !ps.scaleGizmo.isDragging()
                && !ps.planeGizmo.isDragging()
                && !ps.viewPlaneGizmo.isDragging()
                && mc.renderViewEntity != null) {
                net.minecraft.entity.EntityLivingBase eye = mc.renderViewEntity;
                double cx = ps.centerX(), cy = ps.centerY(), cz = ps.centerZ();
                ps.viewPlaneGizmo.updateHover(mx, my, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                if (!ps.viewPlaneGizmo.hovered) {
                    ps.gizmo.updateHover(mx, my, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                    if (ps.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                        ps.scaleGizmo.updateHover(mx, my, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                        if (ps.scaleGizmo.hoveredAxis == ScaleGizmo.Axis.NONE) {
                            ps.rotGizmo.updateHover(mx, my, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                            if (ps.rotGizmo.hoveredAxis == RotationGizmo.Axis.NONE) {
                                ps.planeGizmo.updateHover(mx, my, sw, sh, eye, cx, cy, cz, ps.rotX, ps.rotY, ps.rotZ);
                            } else {
                                ps.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                            }
                        } else {
                            ps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                            ps.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                        }
                    } else {
                        ps.scaleGizmo.hoveredAxis = ScaleGizmo.Axis.NONE;
                        ps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                        ps.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                    }
                } else {
                    ps.gizmo.hoveredAxis = TranslationGizmo.Axis.NONE;
                    ps.scaleGizmo.hoveredAxis = ScaleGizmo.Axis.NONE;
                    ps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                    ps.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                }
            }

            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            if (cps.active && !cps.gizmo.isDragging()
                && !cps.planeGizmo.isDragging()
                && !cps.rotGizmo.isDragging()
                && mc.renderViewEntity != null) {
                net.minecraft.entity.EntityLivingBase cEye = mc.renderViewEntity;
                double ccx = cps.centerX(), ccy = cps.centerY(), ccz = cps.centerZ();
                cps.gizmo.updateHover(mx, my, sw, sh, cEye, ccx, ccy, ccz, 0, 0, 0);
                if (cps.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                    cps.planeGizmo.updateHover(mx, my, sw, sh, cEye, ccx, ccy, ccz, cps.rotX, cps.rotY, cps.rotZ);
                    if (cps.planeGizmo.hoveredPlane == PlaneTranslationGizmo.Plane.NONE) {
                        cps.rotGizmo.updateHover(mx, my, sw, sh, cEye, ccx, ccy, ccz, cps.rotX, cps.rotY, cps.rotZ);
                    } else {
                        cps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                    }
                } else {
                    cps.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                    cps.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                }
            }

            MoveToolState ms = MoveToolState.INSTANCE;
            if (ms.active && !ms.gizmo.isDragging()
                && !ms.planeGizmo.isDragging()
                && !ms.rotGizmo.isDragging()
                && mc.renderViewEntity != null) {
                net.minecraft.entity.EntityLivingBase eye = mc.renderViewEntity;
                double gx = ms.gizmoX(), gy = ms.gizmoY(), gz = ms.gizmoZ();
                ms.gizmo.updateHover(mx, my, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
                if (ms.gizmo.hoveredAxis == TranslationGizmo.Axis.NONE) {
                    ms.planeGizmo.updateHover(mx, my, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
                    if (ms.planeGizmo.hoveredPlane == PlaneTranslationGizmo.Plane.NONE) {
                        ms.rotGizmo.updateHover(mx, my, sw, sh, eye, gx, gy, gz, ms.rotX, ms.rotY, ms.rotZ);
                    } else {
                        ms.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                    }
                } else {
                    ms.planeGizmo.hoveredPlane = PlaneTranslationGizmo.Plane.NONE;
                    ms.rotGizmo.hoveredAxis = RotationGizmo.Axis.NONE;
                }
            }

            // ── Box-select commit on tool change ─────────────────────────────
            SelectionState bxSel = SelectionState.INSTANCE;
            if (bxSel.boxConfirmed && DimensiumMode.INSTANCE.selectedTool != Tool.SELECT) {
                GuiDimensiumOverlay.commitBoxSelection(bxSel, SelectToolState.INSTANCE);
            }

            // ── Per-tool overlay (gizmos, 2D overlays) ────────────────────────
            Tool activeTool = DimensiumMode.INSTANCE.selectedTool;
            ToolRenderer toolRenderer = ToolRegistry.toolRenderer(activeTool);
            toolRenderer.renderOverlay(mc, mx, my, mx, my, sw, sh);

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
            LayoutPresetManageWindow.INSTANCE.renderImGui();
            MenuBar.INSTANCE.renderPopups();
            StatusBar.INSTANCE.render(mc.displayWidth, mc.displayHeight);
            renderKeyPressLog(mc.displayWidth, mc.displayHeight);
            ImGuiManager.INSTANCE.endFrame();

            // Rebake clipboard FBO after ImGui has rendered — result used next frame.
            // Running before endFrame() risks corrupting GL state that renderDrawData() needs.
            ClipboardWindow.INSTANCE.prebake();

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

        int winFlags = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoInputs
            | ImGuiWindowFlags.NoNav
            | ImGuiWindowFlags.NoMove
            | ImGuiWindowFlags.NoBringToFrontOnFocus
            | ImGuiWindowFlags.NoFocusOnAppearing;

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
