/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.render.world;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.freecam.FreecamUtils;
import github.thehighcruw.dimensium.handler.BuilderToolsHandler;
import github.thehighcruw.dimensium.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.handler.TickHandler;
import github.thehighcruw.dimensium.handler.brushes.StampBrushInput;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.MenuBar;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.render.ViewState;
import github.thehighcruw.dimensium.render.ViewportPanel;
import github.thehighcruw.dimensium.render.brushes.BrushViewRegistry;
import github.thehighcruw.dimensium.render.imgui.ImGuiManager;
import github.thehighcruw.dimensium.tool.ActiveDragState;
import github.thehighcruw.dimensium.tool.BlockColorCache;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.DimensiumMode;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.brushes.ElevationBrush;
import github.thehighcruw.dimensium.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.tool.state.ElevationToolState;
import github.thehighcruw.dimensium.tool.state.GradientToolState;
import github.thehighcruw.dimensium.tool.state.MagicSelectToolState;
import github.thehighcruw.dimensium.tool.state.MoveToolState;
import github.thehighcruw.dimensium.tool.state.PathToolState;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectedBlockState;
import github.thehighcruw.dimensium.tool.state.SelectionState;
import github.thehighcruw.dimensium.tool.state.ShapePlacementState;
import github.thehighcruw.dimensium.util.PerfTrace;

@SideOnly(Side.CLIENT)
public class SelectionRenderer {

    public static final SelectionRenderer INSTANCE = new SelectionRenderer();

    private static final FloatBuffer PROJ_BUF = BufferUtils.createFloatBuffer(16);

    // Box-select gizmos — package-private so GuiDimensiumOverlay/OverlayRenderer can access them.
    public static final ViewPlaneGizmo boxPos1ViewPlaneGizmo = new ViewPlaneGizmo();
    public static final ViewPlaneGizmo boxPos2ViewPlaneGizmo = new ViewPlaneGizmo();
    public static final TranslationGizmo boxPos1Gizmo = new TranslationGizmo();
    public static final TranslationGizmo boxPos2Gizmo = new TranslationGizmo();

    // Center gizmos — move the entire box (both corners) together.
    public static final ViewPlaneGizmo boxCenterViewPlaneGizmo = new ViewPlaneGizmo();
    public static final TranslationGizmo boxCenterGizmo = new TranslationGizmo();
    // Corner positions captured at the start of a center-gizmo drag.
    public int boxCenterDragP1X, boxCenterDragP1Y, boxCenterDragP1Z;
    public int boxCenterDragP2X, boxCenterDragP2Y, boxCenterDragP2Z;

    private final BrushPreviewRenderer brushPreview = new BrushPreviewRenderer();
    private final HologramRenderer hologram = new HologramRenderer();

    private long cachedSelVersion = -1;
    private int[] cachedSelWire = null;

    // Magic select preview dedup — rebuild only when cursor moves to a new block.
    private int lastMagicX = Integer.MIN_VALUE;
    private int lastMagicY = Integer.MIN_VALUE;
    private int lastMagicZ = Integer.MIN_VALUE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        PerfTrace.begin("onRenderWorldLast");
        PerfTrace.push("BlockColorCache.init");
        BlockColorCache.INSTANCE.init();
        PerfTrace.pop();
        PROJ_BUF.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJ_BUF);
        float m0 = PROJ_BUF.get(0), m5 = PROJ_BUF.get(5);
        if (m0 > 0 && m5 > 0) {
            FreecamState.INSTANCE.projTanHX = 1.0f / m0;
            FreecamState.INSTANCE.projTanHY = 1.0f / m5;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        float pt = event.partialTicks;
        net.minecraft.entity.Entity cam = mc.renderViewEntity != null ? mc.renderViewEntity : player;
        double rx = cam.lastTickPosX + (cam.posX - cam.lastTickPosX) * pt;
        double ry = cam.lastTickPosY + (cam.posY - cam.lastTickPosY) * pt;
        double rz = cam.lastTickPosZ + (cam.posZ - cam.lastTickPosZ) * pt;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(1.5f);

        // ── Brush cursor preview ──────────────────────────────────────────────
        PerfTrace.push("brushCursorPreview");
        Tool _previewTool = DimensiumMode.INSTANCE.selectedTool;
        boolean _anyModal = ImGuiManager.INSTANCE.anyModalOpen()
            || github.thehighcruw.dimensium.render.popup.FilterSelectionWindow.INSTANCE.isOpen()
            || github.thehighcruw.dimensium.render.popup.DistortSelectionWindow.INSTANCE.isOpen()
            || github.thehighcruw.dimensium.render.popup.SmoothSelectionWindow.INSTANCE.isOpen();
        net.minecraft.client.gui.ScaledResolution _sr = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        float _sf = _sr.getScaleFactor();
        float _mx = FreecamState.INSTANCE.cursorX * _sf;
        float _my = FreecamState.INSTANCE.cursorY * _sf;
        boolean _mouseOverOtherPanel = OverlayRenderer.toolPanel.containsMouse(_mx, _my)
            || MenuBar.INSTANCE.containsMouse(_mx, _my, mc.displayWidth);
        boolean _cursorOnViewport = !_mouseOverOtherPanel
            && (!ImGuiManager.INSTANCE.wantCaptureMouse() || ViewportPanel.INSTANCE.isHovered());
        if (DimensiumMode.INSTANCE.isActive() && !_anyModal
            && _cursorOnViewport
            && (!TickHandler.INSTANCE.isPaintDragging() || _previewTool == Tool.SMOOTH)
            && BrushViewRegistry.hasBrushPreview(_previewTool)) {
            brushPreview.render(mc, rx, ry, rz);
        }
        if (DimensiumMode.INSTANCE.isActive() && !_anyModal
            && _cursorOnViewport
            && DimensiumMode.INSTANCE.selectedTool == Tool.ELEVATION) {
            renderElevationPreview(mc, rx, ry, rz);
        }

        // ── Gradient pos1 → cursor line ───────────────────────────────────────
        if (DimensiumMode.INSTANCE.isActive() && DimensiumMode.INSTANCE.selectedTool == Tool.GRADIENT) {
            GradientToolState gs = GradientToolState.INSTANCE;
            if (gs.gradientHasPos1) {
                net.minecraft.client.gui.ScaledResolution gsr = new net.minecraft.client.gui.ScaledResolution(
                    mc,
                    mc.displayWidth,
                    mc.displayHeight);
                MovingObjectPosition gmop = GuiDimensiumOverlay.raycastFromMouse(
                    (int) FreecamState.INSTANCE.cursorX,
                    (int) FreecamState.INSTANCE.cursorY,
                    gsr.getScaledWidth(),
                    gsr.getScaledHeight());
                if (gmop != null && gmop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    double p1x = gs.gradientPos1X + 0.5 - rx;
                    double p1y = gs.gradientPos1Y + 0.5 - ry;
                    double p1z = gs.gradientPos1Z + 0.5 - rz;
                    double p2x = gmop.blockX + 0.5 - rx;
                    double p2y = gmop.blockY + 0.5 - ry;
                    double p2z = gmop.blockZ + 0.5 - rz;
                    GL11.glColor4f(0.6f, 0.3f, 1.0f, 0.9f);
                    WorldLines.setEye(0, 0, 0); // vertices already camera-relative
                    Tessellator gTess = Tessellator.instance;
                    gTess.startDrawingQuads();
                    WorldLines.addSegment(gTess, p1x, p1y, p1z, p2x, p2y, p2z, WorldLines.W_SEL);
                    gTess.draw();
                    GL11.glPushMatrix();
                    GL11.glTranslated(gs.gradientPos1X - rx, gs.gradientPos1Y - ry, gs.gradientPos1Z - rz);
                    GL11.glColor4f(0.6f, 0.3f, 1.0f, 1.0f);
                    WorldLines
                        .setEyeForTranslation(gs.gradientPos1X - rx, gs.gradientPos1Y - ry, gs.gradientPos1Z - rz);
                    drawBox(0, 0, 0, 1, 1, 1);
                    GL11.glPopMatrix();
                }
            }
        }

        PerfTrace.pop();
        // ── Proposal previews (fill / extrude / shape / move / drag stroke) ────
        PerfTrace.push("proposalPreviews");
        if (DimensiumMode.INSTANCE.isActive()) {
            Tool tool = DimensiumMode.INSTANCE.selectedTool;
            BuilderToolState bts0 = BuilderToolState.INSTANCE;
            if (tool == Tool.EXTRUDE) ExtrudeHelper.INSTANCE.buildExtrudeProposal(mc);
            else {
                bts0.extrudePreview = null;
                ExtrudeHelper.INSTANCE.resetExtrudeDedup();
            }

            if (tool == Tool.MAGIC_SELECT && _cursorOnViewport) updateMagicSelectPreview(mc);
            else {
                bts0.magicPreview = null;
                lastMagicX = Integer.MIN_VALUE;
            }

            // Rebuild shape proposal before rendering so ShapePlacementState.preview is current.
            ShapePlacementState ps0 = ShapePlacementState.INSTANCE;
            if (ps0.active && tool == Tool.SHAPE) ps0.rebuildIfNeeded();

            if (tool == Tool.PATH) {
                PathToolState pathState = PathToolState.INSTANCE;
                pathState.rebuildIfNeeded(SelectedBlockState.INSTANCE.selectedBlock);
            }

            if (tool == Tool.MODELLING) {
                github.thehighcruw.dimensium.tool.state.ModellingToolState mts = github.thehighcruw.dimensium.tool.state.ModellingToolState.INSTANCE;
                mts.rebuildIfNeeded(SelectedBlockState.INSTANCE.selectedBlock);
            }

            if (bts0.fillPreview != null) renderProposalPreview(mc, rx, ry, rz, bts0.fillPreview);
            if (bts0.extrudePreview != null) renderProposalPreview(mc, rx, ry, rz, bts0.extrudePreview);
            if (bts0.magicPreview != null) renderMagicPreview(mc, rx, ry, rz, bts0.magicPreview);
            if (ps0.preview != null) renderProposalPreview(mc, rx, ry, rz, ps0.preview);
            if (ActiveDragState.INSTANCE.activeDrag != null)
                renderProposalPreview(mc, rx, ry, rz, ActiveDragState.INSTANCE.activeDrag);
            if (tool == Tool.PATH && PathToolState.INSTANCE.preview != null)
                renderProposalPreview(mc, rx, ry, rz, PathToolState.INSTANCE.preview);
            if (tool == Tool.MODELLING
                && github.thehighcruw.dimensium.tool.state.ModellingToolState.INSTANCE.preview != null)
                renderProposalPreview(
                    mc,
                    rx,
                    ry,
                    rz,
                    github.thehighcruw.dimensium.tool.state.ModellingToolState.INSTANCE.preview);
            if (tool == Tool.STAMP && StampBrushInput.INSTANCE.dragPreview != null)
                renderProposalPreview(mc, rx, ry, rz, StampBrushInput.INSTANCE.dragPreview);
        }

        PerfTrace.pop();
        // ── Selection rendering ───────────────────────────────────────────────
        PerfTrace.push("selectionRender");
        SelectionState sel = SelectionState.INSTANCE;

        // Tool-change commit: if the user left SELECT while a box was confirmed, apply it now.
        if (sel.boxConfirmed && DimensiumMode.INSTANCE.selectedTool != Tool.SELECT) {
            SelectToolState bts = SelectToolState.INSTANCE;
            sel.applyOp(
                SelectionState
                    .aabbBlocks(sel.pendingX, sel.pendingY, sel.pendingZ, sel.pendingX2, sel.pendingY2, sel.pendingZ2),
                bts.booleanOp);
            sel.boxConfirmed = false;
            boxPos1ViewPlaneGizmo.reset();
            boxPos2ViewPlaneGizmo.reset();
            boxPos1Gizmo.reset();
            boxPos2Gizmo.reset();
        }

        // Active selection drawn first — teal box renders on top.
        if (sel.hasSelection() && ViewState.INSTANCE.showSelection) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 500.0);
            int count = sel.size();

            if (count <= DimensiumConfig.maxSelectionRenderBlocks) {
                // Opaque textured pass — polygon offset -3,-3 to win over world geometry.
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(-3.0f, -3.0f);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                mc.getTextureManager()
                    .bindTexture(TextureMap.locationBlocksTexture);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glFrontFace(GL11.GL_CW);

                GL11.glPushMatrix();
                GL11.glTranslated(-rx, -ry, -rz);

                Tessellator t = Tessellator.instance;
                Set<Long> selBlocks = sel.getSelectedBlocks();
                t.startDrawingQuads();
                int batched = 0;
                for (long key : selBlocks) {
                    int bx = SelectionState.unpackX(key);
                    int by = SelectionState.unpackY(key);
                    int bz = SelectionState.unpackZ(key);
                    Block b = mc.theWorld.getBlock(bx, by, bz);
                    if (b == null || b == Blocks.air || b.getRenderType() != 0) continue;
                    int meta = mc.theWorld.getBlockMetadata(bx, by, bz);
                    int tint = 0xFFFFFF;
                    try {
                        tint = b.colorMultiplier(mc.theWorld, bx, by, bz);
                    } catch (Exception ignored) {}
                    for (int face = 0; face < 6; face++) {
                        long nk = SelectionState.pack(
                            bx + GhostRenderer.NX[face],
                            by + GhostRenderer.NY[face],
                            bz + GhostRenderer.NZ[face]);
                        if (!selBlocks.contains(nk)) {
                            GhostRenderer.addTexturedFace(t, bx, by, bz, b, meta, face, tint);
                            if (++batched % 2048 == 0) {
                                t.draw();
                                t.startDrawingQuads();
                            }
                        }
                    }
                }
                t.draw();

                // Non-standard render type blocks: colored solid boxes.
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(0.65f, 0.80f, 1.0f, 1.0f);
                t.startDrawingQuads();
                batched = 0;
                for (long key : selBlocks) {
                    int bx = SelectionState.unpackX(key);
                    int by = SelectionState.unpackY(key);
                    int bz = SelectionState.unpackZ(key);
                    Block b = mc.theWorld.getBlock(bx, by, bz);
                    if (b != null && b != Blocks.air && b.getRenderType() != 0) {
                        for (int face = 0; face < 6; face++) {
                            long nk = SelectionState.pack(
                                bx + GhostRenderer.NX[face],
                                by + GhostRenderer.NY[face],
                                bz + GhostRenderer.NZ[face]);
                            if (!selBlocks.contains(nk)) {
                                GhostRenderer.addSingleFace(t, bx, by, bz, face);
                                if (++batched % 2048 == 0) {
                                    t.draw();
                                    t.startDrawingQuads();
                                }
                            }
                        }
                    }
                }
                t.draw();

                // Glow pass — slightly more negative offset than opaque so no z-fighting.
                // glDepthMask(false): glow quads never occlude each other at crease edges.
                GL11.glPolygonOffset(-4.0f, -4.0f);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(false);
                GL11.glColor4f(0.30f, 1.0f, 0.80f, 0.06f + 0.08f * pulse);
                t.startDrawingQuads();
                batched = 0;
                for (long key : selBlocks) {
                    int bx = SelectionState.unpackX(key);
                    int by = SelectionState.unpackY(key);
                    int bz = SelectionState.unpackZ(key);
                    Block b = mc.theWorld.getBlock(bx, by, bz);
                    if (b == null || b == Blocks.air) continue;
                    for (int face = 0; face < 6; face++) {
                        long nk = SelectionState.pack(
                            bx + GhostRenderer.NX[face],
                            by + GhostRenderer.NY[face],
                            bz + GhostRenderer.NZ[face]);
                        if (!selBlocks.contains(nk)) {
                            GhostRenderer.addSingleFace(t, bx, by, bz, face, 0.02f);
                            if (++batched % 2048 == 0) {
                                t.draw();
                                t.startDrawingQuads();
                            }
                        }
                    }
                }
                t.draw();
                GL11.glDepthMask(true);
                GL11.glDepthFunc(GL11.GL_LESS);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                GL11.glPopMatrix();
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                GL11.glPolygonOffset(0.0f, 0.0f);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_CULL_FACE);

                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.65f + 0.25f * pulse);
                GL11.glLineWidth(1.8f);
                updateSelWireframeCache(sel);
                drawSelWireframe(rx, ry, rz);
            } else {
                GL11.glPushMatrix();
                GL11.glTranslated(sel.minX() - rx, sel.minY() - ry, sel.minZ() - rz);
                WorldLines.setEyeForTranslation(sel.minX() - rx, sel.minY() - ry, sel.minZ() - rz);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.06f + pulse * 0.04f);
                drawFilledBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.85f);
                drawBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
                GL11.glPopMatrix();
            }

            GL11.glPushMatrix();
            GL11.glTranslated(sel.minX() - rx, sel.minY() - ry, sel.minZ() - rz);
            WorldLines.setEyeForTranslation(sel.minX() - rx, sel.minY() - ry, sel.minZ() - rz);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, pulse * 0.4f);
            drawBox(-0.01f, -0.01f, -0.01f, sel.width() + 0.01f, sel.height() + 0.01f, sel.depth() + 0.01f);
            GL11.glPopMatrix();
        }

        // ── Live drag preview: pos1 anchor + AABB to cursor ──────────────────
        boolean selectToolActive = DimensiumMode.INSTANCE.isActive()
            && DimensiumMode.INSTANCE.selectedTool == Tool.SELECT;
        boolean builderActive = DimensiumMode.INSTANCE.isBuilderToolsActive();
        if (!selectToolActive && !builderActive) {
            sel.pendingPos1 = false;
            sel.boxConfirmed = false;
        }
        if (sel.pendingPos1) {
            GL11.glPushMatrix();
            GL11.glTranslated(sel.pendingX - rx, sel.pendingY - ry, sel.pendingZ - rz);
            WorldLines.setEyeForTranslation(sel.pendingX - rx, sel.pendingY - ry, sel.pendingZ - rz);
            GL11.glColor4f(0.2f, 1.0f, 0.8f, 1.0f);
            drawBox(0, 0, 0, 1, 1, 1);
            GL11.glPopMatrix();

            MovingObjectPosition bxMop;
            if (builderActive && !DimensiumMode.INSTANCE.isActive()) {
                bxMop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
            } else {
                FreecamState bxFs = FreecamState.INSTANCE;
                net.minecraft.client.gui.ScaledResolution bxSr = new net.minecraft.client.gui.ScaledResolution(
                    mc,
                    mc.displayWidth,
                    mc.displayHeight);
                bxMop = GuiDimensiumOverlay.raycastFromMouse(
                    (int) bxFs.cursorX,
                    (int) bxFs.cursorY,
                    bxSr.getScaledWidth(),
                    bxSr.getScaledHeight());
            }
            if (bxMop != null && bxMop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int mnX = Math.min(sel.pendingX, bxMop.blockX);
                int mnY = Math.min(sel.pendingY, bxMop.blockY);
                int mnZ = Math.min(sel.pendingZ, bxMop.blockZ);
                int mxX = Math.max(sel.pendingX, bxMop.blockX) + 1;
                int mxY = Math.max(sel.pendingY, bxMop.blockY) + 1;
                int mxZ = Math.max(sel.pendingZ, bxMop.blockZ) + 1;
                GL11.glPushMatrix();
                GL11.glTranslated(mnX - rx, mnY - ry, mnZ - rz);
                WorldLines.setEyeForTranslation(mnX - rx, mnY - ry, mnZ - rz);
                GL11.glColor4f(0.2f, 1.0f, 0.8f, 0.55f);
                drawBox(0, 0, 0, mxX - mnX, mxY - mnY, mxZ - mnZ);
                GL11.glPopMatrix();
            }
        }

        // ── Box confirmed: frozen AABB + pos1/pos2 gizmos ────────────────────
        if (sel.boxConfirmed && DimensiumMode.INSTANCE.selectedTool == Tool.SELECT) {
            int mnX = Math.min(sel.pendingX, sel.pendingX2);
            int mnY = Math.min(sel.pendingY, sel.pendingY2);
            int mnZ = Math.min(sel.pendingZ, sel.pendingZ2);
            int mxX = Math.max(sel.pendingX, sel.pendingX2) + 1;
            int mxY = Math.max(sel.pendingY, sel.pendingY2) + 1;
            int mxZ = Math.max(sel.pendingZ, sel.pendingZ2) + 1;
            GL11.glPushMatrix();
            GL11.glTranslated(mnX - rx, mnY - ry, mnZ - rz);
            WorldLines.setEyeForTranslation(mnX - rx, mnY - ry, mnZ - rz);
            GL11.glColor4f(0.2f, 1.0f, 0.8f, 0.9f);
            drawBox(0, 0, 0, mxX - mnX, mxY - mnY, mxZ - mnZ);
            GL11.glPopMatrix();
            boxPos1Gizmo.axisFlip[0] = sel.pendingX <= sel.pendingX2 ? -1f : 1f;
            boxPos1Gizmo.axisFlip[1] = sel.pendingY <= sel.pendingY2 ? -1f : 1f;
            boxPos1Gizmo.axisFlip[2] = sel.pendingZ <= sel.pendingZ2 ? -1f : 1f;
            boxPos2Gizmo.axisFlip[0] = -boxPos1Gizmo.axisFlip[0];
            boxPos2Gizmo.axisFlip[1] = -boxPos1Gizmo.axisFlip[1];
            boxPos2Gizmo.axisFlip[2] = -boxPos1Gizmo.axisFlip[2];
            boxPos1ViewPlaneGizmo
                .render(sel.pendingX + 0.5, sel.pendingY + 0.5, sel.pendingZ + 0.5, rx, ry, rz, 0, 0, 0);
            boxPos2ViewPlaneGizmo
                .render(sel.pendingX2 + 0.5, sel.pendingY2 + 0.5, sel.pendingZ2 + 0.5, rx, ry, rz, 0, 0, 0);
            boxPos1Gizmo.render(sel.pendingX + 0.5, sel.pendingY + 0.5, sel.pendingZ + 0.5, rx, ry, rz, 0, 0, 0);
            boxPos2Gizmo.render(sel.pendingX2 + 0.5, sel.pendingY2 + 0.5, sel.pendingZ2 + 0.5, rx, ry, rz, 0, 0, 0);
            double cxWorld = (sel.pendingX + sel.pendingX2) / 2.0 + 0.5;
            double cyWorld = (sel.pendingY + sel.pendingY2) / 2.0 + 0.5;
            double czWorld = (sel.pendingZ + sel.pendingZ2) / 2.0 + 0.5;
            boxCenterViewPlaneGizmo.render(cxWorld, cyWorld, czWorld, rx, ry, rz, 0, 0, 0);
            boxCenterGizmo.render(cxWorld, cyWorld, czWorld, rx, ry, rz, 0, 0, 0);
        }

        PerfTrace.pop();
        // ── Builder tools hologram + smear preview ────────────────────────────
        if (DimensiumMode.INSTANCE.isBuilderToolsActive()) {
            BuilderToolState bts = BuilderToolState.INSTANCE;
            if (bts.phase == Phase.MANIPULATING && sel.hasSelection() && sel.clipboard != null) {
                PerfTrace.begin("builder MANIPULATING render tool=" + bts.activeTool);
                if (bts.activeTool == BuilderTool.SMEAR) {
                    PerfTrace.push("buildSmearPreview");
                    BuilderToolsHandler.buildSmearPreview(mc, sel, bts);
                    PerfTrace.pop();
                    if (bts.smearPreview != null) {
                        PerfTrace.push("renderSmearPreview");
                        renderProposalPreview(mc, rx, ry, rz, bts.smearPreview);
                        PerfTrace.pop();
                    }
                } else {
                    bts.smearPreview = null;
                    PerfTrace.push("hologram.render");
                    hologram.render(mc, sel, bts, rx, ry, rz);
                    PerfTrace.pop();
                }
                PerfTrace.end(16);
            } else {
                bts.smearPreview = null;
            }
        }

        // ── Shape placement gizmos + remaining ────────────────────────────
        PerfTrace.push("gizmosAndRemainder");
        // ── Shape placement gizmos ────────────────────────────────────────���───
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (ps.active) {
            if (DimensiumMode.INSTANCE.selectedTool != Tool.SHAPE) {
                ps.cancel();
            } else {
                ps.rebuildIfNeeded();
                ps.viewPlaneGizmo
                    .render(ps.centerX(), ps.centerY(), ps.centerZ(), rx, ry, rz, ps.rotX, ps.rotY, ps.rotZ);
                ps.scaleGizmo.render(ps.centerX(), ps.centerY(), ps.centerZ(), rx, ry, rz, ps.rotX, ps.rotY, ps.rotZ);
                ps.gizmo.render(ps.centerX(), ps.centerY(), ps.centerZ(), rx, ry, rz, ps.rotX, ps.rotY, ps.rotZ);
                ps.rotGizmo.render(ps.centerX(), ps.centerY(), ps.centerZ(), rx, ry, rz, ps.rotX, ps.rotY, ps.rotZ);
            }
        }

        // ── Clipboard paste placement ghost + gizmo ───────────────────────────
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (cps.preview != null) renderProposalPreview(mc, rx, ry, rz, cps.preview);
            cps.viewPlaneGizmo
                .render(cps.centerX(), cps.centerY(), cps.centerZ(), rx, ry, rz, cps.rotX, cps.rotY, cps.rotZ);
            cps.gizmo.render(cps.centerX(), cps.centerY(), cps.centerZ(), rx, ry, rz, 0, 0, 0);
            cps.rotGizmo.render(cps.centerX(), cps.centerY(), cps.centerZ(), rx, ry, rz, cps.rotX, cps.rotY, cps.rotZ);
        }

        // ── Move tool ghost + gizmos ──────────────────────────────────────────
        MoveToolState ms = MoveToolState.INSTANCE;
        if (DimensiumMode.INSTANCE.isActive() && DimensiumMode.INSTANCE.selectedTool == Tool.MOVE) {
            if (sel.hasSelection()) {
                if (!ms.active || ms.capturedSelVersion != sel.renderVersion) {
                    long _msT0 = System.nanoTime();
                    ms.activate(sel, mc.theWorld);
                    long _msActMs = (System.nanoTime() - _msT0) / 1_000_000;
                    System.err.println("[DIMTIMER] MoveToolState.activate=" + _msActMs + "ms selSize=" + sel.size());
                }
                long _rbT0 = System.nanoTime();
                ms.rebuildIfNeeded();
                long _rbMs = (System.nanoTime() - _rbT0) / 1_000_000;
                if (_rbMs > 5) System.err.println("[DIMTIMER] MoveToolState.rebuildIfNeeded=" + _rbMs + "ms");
                if (ms.preview != null) {
                    renderProposalPreview(mc, rx, ry, rz, ms.preview);
                }
                ms.viewPlaneGizmo.render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), rx, ry, rz, ms.rotX, ms.rotY, ms.rotZ);
                ms.scaleGizmo.render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), rx, ry, rz, ms.rotX, ms.rotY, ms.rotZ);
                ms.gizmo.render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), rx, ry, rz, ms.rotX, ms.rotY, ms.rotZ);
                ms.rotGizmo.render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), rx, ry, rz, ms.rotX, ms.rotY, ms.rotZ);
            } else if (ms.active) {
                ms.cancel();
            }
        } else if (ms.active) {
            ms.cancel();
        }

        // ── Modelling tool point rendering ────────────────────────────────────
        if (DimensiumMode.INSTANCE.isActive() && DimensiumMode.INSTANCE.selectedTool == Tool.MODELLING) {
            github.thehighcruw.dimensium.tool.state.ModellingToolState mts = github.thehighcruw.dimensium.tool.state.ModellingToolState.INSTANCE;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            for (int r = 0; r < mts.rows.size(); r++) {
                java.util.List<github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint> row = mts.rows
                    .get(r);
                // Lines within row
                java.util.List<int[]> rowXyz = new java.util.ArrayList<>(row.size());
                for (github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint p : row)
                    rowXyz.add(new int[] { p.x, p.y, p.z });
                renderLineStrip(rowXyz, 0.55f, 0.70f, 0.90f, 0.6f, 1.5f, rx, ry, rz);
                // Point boxes
                for (int c = 0; c < row.size(); c++) {
                    github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint mpt = row.get(c);
                    boolean ptSel = r == mts.selectedRow && c == mts.selectedPoint;
                    boolean activeRow = r == mts.currentRowIndex;
                    float pr = ptSel ? 1.0f : activeRow ? 0.80f : 0.55f;
                    float pg = ptSel ? 0.80f : activeRow ? 0.55f : 0.50f;
                    float pb = ptSel ? 0.20f : activeRow ? 0.90f : 0.65f;
                    renderPointBox(mpt.x, mpt.y, mpt.z, pr, pg, pb, 1.0f, rx, ry, rz);
                }
            }
            // Capture GL matrices unconditionally so GizmoProjection is valid for findNearestPointOnScreen
            // even before any point is selected (gizmo.render only captures when selectedPointObj != null).
            mts.gizmo.getProjection()
                .capture(rx, ry, rz);
            // Gizmo on selected point
            github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
            if (mSelPt != null) {
                mts.gizmo.render(mSelPt.x + 0.5, mSelPt.y + 0.5, mSelPt.z + 0.5, rx, ry, rz, 0, 0, 0);
            }
            // Lines between adjacent rows (column-matched)
            if (mts.rows.size() >= 2) {
                GL11.glLineWidth(1.0f);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor4f(0.70f, 0.55f, 0.90f, 0.4f);
                for (int r = 0; r + 1 < mts.rows.size(); r++) {
                    java.util.List<github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint> rowA = mts.rows
                        .get(r);
                    java.util.List<github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint> rowB = mts.rows
                        .get(r + 1);
                    int maxC = Math.min(rowA.size(), rowB.size());
                    for (int c = 0; c < maxC; c++) {
                        github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint a = rowA.get(c);
                        github.thehighcruw.dimensium.tool.state.ModellingToolState.ModelPoint b = rowB.get(c);
                        GL11.glVertex3d(a.x + 0.5 - rx, a.y + 0.5 - ry, a.z + 0.5 - rz);
                        GL11.glVertex3d(b.x + 0.5 - rx, b.y + 0.5 - ry, b.z + 0.5 - rz);
                    }
                }
                GL11.glEnd();
            }
        }

        // ── Path tool point rendering ─────────────────────────────────────────
        if (DimensiumMode.INSTANCE.isActive() && DimensiumMode.INSTANCE.selectedTool == Tool.PATH) {
            PathToolState pathState = PathToolState.INSTANCE;
            // Capture GL matrices here unconditionally so GizmoProjection is valid even
            // before any point is selected (gizmo.render only captures when selectedIndex >= 0).
            pathState.gizmo.getProjection()
                .capture(rx, ry, rz);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            for (int i = 0; i < pathState.points.size(); i++) {
                PathToolState.PathPoint pathPt = pathState.points.get(i);
                boolean ptSel = i == pathState.selectedIndex;
                renderPointBox(
                    pathPt.x,
                    pathPt.y,
                    pathPt.z,
                    ptSel ? 1.0f : 0.55f,
                    ptSel ? 0.85f : 0.70f,
                    1.0f,
                    1.0f,
                    rx,
                    ry,
                    rz);
            }
            if (pathState.selectedIndex >= 0 && !pathState.points.isEmpty()) {
                PathToolState.PathPoint selPt = pathState.selectedPoint();
                if (selPt != null) {
                    pathState.gizmo.render(selPt.x + 0.5, selPt.y + 0.5, selPt.z + 0.5, rx, ry, rz, 0, 0, 0);
                }
            }
        }

        PerfTrace.pop();
        GL11.glPopAttrib();
        PerfTrace.end(16);
    }

    private static void renderPointBox(int wx, int wy, int wz, float r, float g, float b, float a, double rx, double ry,
        double rz) {
        GL11.glPushMatrix();
        GL11.glTranslated(wx - rx, wy - ry, wz - rz);
        WorldLines.setEyeForTranslation(wx - rx, wy - ry, wz - rz);
        GL11.glColor4f(r, g, b, a);
        drawBox(0, 0, 0, 1, 1, 1);
        GL11.glPopMatrix();
    }

    private static void renderLineStrip(java.util.List<int[]> xyzList, float r, float g, float b, float a, float width,
        double rx, double ry, double rz) {
        if (xyzList.size() < 2) return;
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(r, g, b, a);
        for (int[] p : xyzList) GL11.glVertex3d(p[0] + 0.5 - rx, p[1] + 0.5 - ry, p[2] + 0.5 - rz);
        GL11.glEnd();
    }

    // ── Magic select preview (runs each frame while MAGIC_SELECT tool is active) ──

    private void updateMagicSelectPreview(Minecraft mc) {
        FreecamState fs = FreecamState.INSTANCE;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            bts.magicPreview = null;
            lastMagicX = Integer.MIN_VALUE;
            return;
        }

        if (mop.blockX == lastMagicX && mop.blockY == lastMagicY && mop.blockZ == lastMagicZ) return;
        lastMagicX = mop.blockX;
        lastMagicY = mop.blockY;
        lastMagicZ = mop.blockZ;

        MagicSelectToolState ts = MagicSelectToolState.INSTANCE;
        Set<Long> flooded = SelectionState.floodFill(
            mc.theWorld,
            mop.blockX,
            mop.blockY,
            mop.blockZ,
            ts.magicSelectLimit,
            ts.magicSelectRange,
            ts.magicSelectSurface,
            ts.magicSelectCorners,
            ts.magicCompareType,
            ts.magicDirection);

        ChangeProposal p = ChangeProposal.forPreview();
        for (long key : flooded) {
            int bx = SelectionState.unpackX(key);
            int by = SelectionState.unpackY(key);
            int bz = SelectionState.unpackZ(key);
            Block blk = mc.theWorld.getBlock(bx, by, bz);
            int meta = mc.theWorld.getBlockMetadata(bx, by, bz);
            p.proposed.put(ChangeProposal.packKey(bx, by, bz), new int[] { Block.getIdFromBlock(blk), meta });
        }
        bts.magicPreview = p;
    }

    private static void renderMagicPreview(Minecraft mc, double rx, double ry, double rz, ChangeProposal preview) {
        if (preview == null || preview.proposed.isEmpty()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        beginProposalRender(mc, rx, ry, rz);
        Tessellator t = Tessellator.instance;

        // Textured pass — fully opaque, exterior faces only.
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        t.startDrawingQuads();
        int batched = 0;
        for (Map.Entry<Long, int[]> e : preview.proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            Block blk = Block.getBlockById(bm[0]);
            if (blk == null || blk == Blocks.air || blk.getRenderType() != 0) continue;
            int bx = ChangeProposal.unpackX(key);
            int by = ChangeProposal.unpackY(key);
            int bz = ChangeProposal.unpackZ(key);
            int tint = 0xFFFFFF;
            try {
                tint = blk.colorMultiplier(mc.theWorld, bx, by, bz);
            } catch (Exception ignored) {}
            for (int face = 0; face < 6; face++) {
                long nk = ChangeProposal
                    .packKey(bx + GhostRenderer.NX[face], by + GhostRenderer.NY[face], bz + GhostRenderer.NZ[face]);
                if (!preview.proposed.containsKey(nk)) {
                    GhostRenderer.addTexturedFace(t, bx, by, bz, blk, bm[1], face, tint);
                    if (++batched % 2048 == 0) {
                        t.draw();
                        t.startDrawingQuads();
                    }
                }
            }
        }
        t.draw();

        // Glow — slightly more negative offset so no z-fighting with opaque pass.
        GL11.glPolygonOffset(-2.0f, -2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glColor4f(0.25f, 1.0f, 0.55f, 0.05f + 0.07f * pulse);
        GhostRenderer.drawExteriorFacesSingleColor(t, preview.proposed, null);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0.0f, 0.0f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Wireframe pass.
        rebuildProposalWireIfNeeded(preview);
        if (preview.cachedWire != null && preview.cachedWire.length > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0.40f, 1.0f, 0.55f, 0.70f + 0.20f * pulse);
            GL11.glPushMatrix();
            GL11.glTranslated(preview.wireOrigin[0], preview.wireOrigin[1], preview.wireOrigin[2]);
            // Outer translate: (-rx,-ry,-rz). Inner: wireOrigin. Eye in local = rx-wireOrigin[0], etc.
            WorldLines.setEye(rx - preview.wireOrigin[0], ry - preview.wireOrigin[1], rz - preview.wireOrigin[2]);
            GhostRenderer.drawWireframeCache(t, preview.cachedWire);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }

    // ── Selection wireframe cache ─────────────────────────────────────────────

    private void updateSelWireframeCache(SelectionState sel) {
        if (sel.renderVersion == cachedSelVersion) return;
        cachedSelWire = computeSelWireframe(sel.getSelectedBlocks());
        cachedSelVersion = sel.renderVersion;
    }

    private void drawSelWireframe(double rx, double ry, double rz) {
        if (cachedSelWire == null || cachedSelWire.length == 0) return;
        // Vertices are world-space ints; offset by rx/ry/rz → camera-relative. Eye = origin.
        WorldLines.setEye(0, 0, 0);
        WorldLines.drawIntWireframeCache(Tessellator.instance, cachedSelWire, rx, ry, rz, WorldLines.W_THIN);
    }

    private static int[] computeSelWireframe(Set<Long> blockSet) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(blockSet.size() * 4);
        for (long packed : blockSet) {
            int bx = SelectionState.unpackX(packed);
            int by = SelectionState.unpackY(packed);
            int bz = SelectionState.unpackZ(packed);
            for (int face = 0; face < 6; face++) {
                if (blockSet.contains(
                    SelectionState
                        .pack(bx + GhostRenderer.NX[face], by + GhostRenderer.NY[face], bz + GhostRenderer.NZ[face])))
                    continue;
                int axisBit = GhostRenderer.FACE_AXIS_BIT[face];
                for (int[] e : GhostRenderer.FACE_EDGES[face]) {
                    long ek = ((long) e[0] << 60) | SelectionState.pack(bx + e[1], by + e[2], bz + e[3]);
                    Integer prev = edgeMask.get(ek);
                    edgeMask.put(ek, prev == null ? axisBit : prev | axisBit);
                }
            }
        }
        int creaseCount = 0;
        for (int mask : edgeMask.values()) if (Integer.bitCount(mask) > 1) creaseCount++;
        int[] verts = new int[creaseCount * 6];
        int vi = 0;
        for (Map.Entry<Long, Integer> entry : edgeMask.entrySet()) {
            if (Integer.bitCount(entry.getValue()) <= 1) continue;
            long ek = entry.getKey();
            int axis = (int) (ek >>> 60) & 3;
            int ex = (int) ((ek >> 34) & 0x3FFFFFF) - 30_000_000;
            int ey = (int) ((ek >> 26) & 0xFF);
            int ez = (int) (ek & 0x3FFFFFF) - 30_000_000;
            verts[vi++] = ex;
            verts[vi++] = ey;
            verts[vi++] = ez;
            verts[vi++] = ex + (axis == 0 ? 1 : 0);
            verts[vi++] = ey + (axis == 1 ? 1 : 0);
            verts[vi++] = ez + (axis == 2 ? 1 : 0);
        }
        return verts;
    }

    // ── Package-private draw helpers (used by BrushPreviewRenderer, HologramRenderer) ──

    static long lPack(int x, int y, int z) {
        return ((long) (x + 4096) << 26) | ((long) (y + 4096) << 13) | (z + 4096);
    }

    static void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        WorldLines.drawBox(x1, y1, z1, x2, y2, z2, WorldLines.W_SEL);
    }

    private static void renderProposalPreview(Minecraft mc, double rx, double ry, double rz, ChangeProposal drag) {
        if (drag == null || drag.proposed.isEmpty()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        beginProposalRender(mc, rx, ry, rz);
        Tessellator t = Tessellator.instance;

        // Pass 0: removals — orange tint over existing blocks, exterior faces only.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 0.40f, 0.10f, 0.70f);
        GhostRenderer.drawExteriorFacesSingleColor(t, drag.proposed, bm -> bm[0] == 0);

        // Pass 1: textured additions fully opaque, exterior faces only.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        t.startDrawingQuads();
        int batched = 0;
        for (Map.Entry<Long, int[]> e : drag.proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            Block blk = Block.getBlockById(bm[0]);
            if (blk == null || blk == Blocks.air || blk.getRenderType() != 0) continue;
            int bx = ChangeProposal.unpackX(key);
            int by = ChangeProposal.unpackY(key);
            int bz = ChangeProposal.unpackZ(key);
            for (int face = 0; face < 6; face++) {
                long nk = ChangeProposal
                    .packKey(bx + GhostRenderer.NX[face], by + GhostRenderer.NY[face], bz + GhostRenderer.NZ[face]);
                if (!drag.proposed.containsKey(nk)) {
                    GhostRenderer.addTexturedFace(t, bx, by, bz, blk, bm[1], face);
                    if (++batched % 2048 == 0) {
                        t.draw();
                        t.startDrawingQuads();
                    }
                }
            }
        }
        t.draw();

        // Pass 2: colored fallback for non-standard render type additions only, exterior faces only.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.6f, 0.85f, 1.0f, 1.0f);
        GhostRenderer.drawExteriorFacesSingleColor(
            t,
            drag.proposed,
            bm -> bm[0] != 0 && (Block.getBlockById(bm[0]) == null || Block.getBlockById(bm[0])
                .getRenderType() != 0));

        // Glow passes — slightly more negative offset so no z-fighting with opaque pass.
        // glDepthMask(false): glow quads never occlude each other at crease edges.
        GL11.glPolygonOffset(-2.0f, -2.0f);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);

        // Additive orange glow over removals — exterior faces only.
        GL11.glColor4f(1.0f, 0.30f, 0.0f, 0.05f + 0.07f * pulse);
        GhostRenderer.drawExteriorFacesSingleColor(t, drag.proposed, bm -> bm[0] == 0);

        // Additive blue glow over all additions — exterior faces only.
        GL11.glColor4f(0.40f, 0.75f, 1.0f, 0.05f + 0.07f * pulse);
        GhostRenderer.drawExteriorFacesSingleColor(t, drag.proposed, bm -> bm[0] != 0);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0.0f, 0.0f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Pass 3: crease-edge wireframe around the exterior of the proposed shape.
        rebuildProposalWireIfNeeded(drag);
        if (drag.cachedWire != null && drag.cachedWire.length > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0.75f, 0.90f, 1.0f, 0.70f + 0.20f * pulse);
            GL11.glPushMatrix();
            GL11.glTranslated(drag.wireOrigin[0], drag.wireOrigin[1], drag.wireOrigin[2]);
            // Outer translate: (-rx,-ry,-rz). Inner: wireOrigin. Eye in local = rx-wireOrigin[0], etc.
            WorldLines.setEye(rx - drag.wireOrigin[0], ry - drag.wireOrigin[1], rz - drag.wireOrigin[2]);
            GhostRenderer.drawWireframeCache(t, drag.cachedWire);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();
    }

    private static void beginProposalRender(Minecraft mc, double rx, double ry, double rz) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CW);
        GL11.glPushMatrix();
        GL11.glTranslated(-rx, -ry, -rz);
    }

    private static void rebuildProposalWireIfNeeded(ChangeProposal drag) {
        if (drag.proposed.size() == drag.wireCacheSize) return;
        drag.wireCacheSize = drag.proposed.size();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (long key : drag.proposed.keySet()) {
            int x = ChangeProposal.unpackX(key);
            int y = ChangeProposal.unpackY(key);
            int z = ChangeProposal.unpackZ(key);
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
        }
        drag.wireOrigin[0] = minX;
        drag.wireOrigin[1] = minY;
        drag.wireOrigin[2] = minZ;

        java.util.List<int[]> local = new java.util.ArrayList<>(drag.proposed.size());
        for (long key : drag.proposed.keySet()) {
            local.add(
                new int[] { ChangeProposal.unpackX(key) - minX, ChangeProposal.unpackY(key) - minY,
                    ChangeProposal.unpackZ(key) - minZ });
        }
        drag.cachedWire = GhostRenderer.INSTANCE.computeLocalWireframe(local);
    }

    // ── Elevation tool terrain-projected preview ──────────────────────────────

    private void renderElevationPreview(Minecraft mc, double rx, double ry, double rz) {
        FreecamState fs = FreecamState.INSTANCE;
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
            mc,
            mc.displayWidth,
            mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        ElevationToolState s = ElevationToolState.INSTANCE;
        int cx = mop.blockX, cz = mop.blockZ;
        int radius = Math.max(1, s.elevationRadius);

        boolean held = org.lwjgl.input.Mouse.isButtonDown(KeyConstants.RMB);
        float pulse = held ? 0.10f * (float) Math.sin(System.currentTimeMillis() / 180.0) : 0f;

        Tessellator t = Tessellator.instance;

        // ── Fill: top faces colored by falloff weight ─────────────────────────
        t.startDrawingQuads();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float r = (float) Math.sqrt((double) (dx * dx + dz * dz)) / radius;
                if (r > 1f) continue;

                float weight = ElevationBrush.falloff(s.elevationFalloff, r);
                weight = ElevationBrush.edgeSmoothing(weight, r, s.elevationSmoothing);
                if (weight <= 0f) continue;

                int wx = cx + dx, wz = cz + dz;
                double topY = elevPreviewTopY(mc, wx, wz) + 1.002; // slightly above block top

                float alpha = (held ? 0.15f + pulse : 0.08f) + 0.18f * weight;
                // Green with weight-based brightness
                float cr = 0.30f + 0.20f * weight;
                float cg = 0.65f + 0.15f * weight;
                float cb = 0.25f;

                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx - rx, topY - ry, wz - rz);
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx + 1 - rx, topY - ry, wz - rz);
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx + 1 - rx, topY - ry, wz + 1 - rz);
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx - rx, topY - ry, wz + 1 - rz);
            }
        }
        t.draw();

        // ── Ring: boundary edges where the circle meets outside ───────────────
        // Vertices are camera-relative (no active glTranslated) → eye = origin.
        WorldLines.setEye(0, 0, 0);
        GL11.glColor4f(0.45f, 0.88f, 0.32f, 0.95f);
        WorldLines.prepareSegmentBatch();
        t.startDrawingQuads();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float r = (float) Math.sqrt((double) (dx * dx + dz * dz)) / radius;
                if (r > 1f) continue;

                int wx = cx + dx, wz = cz + dz;
                double topY = elevPreviewTopY(mc, wx, wz) + 1.002;

                if ((float) Math.sqrt((double) ((dx + 1) * (dx + 1) + dz * dz)) / radius > 1f) {
                    WorldLines.addSegment(
                        t,
                        wx + 1 - rx,
                        topY - ry,
                        wz - rz,
                        wx + 1 - rx,
                        topY - ry,
                        wz + 1 - rz,
                        WorldLines.W_THIN);
                }
                if ((float) Math.sqrt((double) ((dx - 1) * (dx - 1) + dz * dz)) / radius > 1f) {
                    WorldLines
                        .addSegment(t, wx - rx, topY - ry, wz - rz, wx - rx, topY - ry, wz + 1 - rz, WorldLines.W_THIN);
                }
                if ((float) Math.sqrt((double) (dx * dx + (dz + 1) * (dz + 1))) / radius > 1f) {
                    WorldLines.addSegment(
                        t,
                        wx - rx,
                        topY - ry,
                        wz + 1 - rz,
                        wx + 1 - rx,
                        topY - ry,
                        wz + 1 - rz,
                        WorldLines.W_THIN);
                }
                if ((float) Math.sqrt((double) (dx * dx + (dz - 1) * (dz - 1))) / radius > 1f) {
                    WorldLines
                        .addSegment(t, wx - rx, topY - ry, wz - rz, wx + 1 - rx, topY - ry, wz - rz, WorldLines.W_THIN);
                }
            }
        }
        t.draw();
    }

    private static double elevPreviewTopY(Minecraft mc, int wx, int wz) {
        // Check activeDrag proposal first (for Continuous mode visual feedback)
        ChangeProposal drag = ActiveDragState.INSTANCE.activeDrag;
        if (drag != null) {
            for (int y = 255; y >= 0; y--) {
                long key = ChangeProposal.packKey(wx, y, wz);
                int[] bm = drag.proposed.get(key);
                if (bm != null) {
                    if (bm[0] != 0) return y;
                    continue;
                }
                if (mc.theWorld.getBlock(wx, y, wz) != net.minecraft.init.Blocks.air) return y;
            }
            return 0;
        }
        for (int y = 255; y >= 0; y--) {
            if (mc.theWorld.getBlock(wx, y, wz) != net.minecraft.init.Blocks.air) return y;
        }
        return 0;
    }

    static void drawFilledBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        GhostRenderer.addBoxFaces(t, x1, y1, z1, x2, y2, z2);
        t.draw();
    }
}
