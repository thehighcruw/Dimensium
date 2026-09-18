/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.handler.TickHandler;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.MenuBar;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.overlay.ViewState;
import github.thehighcruw.dimensium.editor.tool.ActiveDragState;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.ToolRegistry;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.creating.stamp.StampBrushInput;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationBrush;
import github.thehighcruw.dimensium.editor.tool.manipulating.elevation.ElevationToolState;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveToolState;
import github.thehighcruw.dimensium.editor.tool.painting.gradient.GradientToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.magic.MagicSelectToolState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.DistortSelectionWindow;
import github.thehighcruw.dimensium.editor.window.FilterSelectionWindow;
import github.thehighcruw.dimensium.editor.window.SmoothSelectionWindow;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.viewport.ViewportPanel;
import github.thehighcruw.dimensium.shared.BlockColorCache;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec2DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.shared.util.WorldUtils;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.world.handler.BuilderToolsHandler;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class SelectionRenderer {

    public static final SelectionRenderer INSTANCE = new SelectionRenderer();

    private static final FloatBuffer PROJ_BUF = BufferUtils.createFloatBuffer(16);

    // Box-select gizmos — package-private so GuiDimensiumOverlay/OverlayRenderer can access them.
    public static final ViewPlaneGizmo boxPos1ViewPlaneGizmo = new ViewPlaneGizmo();
    public static final ViewPlaneGizmo boxPos2ViewPlaneGizmo = new ViewPlaneGizmo();
    public static final TranslationGizmo boxPos1Gizmo = new TranslationGizmo();
    public static final TranslationGizmo boxPos2Gizmo = new TranslationGizmo();
    public static final PlaneTranslationGizmo boxPos1PlaneGizmo = new PlaneTranslationGizmo();
    public static final PlaneTranslationGizmo boxPos2PlaneGizmo = new PlaneTranslationGizmo();

    // Center gizmos — move the entire box (both corners) together.
    public static final ViewPlaneGizmo boxCenterViewPlaneGizmo = new ViewPlaneGizmo();
    public static final TranslationGizmo boxCenterGizmo = new TranslationGizmo();
    public static final PlaneTranslationGizmo boxCenterPlaneGizmo = new PlaneTranslationGizmo();
    // Corner positions captured at the start of a center-gizmo drag.
    public Vec3DInt boxCenterDragP1 = Vec3DInt.ZERO;
    public Vec3DInt boxCenterDragP2 = Vec3DInt.ZERO;

    private final HologramRenderer hologram = new HologramRenderer();

    private long cachedSelVersion = -1;
    private int[] cachedSelWire = null;

    // Magic select preview dedup — rebuild only when cursor moves to a new block.
    private Vec3DInt lastMagicPos = null;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        boolean cameraMoving = FreecamState.INSTANCE.isMoving();
        boolean gizmoDragging = GuiDimensiumOverlay.anyGizmoDragging();

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
        Entity cam = mc.renderViewEntity != null ? mc.renderViewEntity : player;
        Vec3DDouble camPos = Vec3DDouble.from(
                cam.lastTickPosX + (cam.posX - cam.lastTickPosX) * pt,
                cam.lastTickPosY + (cam.posY - cam.lastTickPosY) * pt,
                cam.lastTickPosZ + (cam.posZ - cam.lastTickPosZ) * pt);

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
        Tool _previewTool = DimensiumEditorMode.INSTANCE.selectedTool;
        boolean _anyModal = ImGuiManager.INSTANCE.anyModalOpen()
                || FilterSelectionWindow.INSTANCE.isOpen()
                || DistortSelectionWindow.INSTANCE.isOpen()
                || SmoothSelectionWindow.INSTANCE.isOpen();
        float _sf = RenderUtils.scaleFactor();
        float _mx = FreecamState.INSTANCE.cursorX * _sf;
        float _my = FreecamState.INSTANCE.cursorY * _sf;
        boolean _mouseOverOtherPanel = OverlayRenderer.TOOL_WINDOW.containsMouse(_mx, _my)
                || MenuBar.INSTANCE.containsMouse(_mx, _my, mc.displayWidth);
        boolean _cursorOnViewport = !_mouseOverOtherPanel
                && (!ImGuiManager.INSTANCE.wantCaptureMouse() || ViewportPanel.INSTANCE.isHovered());
        if (!cameraMoving) {
            if (DimensiumEditorMode.INSTANCE.isActive()
                    && !_anyModal
                    && _cursorOnViewport
                    && (!TickHandler.INSTANCE.isPaintDragging() || _previewTool == Tool.SMOOTH)) {
                ToolRegistry.toolRenderer(_previewTool).renderWorldPreview(mc, camPos);
            }
            if (DimensiumEditorMode.INSTANCE.isActive()
                    && !_anyModal
                    && _cursorOnViewport
                    && DimensiumEditorMode.INSTANCE.selectedTool == Tool.ELEVATION) {
                renderElevationPreview(mc, camPos);
            }
        }

        // ── Gradient pos1 → cursor line ───────────────────────────────────────
        if (DimensiumEditorMode.INSTANCE.isActive() && DimensiumEditorMode.INSTANCE.selectedTool == Tool.GRADIENT) {
            GradientToolState gs = GradientToolState.INSTANCE;
            if (gs.gradientHasPos1) {
                MovingObjectPosition gmop = RenderUtils.raycastAtCursor();
                if (gmop != null && gmop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    Vec3DDouble p1 = gs.gradientPos1.toDouble().plus(0.5).minus(camPos);
                    Vec3DDouble p2 = Vec3DDouble.from(
                            gmop.blockX + 0.5 - camPos.x(),
                            gmop.blockY + 0.5 - camPos.y(),
                            gmop.blockZ + 0.5 - camPos.z());
                    GL11.glColor4f(0.6f, 0.3f, 1.0f, 0.9f);
                    WorldLines.setEye(Vec3DDouble.ZERO); // vertices already camera-relative
                    Tessellator gTess = Tessellator.instance;
                    gTess.startDrawingQuads();
                    WorldLines.addSegment(gTess, p1, p2, WorldLines.W_SEL);
                    gTess.draw();
                    Vec3DDouble gradTrans = gs.gradientPos1.toDouble().minus(camPos);
                    GL11.glPushMatrix();
                    GL11.glTranslated(gradTrans.x(), gradTrans.y(), gradTrans.z());
                    GL11.glColor4f(0.6f, 0.3f, 1.0f, 1.0f);
                    WorldLines.setEyeForTranslation(gradTrans);
                    drawBox(0, 0, 0, 1, 1, 1);
                    GL11.glPopMatrix();
                }
            }
        }

        PerfTrace.pop();
        // ── Proposal previews (fill / extrude / shape / move / drag stroke) ────
        PerfTrace.push("proposalPreviews");
        if (DimensiumEditorMode.INSTANCE.isActive()) {
            Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;
            BuilderToolState bts0 = BuilderToolState.INSTANCE;
            if (tool == Tool.EXTRUDE) ExtrudeHelper.INSTANCE.buildExtrudeProposal(mc);
            else {
                bts0.extrudePreview = null;
                ExtrudeHelper.INSTANCE.resetExtrudeDedup();
            }

            if (tool == Tool.MAGIC_SELECT && _cursorOnViewport) updateMagicSelectPreview(mc);
            else {
                bts0.magicPreview = null;
                lastMagicPos = null;
            }

            // Rebuild shape proposal before rendering so ShapePlacementState.preview is current.
            ShapePlacementState ps0 = ShapePlacementState.INSTANCE;
            if (ps0.active && tool == Tool.SHAPE) ps0.rebuildIfNeeded();

            if (tool == Tool.PATH) {
                PathToolState pathState = PathToolState.INSTANCE;
                pathState.rebuildIfNeeded(SelectedBlockState.INSTANCE.selectedBlock);
            }

            if (tool == Tool.MODELLING) {
                ModellingToolState mts = ModellingToolState.INSTANCE;
                mts.rebuildIfNeeded(SelectedBlockState.INSTANCE.selectedBlock);
            }

            if (bts0.fillPreview != null) renderProposalPreview(mc, camPos, bts0.fillPreview);
            if (bts0.extrudePreview != null) renderProposalPreview(mc, camPos, bts0.extrudePreview);
            if (bts0.magicPreview != null) renderMagicPreview(mc, camPos, bts0.magicPreview);
            if (ps0.preview != null) renderProposalPreview(mc, camPos, ps0.preview);
            if (ActiveDragState.INSTANCE.activeDrag != null)
                renderProposalPreview(mc, camPos, ActiveDragState.INSTANCE.activeDrag);
            if (tool == Tool.PATH && PathToolState.INSTANCE.preview != null)
                renderProposalPreview(mc, camPos, PathToolState.INSTANCE.preview);
            if (tool == Tool.MODELLING && ModellingToolState.INSTANCE.preview != null)
                renderProposalPreview(mc, camPos, ModellingToolState.INSTANCE.preview);
            if (tool == Tool.STAMP && StampBrushInput.INSTANCE.dragPreview != null)
                renderProposalPreview(mc, camPos, StampBrushInput.INSTANCE.dragPreview);
        }

        PerfTrace.pop();
        // ── Selection rendering ───────────────────────────────────────────────
        PerfTrace.push("selectionRender");
        SelectionState sel = SelectionState.INSTANCE;

        // Tool-change commit: if the user left SELECT while a box was confirmed, apply it now.
        if (sel.boxConfirmed && DimensiumEditorMode.INSTANCE.selectedTool != Tool.SELECT) {
            BoxSelectToolState bts = BoxSelectToolState.INSTANCE;
            sel.applyOp(SelectionState.aabbBlocks(sel.pendingPos, sel.pendingPos2), bts.booleanOp);
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
                mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glFrontFace(GL11.GL_CW);

                GL11.glPushMatrix();
                GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());

                Tessellator t = Tessellator.instance;
                Set<Long> selBlocks = sel.getSelectedBlocks();
                t.startDrawingQuads();
                int batched = 0;
                for (long key : selBlocks) {
                    Vec3DInt bv = SelectionState.unpack(key);
                    Block b = WorldUtils.getBlock(mc.theWorld, bv);
                    if (b == null || b == Blocks.air || b.getRenderType() != 0) continue;
                    int meta = WorldUtils.getBlockMetadata(mc.theWorld, bv);
                    int tint = 0xFFFFFF;
                    try {
                        tint = b.colorMultiplier(mc.theWorld, bv.x(), bv.y(), bv.z());
                    } catch (Exception ignored) {
                    }
                    for (int face = 0; face < 6; face++) {
                        long nk = SelectionState.pack(
                                bv.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]));
                        if (!selBlocks.contains(nk)) {
                            GhostRenderer.addTexturedFace(t, bv, b, meta, face, tint);
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
                    Vec3DInt bv = SelectionState.unpack(key);
                    Block b = WorldUtils.getBlock(mc.theWorld, bv);
                    if (b != null && b != Blocks.air && b.getRenderType() != 0) {
                        for (int face = 0; face < 6; face++) {
                            long nk = SelectionState.pack(
                                    bv.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]));
                            if (!selBlocks.contains(nk)) {
                                GhostRenderer.addSingleFace(t, bv, face);
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
                    Vec3DInt bv = SelectionState.unpack(key);
                    Block b = WorldUtils.getBlock(mc.theWorld, bv);
                    if (b == null || b == Blocks.air) continue;
                    for (int face = 0; face < 6; face++) {
                        long nk = SelectionState.pack(
                                bv.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face]));
                        if (!selBlocks.contains(nk)) {
                            GhostRenderer.addSingleFace(t, bv, face, 0.02f);
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
                drawSelWireframe(camPos);
            } else {
                Vec3DDouble selTrans =
                        Vec3DDouble.from(sel.minX() - camPos.x(), sel.minY() - camPos.y(), sel.minZ() - camPos.z());
                GL11.glPushMatrix();
                GL11.glTranslated(selTrans.x(), selTrans.y(), selTrans.z());
                WorldLines.setEyeForTranslation(selTrans);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.06f + pulse * 0.04f);
                drawFilledBox(sel.width(), sel.height(), sel.depth());
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.85f);
                drawBox(0, 0, 0, sel.width(), sel.height(), sel.depth());
                GL11.glPopMatrix();
            }

            Vec3DDouble selOutlineTrans =
                    Vec3DDouble.from(sel.minX() - camPos.x(), sel.minY() - camPos.y(), sel.minZ() - camPos.z());
            GL11.glPushMatrix();
            GL11.glTranslated(selOutlineTrans.x(), selOutlineTrans.y(), selOutlineTrans.z());
            WorldLines.setEyeForTranslation(selOutlineTrans);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, pulse * 0.4f);
            drawBox(-0.01f, -0.01f, -0.01f, sel.width() + 0.01f, sel.height() + 0.01f, sel.depth() + 0.01f);
            GL11.glPopMatrix();
        }

        // ── Live drag preview: pos1 anchor + AABB to cursor ──────────────────
        boolean selectToolActive =
                DimensiumEditorMode.INSTANCE.isActive() && DimensiumEditorMode.INSTANCE.selectedTool == Tool.SELECT;
        boolean builderActive = DimensiumEditorMode.INSTANCE.isBuilderToolsActive();
        if (!selectToolActive && !builderActive) {
            sel.pendingPos1 = false;
            sel.boxConfirmed = false;
        }
        if (sel.pendingPos1) {
            Vec3DDouble pendingTrans = Vec3DDouble.from(
                    sel.pendingPos.x() - camPos.x(), sel.pendingPos.y() - camPos.y(), sel.pendingPos.z() - camPos.z());
            GL11.glPushMatrix();
            GL11.glTranslated(pendingTrans.x(), pendingTrans.y(), pendingTrans.z());
            WorldLines.setEyeForTranslation(pendingTrans);
            GL11.glColor4f(0.2f, 1.0f, 0.8f, 1.0f);
            drawBox(0, 0, 0, 1, 1, 1);
            GL11.glPopMatrix();

            MovingObjectPosition bxMop;
            if (builderActive && !DimensiumEditorMode.INSTANCE.isActive()) {
                bxMop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
            } else {
                bxMop = RenderUtils.raycastAtCursor();
            }
            if (bxMop != null && bxMop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Vec3DInt mopPos = Vec3DInt.from(bxMop.blockX, bxMop.blockY, bxMop.blockZ);
                Vec3DInt mn = sel.pendingPos.min(mopPos);
                Vec3DInt mx = sel.pendingPos.max(mopPos).plus(1);
                Vec3DDouble dragTrans = mn.toDouble().minus(camPos);
                GL11.glPushMatrix();
                GL11.glTranslated(dragTrans.x(), dragTrans.y(), dragTrans.z());
                WorldLines.setEyeForTranslation(dragTrans);
                GL11.glColor4f(0.2f, 1.0f, 0.8f, 0.55f);
                drawBox(0, 0, 0, mx.x() - mn.x(), mx.y() - mn.y(), mx.z() - mn.z());
                GL11.glPopMatrix();
            }
        }

        // ── Box confirmed: frozen AABB + pos1/pos2 gizmos ────────────────────
        if (sel.boxConfirmed && DimensiumEditorMode.INSTANCE.selectedTool == Tool.SELECT) {
            Vec3DInt mn = sel.pendingPos.min(sel.pendingPos2);
            Vec3DInt mx = sel.pendingPos.max(sel.pendingPos2).plus(1);
            Vec3DDouble boxTrans = mn.toDouble().minus(camPos);
            GL11.glPushMatrix();
            GL11.glTranslated(boxTrans.x(), boxTrans.y(), boxTrans.z());
            WorldLines.setEyeForTranslation(boxTrans);
            GL11.glColor4f(0.2f, 1.0f, 0.8f, 0.9f);
            drawBox(0, 0, 0, mx.x() - mn.x(), mx.y() - mn.y(), mx.z() - mn.z());
            GL11.glPopMatrix();
            if (!cameraMoving || gizmoDragging) {
                boxPos1Gizmo.axisFlip[0] = sel.pendingPos.x() <= sel.pendingPos2.x() ? -1f : 1f;
                boxPos1Gizmo.axisFlip[1] = sel.pendingPos.y() <= sel.pendingPos2.y() ? -1f : 1f;
                boxPos1Gizmo.axisFlip[2] = sel.pendingPos.z() <= sel.pendingPos2.z() ? -1f : 1f;
                boxPos2Gizmo.axisFlip[0] = -boxPos1Gizmo.axisFlip[0];
                boxPos2Gizmo.axisFlip[1] = -boxPos1Gizmo.axisFlip[1];
                boxPos2Gizmo.axisFlip[2] = -boxPos1Gizmo.axisFlip[2];
                boxPos1ViewPlaneGizmo.render(
                        sel.pendingPos.x() + 0.5, sel.pendingPos.y() + 0.5, sel.pendingPos.z() + 0.5, camPos);
                boxPos2ViewPlaneGizmo.render(
                        sel.pendingPos2.x() + 0.5, sel.pendingPos2.y() + 0.5, sel.pendingPos2.z() + 0.5, camPos);
                boxPos1PlaneGizmo.render(
                        sel.pendingPos.x() + 0.5, sel.pendingPos.y() + 0.5, sel.pendingPos.z() + 0.5, camPos, 0, 0, 0);
                boxPos1Gizmo.render(
                        sel.pendingPos.x() + 0.5, sel.pendingPos.y() + 0.5, sel.pendingPos.z() + 0.5, camPos, 0, 0, 0);
                boxPos2PlaneGizmo.render(
                        sel.pendingPos2.x() + 0.5,
                        sel.pendingPos2.y() + 0.5,
                        sel.pendingPos2.z() + 0.5,
                        camPos,
                        0,
                        0,
                        0);
                boxPos2Gizmo.render(
                        sel.pendingPos2.x() + 0.5,
                        sel.pendingPos2.y() + 0.5,
                        sel.pendingPos2.z() + 0.5,
                        camPos,
                        0,
                        0,
                        0);
                Vec3DDouble cWorld = sel.pendingPos
                        .toDouble()
                        .plus(sel.pendingPos2.toDouble())
                        .times(0.5)
                        .plus(0.5);
                boxCenterViewPlaneGizmo.render(cWorld.x(), cWorld.y(), cWorld.z(), camPos);
                boxCenterPlaneGizmo.render(cWorld.x(), cWorld.y(), cWorld.z(), camPos, 0, 0, 0);
                boxCenterGizmo.render(cWorld.x(), cWorld.y(), cWorld.z(), camPos, 0, 0, 0);
            }
        }

        PerfTrace.pop();
        // ── Builder tools hologram + smear preview ────────────────────────────
        if (DimensiumEditorMode.INSTANCE.isBuilderToolsActive()) {
            BuilderToolState bts = BuilderToolState.INSTANCE;
            if (bts.phase == Phase.MANIPULATING && sel.hasSelection() && sel.clipboard != null) {
                PerfTrace.begin("builder MANIPULATING render tool=" + bts.activeTool);
                if (bts.activeTool == BuilderTool.SMEAR) {
                    PerfTrace.push("buildSmearPreview");
                    BuilderToolsHandler.buildSmearPreview(mc, sel, bts);
                    PerfTrace.pop();
                    if (bts.smearPreview != null) {
                        PerfTrace.push("renderSmearPreview");
                        renderProposalPreview(mc, camPos, bts.smearPreview);
                        PerfTrace.pop();
                    }
                } else {
                    bts.smearPreview = null;
                    PerfTrace.push("hologram.render");
                    hologram.render(mc, sel, bts, camPos);
                    PerfTrace.pop();
                }
                PerfTrace.end(16);
            } else {
                bts.smearPreview = null;
            }
        }

        // ── Shape placement gizmos + remaining ────────────────────────────
        PerfTrace.push("gizmosAndRemainder");
        // ── Shape placement gizmos ────────────────────────────────────────────
        ShapePlacementState ps = ShapePlacementState.INSTANCE;
        if (ps.active) {
            if (DimensiumEditorMode.INSTANCE.selectedTool != Tool.SHAPE) {
                ps.cancel();
            } else if (!cameraMoving || gizmoDragging) {
                ps.rebuildIfNeeded();
                ps.viewPlaneGizmo.render(ps.centerX(), ps.centerY(), ps.centerZ(), camPos);
                ps.getPlaneTranslationGizmo()
                        .render(ps.centerX(), ps.centerY(), ps.centerZ(), camPos, ps.rot.x(), ps.rot.y(), ps.rot.z());
                ps.getAxisTranslationGizmo()
                        .render(ps.centerX(), ps.centerY(), ps.centerZ(), camPos, ps.rot.x(), ps.rot.y(), ps.rot.z());
                ps.getScalingGizmo()
                        .render(ps.centerX(), ps.centerY(), ps.centerZ(), camPos, ps.rot.x(), ps.rot.y(), ps.rot.z());
                ps.getRotationGizmo()
                        .render(ps.centerX(), ps.centerY(), ps.centerZ(), camPos, ps.rot.x(), ps.rot.y(), ps.rot.z());
            }
        }

        // ── Clipboard paste placement ghost + gizmo ───────────────────────────
        ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
        if (cps.active) {
            if (cps.preview != null) renderProposalPreview(mc, camPos, cps.preview);
            if (!cameraMoving || gizmoDragging) {
                cps.viewPlaneGizmo.render(cps.centerX(), cps.centerY(), cps.centerZ(), camPos);
                cps.getPlaneTranslationGizmo()
                        .render(
                                cps.centerX(),
                                cps.centerY(),
                                cps.centerZ(),
                                camPos,
                                cps.rot.x(),
                                cps.rot.y(),
                                cps.rot.z());
                cps.getAxisTranslationGizmo().render(cps.centerX(), cps.centerY(), cps.centerZ(), camPos, 0, 0, 0);
                cps.getRotationGizmo()
                        .render(
                                cps.centerX(),
                                cps.centerY(),
                                cps.centerZ(),
                                camPos,
                                cps.rot.x(),
                                cps.rot.y(),
                                cps.rot.z());
            }
        }

        // ── Move tool ghost + gizmos ──────────────────────────────────────────
        MoveToolState ms = MoveToolState.INSTANCE;
        if (DimensiumEditorMode.INSTANCE.isActive() && DimensiumEditorMode.INSTANCE.selectedTool == Tool.MOVE) {
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
                    renderProposalPreview(mc, camPos, ms.preview);
                }
                if (!cameraMoving || gizmoDragging) {
                    ms.viewPlaneGizmo.render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), camPos);
                    ms.getPlaneTranslationGizmo()
                            .render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), camPos, ms.rot.x(), ms.rot.y(), ms.rot.z());
                    ms.getAxisTranslationGizmo()
                            .render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), camPos, ms.rot.x(), ms.rot.y(), ms.rot.z());
                    ms.getScalingGizmo()
                            .render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), camPos, ms.rot.x(), ms.rot.y(), ms.rot.z());
                    ms.getRotationGizmo()
                            .render(ms.gizmoX(), ms.gizmoY(), ms.gizmoZ(), camPos, ms.rot.x(), ms.rot.y(), ms.rot.z());
                }
            } else if (ms.active) {
                ms.cancel();
            }
        } else if (ms.active) {
            ms.cancel();
        }

        // ── Modelling tool point rendering ────────────────────────────────────
        if (DimensiumEditorMode.INSTANCE.isActive() && DimensiumEditorMode.INSTANCE.selectedTool == Tool.MODELLING) {
            ModellingToolState mts = ModellingToolState.INSTANCE;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            for (int r = 0; r < mts.rows.size(); r++) {
                List<ModellingToolState.ModelPoint> row = mts.rows.get(r);
                // Lines within row
                List<Vec3DInt> rowXyz = new ArrayList<>(row.size());
                for (ModellingToolState.ModelPoint p : row) rowXyz.add(p.pos());
                renderLineStrip(rowXyz, camPos);
                // Point boxes
                for (int c = 0; c < row.size(); c++) {
                    ModellingToolState.ModelPoint mpt = row.get(c);
                    boolean ptSel = r == mts.selectedRow && c == mts.selectedPoint;
                    boolean activeRow = r == mts.currentRowIndex;
                    float pr = ptSel ? 1.0f : activeRow ? 0.80f : 0.55f;
                    float pg = ptSel ? 0.80f : activeRow ? 0.55f : 0.50f;
                    float pb = ptSel ? 0.20f : activeRow ? 0.90f : 0.65f;
                    renderPointBox(mpt.pos(), pr, pg, pb, camPos);
                }
            }
            // Capture GL matrices unconditionally so GizmoProjection is valid for findNearestPointOnScreen
            // even before any point is selected (gizmo.render only captures when selectedPointObj != null).
            mts.getAxisTranslationGizmo().getProjection().capture(camPos);
            // Gizmo on selected point
            ModellingToolState.ModelPoint mSelPt = mts.selectedPointObj();
            if (mSelPt != null && (!cameraMoving || gizmoDragging)) {
                mts.getPlaneTranslationGizmo()
                        .render(
                                mSelPt.pos().x() + 0.5,
                                mSelPt.pos().y() + 0.5,
                                mSelPt.pos().z() + 0.5,
                                camPos,
                                0,
                                0,
                                0);
                mts.getAxisTranslationGizmo()
                        .render(
                                mSelPt.pos().x() + 0.5,
                                mSelPt.pos().y() + 0.5,
                                mSelPt.pos().z() + 0.5,
                                camPos,
                                0,
                                0,
                                0);
            }
            // Lines between adjacent rows (column-matched)
            if (mts.rows.size() >= 2) {
                GL11.glLineWidth(1.0f);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor4f(0.70f, 0.55f, 0.90f, 0.4f);
                for (int r = 0; r + 1 < mts.rows.size(); r++) {
                    List<ModellingToolState.ModelPoint> rowA = mts.rows.get(r);
                    List<ModellingToolState.ModelPoint> rowB = mts.rows.get(r + 1);
                    int maxC = Math.min(rowA.size(), rowB.size());
                    for (int c = 0; c < maxC; c++) {
                        ModellingToolState.ModelPoint a = rowA.get(c);
                        ModellingToolState.ModelPoint b = rowB.get(c);
                        GL11.glVertex3d(
                                a.pos().x() + 0.5 - camPos.x(),
                                a.pos().y() + 0.5 - camPos.y(),
                                a.pos().z() + 0.5 - camPos.z());
                        GL11.glVertex3d(
                                b.pos().x() + 0.5 - camPos.x(),
                                b.pos().y() + 0.5 - camPos.y(),
                                b.pos().z() + 0.5 - camPos.z());
                    }
                }
                GL11.glEnd();
            }
        }

        // ── Path tool point rendering ─────────────────────────────────────────
        if (DimensiumEditorMode.INSTANCE.isActive() && DimensiumEditorMode.INSTANCE.selectedTool == Tool.PATH) {
            PathToolState pathState = PathToolState.INSTANCE;
            // Capture GL matrices here unconditionally so GizmoProjection is valid even
            // before any point is selected (gizmo.render only captures when selectedIndex >= 0).
            pathState.getAxisTranslationGizmo().getProjection().capture(camPos);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            for (int i = 0; i < pathState.points.size(); i++) {
                PathToolState.PathPoint pathPt = pathState.points.get(i);
                boolean ptSel = i == pathState.selectedIndex;
                renderPointBox(pathPt.pos, ptSel ? 1.0f : 0.55f, ptSel ? 0.85f : 0.70f, 1.0f, camPos);
            }
            if (pathState.selectedIndex >= 0 && !pathState.points.isEmpty() && (!cameraMoving || gizmoDragging)) {
                PathToolState.PathPoint selPt = pathState.selectedPoint();
                if (selPt != null) {
                    pathState
                            .getPlaneTranslationGizmo()
                            .render(selPt.pos.x() + 0.5, selPt.pos.y() + 0.5, selPt.pos.z() + 0.5, camPos, 0, 0, 0);
                    pathState
                            .getAxisTranslationGizmo()
                            .render(selPt.pos.x() + 0.5, selPt.pos.y() + 0.5, selPt.pos.z() + 0.5, camPos, 0, 0, 0);
                }
            }
        }

        PerfTrace.pop();
        GL11.glPopAttrib();
        PerfTrace.end(16);
    }

    private static void renderPointBox(Vec3DInt worldPos, float r, float g, float b, Vec3DDouble camPos) {
        Vec3DDouble ptTrans = worldPos.toDouble().minus(camPos);
        GL11.glPushMatrix();
        GL11.glTranslated(ptTrans.x(), ptTrans.y(), ptTrans.z());
        WorldLines.setEyeForTranslation(ptTrans);
        GL11.glColor4f(r, g, b, (float) 1.0);
        drawBox(0, 0, 0, 1, 1, 1);
        GL11.glPopMatrix();
    }

    private static void renderLineStrip(List<Vec3DInt> xyzList, Vec3DDouble camPos) {
        if (xyzList.size() < 2) return;
        GL11.glLineWidth((float) 1.5);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f((float) 0.55, (float) 0.7, (float) 0.9, (float) 0.6);
        for (Vec3DInt p : xyzList)
            GL11.glVertex3d(p.x() + 0.5 - camPos.x(), p.y() + 0.5 - camPos.y(), p.z() + 0.5 - camPos.z());
        GL11.glEnd();
    }

    // ── Magic select preview (runs each frame while MAGIC_SELECT tool is active) ──

    private void updateMagicSelectPreview(Minecraft mc) {
        MovingObjectPosition mop = RenderUtils.raycastAtCursor();

        BuilderToolState bts = BuilderToolState.INSTANCE;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            bts.magicPreview = null;
            lastMagicPos = null;
            return;
        }

        Vec3DInt curMopPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
        if (curMopPos.equals(lastMagicPos)) return;
        lastMagicPos = curMopPos;

        MagicSelectToolState ts = MagicSelectToolState.INSTANCE;
        Set<Long> flooded = ts.floodFillFrom(mc.theWorld, mop);

        ChangeProposal p = ChangeProposal.forPreview();
        for (long key : flooded) {
            Vec3DInt bv = SelectionState.unpack(key);
            Block blk = WorldUtils.getBlock(mc.theWorld, bv);
            int meta = WorldUtils.getBlockMetadata(mc.theWorld, bv);
            p.proposed.put(ChangeProposal.packKey(bv), new int[] {Block.getIdFromBlock(blk), meta});
        }
        bts.magicPreview = p;
    }

    private static void renderMagicPreview(Minecraft mc, Vec3DDouble camPos, ChangeProposal preview) {
        if (preview == null || preview.proposed.isEmpty()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        beginProposalRender(mc, camPos);
        Tessellator t = Tessellator.instance;

        // Textured pass — fully opaque, exterior faces only.
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawBatchedTexturedFaces(t, preview.proposed, mc);

        // Glow — slightly more negative offset so no z-fighting with opaque pass.
        GL11.glPolygonOffset(-2.0f, -2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glColor4f(0.25f, 1.0f, 0.55f, 0.05f + 0.07f * pulse);
        GhostRenderer.drawExteriorFacesSingleColor(t, preview.proposed, null);
        RenderUtils.unsetGhostRendering();

        // Wireframe pass.
        drawProposalWireframe(preview, t, camPos, 0.40f, 1.0f, 0.55f, pulse);

        GL11.glPopMatrix();
    }

    // ── Selection wireframe cache ─────────────────────────────────────────────

    private void updateSelWireframeCache(SelectionState sel) {
        if (sel.renderVersion == cachedSelVersion) return;
        cachedSelWire = computeSelWireframe(sel.getSelectedBlocks());
        cachedSelVersion = sel.renderVersion;
    }

    private void drawSelWireframe(Vec3DDouble camPos) {
        if (cachedSelWire == null || cachedSelWire.length == 0) return;
        // Vertices are world-space ints; offset by camPos → camera-relative. Eye = origin.
        WorldLines.setEye(Vec3DDouble.ZERO);
        WorldLines.drawIntWireframeCache(cachedSelWire, camPos);
    }

    private static int[] computeSelWireframe(Set<Long> blockSet) {
        HashMap<Long, Integer> edgeMask = getEdgeMask(blockSet);
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

    @Nonnull
    private static HashMap<Long, Integer> getEdgeMask(Set<Long> blockSet) {
        HashMap<Long, Integer> edgeMask = new HashMap<>(blockSet.size() * 4);
        for (long packed : blockSet) {
            Vec3DInt bv = SelectionState.unpack(packed);
            for (int face = 0; face < 6; face++) {
                if (blockSet.contains(SelectionState.pack(
                        bv.plus(GhostRenderer.NX[face], GhostRenderer.NY[face], GhostRenderer.NZ[face])))) continue;
                int axisBit = GhostRenderer.FACE_AXIS_BIT[face];
                for (int[] e : GhostRenderer.FACE_EDGES[face]) {
                    long ek = ((long) e[0] << 60) | SelectionState.pack(bv.plus(e[1], e[2], e[3]));
                    edgeMask.compute(ek, (k, prev) -> prev == null ? axisBit : prev | axisBit);
                }
            }
        }
        return edgeMask;
    }

    // ── Package-private draw helpers (used by BrushPreviewRenderer, HologramRenderer) ──

    static long lPack(int x, int y, int z) {
        return ((long) (x + 4096) << 26) | ((long) (y + 4096) << 13) | (z + 4096);
    }

    static void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        WorldLines.drawBox(x1, y1, z1, x2, y2, z2);
    }

    private static void renderProposalPreview(Minecraft mc, Vec3DDouble camPos, ChangeProposal drag) {
        if (drag == null || drag.proposed.isEmpty()) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        beginProposalRender(mc, camPos);
        Tessellator t = Tessellator.instance;

        // Pass 0: removals — orange tint over existing blocks, exterior faces only.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0f, 0.40f, 0.10f, 0.70f);
        GhostRenderer.drawExteriorFacesSingleColor(t, drag.proposed, bm -> bm[0] == 0);

        // Pass 1: textured additions fully opaque, exterior faces only.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawBatchedTexturedFaces(t, drag.proposed, mc);

        // Pass 2: colored fallback for non-standard render type additions only, exterior faces only.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.6f, 0.85f, 1.0f, 1.0f);
        GhostRenderer.drawExteriorFacesSingleColor(
                t,
                drag.proposed,
                bm -> bm[0] != 0
                        && (Block.getBlockById(bm[0]) == null
                                || Block.getBlockById(bm[0]).getRenderType() != 0));

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
        RenderUtils.unsetGhostRendering();

        // Pass 3: crease-edge wireframe around the exterior of the proposed shape.
        drawProposalWireframe(drag, t, camPos, 0.75f, 0.90f, 1.0f, pulse);

        GL11.glPopMatrix();
    }

    private static void beginProposalRender(Minecraft mc, Vec3DDouble camPos) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CW);
        GL11.glPushMatrix();
        GL11.glTranslated(-camPos.x(), -camPos.y(), -camPos.z());
    }

    private static void rebuildProposalWireIfNeeded(ChangeProposal drag) {
        if (drag.proposed.size() == drag.wireCacheSize) return;
        drag.wireCacheSize = drag.proposed.size();

        Vec3DInt min = Vec3DInt.from(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (long key : drag.proposed.keySet()) {
            min = min.min(ChangeProposal.unpackKey(key));
        }
        drag.wireOrigin = min;

        List<Vec3DInt> local = new ArrayList<>(drag.proposed.size());
        for (long key : drag.proposed.keySet()) {
            local.add(ChangeProposal.unpackKey(key).minus(min));
        }
        drag.cachedWire = GhostRenderer.INSTANCE.computeLocalWireframe(local);
    }

    // ── Elevation tool terrain-projected preview ──────────────────────────────

    private void renderElevationPreview(Minecraft mc, Vec3DDouble camPos) {
        MovingObjectPosition mop = RenderUtils.raycastAtCursor();
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        ElevationToolState s = ElevationToolState.INSTANCE;
        int cx = mop.blockX, cz = mop.blockZ;
        int radius = Math.max(1, s.elevationRadius);

        boolean held = Mouse.isButtonDown(KeyConstants.RMB);
        float pulse = held ? 0.10f * (float) Math.sin(System.currentTimeMillis() / 180.0) : 0f;

        Tessellator t = Tessellator.instance;

        // ── Fill: top faces colored by falloff weight ─────────────────────────
        t.startDrawingQuads();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float r = Vec2DFloat.from(dx, dz).length() / radius;
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
                t.addVertex(wx - camPos.x(), topY - camPos.y(), wz - camPos.z());
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx + 1 - camPos.x(), topY - camPos.y(), wz - camPos.z());
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx + 1 - camPos.x(), topY - camPos.y(), wz + 1 - camPos.z());
                t.setColorRGBA_F(cr, cg, cb, alpha);
                t.addVertex(wx - camPos.x(), topY - camPos.y(), wz + 1 - camPos.z());
            }
        }
        t.draw();

        // ── Ring: boundary edges where the circle meets outside ───────────────
        // Vertices are camera-relative (no active glTranslated) → eye = origin.
        WorldLines.setEye(Vec3DDouble.ZERO);
        GL11.glColor4f(0.45f, 0.88f, 0.32f, 0.95f);
        WorldLines.prepareSegmentBatch();
        t.startDrawingQuads();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float r = Vec2DFloat.from(dx, dz).length() / radius;
                if (r > 1f) continue;

                int wx = cx + dx, wz = cz + dz;
                double topY = elevPreviewTopY(mc, wx, wz) + 1.002;

                if (Vec2DFloat.from(dx + 1, dz).length() > radius) {
                    WorldLines.addSegment(
                            t,
                            wx + 1 - camPos.x(),
                            topY - camPos.y(),
                            wz - camPos.z(),
                            wx + 1 - camPos.x(),
                            topY - camPos.y(),
                            wz + 1 - camPos.z(),
                            WorldLines.W_THIN);
                }
                if (Vec2DFloat.from(dx - 1, dz).length() > radius) {
                    WorldLines.addSegment(
                            t,
                            wx - camPos.x(),
                            topY - camPos.y(),
                            wz - camPos.z(),
                            wx - camPos.x(),
                            topY - camPos.y(),
                            wz + 1 - camPos.z(),
                            WorldLines.W_THIN);
                }
                if (Vec2DFloat.from(dx, dz + 1).length() > radius) {
                    WorldLines.addSegment(
                            t,
                            wx - camPos.x(),
                            topY - camPos.y(),
                            wz + 1 - camPos.z(),
                            wx + 1 - camPos.x(),
                            topY - camPos.y(),
                            wz + 1 - camPos.z(),
                            WorldLines.W_THIN);
                }
                if (Vec2DFloat.from(dx, dz - 1).length() > radius) {
                    WorldLines.addSegment(
                            t,
                            wx - camPos.x(),
                            topY - camPos.y(),
                            wz - camPos.z(),
                            wx + 1 - camPos.x(),
                            topY - camPos.y(),
                            wz - camPos.z(),
                            WorldLines.W_THIN);
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
                if (mc.theWorld.getBlock(wx, y, wz) != Blocks.air) return y;
            }
            return 0;
        }
        for (int y = 255; y >= 0; y--) {
            if (mc.theWorld.getBlock(wx, y, wz) != Blocks.air) return y;
        }
        return 0;
    }

    static void drawFilledBox(float x2, float y2, float z2) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        GhostRenderer.addBoxFaces(t, x2, y2, z2);
        t.draw();
    }

    /** Draws the crease-edge wireframe for a proposal, rebuilding the cache if needed. */
    private static void drawProposalWireframe(
            ChangeProposal proposal, Tessellator t, Vec3DDouble camPos, float r, float g, float b, float pulse) {
        rebuildProposalWireIfNeeded(proposal);
        if (proposal.cachedWire == null || proposal.cachedWire.length == 0) return;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(r, g, b, 0.70f + 0.20f * pulse);
        GL11.glPushMatrix();
        GL11.glTranslated(proposal.wireOrigin.x(), proposal.wireOrigin.y(), proposal.wireOrigin.z());
        WorldLines.setEye(camPos.minus(proposal.wireOrigin.toDouble()));
        GhostRenderer.drawWireframeCache(t, proposal.cachedWire);
        GL11.glPopMatrix();
    }

    /**
     * Draws standard-render-type blocks in a proposal map as textured exterior faces,
     * batching flushes every 2048 quads. Computes and applies per-block color tint.
     * Caller must bind the block texture atlas and set GL color before calling.
     */
    private static void drawBatchedTexturedFaces(Tessellator t, Map<Long, int[]> proposed, Minecraft mc) {
        t.startDrawingQuads();
        int batched = 0;
        for (Map.Entry<Long, int[]> e : proposed.entrySet()) {
            long key = e.getKey();
            int[] bm = e.getValue();
            Block blk = Block.getBlockById(bm[0]);
            if (blk == null || blk == Blocks.air || blk.getRenderType() != 0) continue;
            Vec3DInt bv = ChangeProposal.unpackKey(key);
            int tint = 0xFFFFFF;
            try {
                tint = blk.colorMultiplier(mc.theWorld, bv.x(), bv.y(), bv.z());
            } catch (Exception ignored) {
            }
            for (int face = 0; face < 6; face++) {
                long nk = ChangeProposal.packKey(
                        bv.x() + GhostRenderer.NX[face],
                        bv.y() + GhostRenderer.NY[face],
                        bv.z() + GhostRenderer.NZ[face]);
                if (!proposed.containsKey(nk)) {
                    GhostRenderer.addTexturedFace(t, bv, blk, bm[1], face, tint);
                    if (++batched % 2048 == 0) {
                        t.draw();
                        t.startDrawingQuads();
                    }
                }
            }
        }
        t.draw();
    }
}
