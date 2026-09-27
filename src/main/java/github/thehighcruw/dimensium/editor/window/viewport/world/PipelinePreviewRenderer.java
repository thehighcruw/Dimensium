/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.Curve;
import github.thehighcruw.dimensium.editor.pipeline.Skeleton;
import github.thehighcruw.dimensium.editor.pipeline.SkeletonNode;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.glu.GLU;

/**
 * Renders a {@link BlockMap} to an off-screen FBO for display as an ImGui image.
 * Mirrors the architecture of {@link ClipboardRenderer}.
 */
@SideOnly(Side.CLIENT)
public class PipelinePreviewRenderer {

    public static final int TEX_SIZE = 512;

    private int fboId = -1;
    private int texId = -1;
    private int whiteLightmap = -1;
    public boolean fboFailed = false;
    public String failReason = null;

    private enum RenderMode {
        BLOCKS,
        SKELETON,
        CURVE
    }

    private RenderMode renderMode = RenderMode.BLOCKS;
    private Map<Long, int[]> localBlocks = null;
    private Skeleton localSkeleton = null;
    private Curve localCurve = null;
    private Vec3DInt dims = null;
    private Vec3DFloat effectiveCenter = null;
    private float effectiveSpan = 1f;
    private boolean dirty = false;

    private float azim = 225f;
    private float elev = 28f;
    private float zoom = 1.0f;

    private void computeEffectiveCamera(Map<Long, int[]> blocks) {
        int count = blocks.size();
        if (count == 0) return;

        double sumX = 0, sumY = 0, sumZ = 0;
        for (long key : blocks.keySet()) {
            sumX += key & 0x3FFFFFF;
            sumY += (key >> 26) & 0x3FFFFFF;
            sumZ += (key >> 52) & 0x3FFFFFF;
        }
        float cx = (float) (sumX / count);
        float cy = (float) (sumY / count);
        float cz = (float) (sumZ / count);
        effectiveCenter = Vec3DFloat.from(cx, cy, cz);

        float[] distances = new float[count];
        int di = 0;
        for (long key : blocks.keySet()) {
            float dx = (key & 0x3FFFFFF) - cx;
            float dy = ((key >> 26) & 0x3FFFFFF) - cy;
            float dz = ((key >> 52) & 0x3FFFFFF) - cz;
            distances[di++] = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        Arrays.sort(distances);
        effectiveSpan = distances[(int) (count * 0.95f)] * 2f + 2f;
    }

    public void setCamera(float azim, float elev, float zoom) {
        this.azim = azim;
        this.elev = elev;
        this.zoom = zoom;
        this.dirty = hasContent();
    }

    public void setSkeleton(Skeleton skeleton) {
        localSkeleton = skeleton;
        localBlocks = null;
        renderMode = RenderMode.SKELETON;
        if (skeleton == null || skeleton.roots.isEmpty()) {
            dirty = false;
            return;
        }
        computeSkeletonCamera(skeleton.roots.get(0));
        dirty = true;
    }

    private void computeSkeletonCamera(SkeletonNode root) {
        // Collect all node positions
        java.util.List<Vec3DInt> positions = new java.util.ArrayList<>();
        collectSkeletonPositions(root, positions);
        if (positions.isEmpty()) return;

        double sumX = 0, sumY = 0, sumZ = 0;
        for (Vec3DInt pos : positions) {
            sumX += pos.x();
            sumY += pos.y();
            sumZ += pos.z();
        }
        float cx = (float) (sumX / positions.size());
        float cy = (float) (sumY / positions.size());
        float cz = (float) (sumZ / positions.size());
        effectiveCenter = Vec3DFloat.from(cx, cy, cz);

        float maxDist = 0f;
        for (Vec3DInt pos : positions) {
            float dx = pos.x() - cx;
            float dy = pos.y() - cy;
            float dz = pos.z() - cz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > maxDist) maxDist = dist;
        }
        effectiveSpan = maxDist * 2f + 2f;
    }

    private static void collectSkeletonPositions(SkeletonNode node, java.util.List<Vec3DInt> out) {
        out.add(node.position);
        for (SkeletonNode child : node.children) collectSkeletonPositions(child, out);
    }

    public void setCurve(Curve curve) {
        localBlocks = null;
        localSkeleton = null;
        localCurve = curve;
        renderMode = RenderMode.CURVE;
        if (curve == null || curve.points.isEmpty()) {
            dirty = false;
            return;
        }
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Vec3DFloat point : curve.points) {
            sumX += point.x();
            sumY += point.y();
            sumZ += point.z();
        }
        int count = curve.points.size();
        float cx = (float) (sumX / count);
        float cy = (float) (sumY / count);
        float cz = (float) (sumZ / count);
        effectiveCenter = Vec3DFloat.from(cx, cy, cz);
        float maxDist = 0f;
        for (Vec3DFloat point : curve.points) {
            float dx = point.x() - cx;
            float dy = point.y() - cy;
            float dz = point.z() - cz;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > maxDist) maxDist = dist;
        }
        effectiveSpan = maxDist * 2f + 2f;
        dirty = true;
    }

    public void setBlockMap(BlockMap map) {
        localSkeleton = null;
        renderMode = RenderMode.BLOCKS;
        if (map == null || map.entries().isEmpty()) {
            localBlocks = null;
            dims = null;
            dirty = false;
            return;
        }

        // Compute world-space bounding box
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Map.Entry<Long, int[]> entry : map.entries().entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            if (pos.x() < minX) minX = pos.x();
            if (pos.y() < minY) minY = pos.y();
            if (pos.z() < minZ) minZ = pos.z();
            if (pos.x() > maxX) maxX = pos.x();
            if (pos.y() > maxY) maxY = pos.y();
            if (pos.z() > maxZ) maxZ = pos.z();
        }

        // Normalize to 0-based local coords
        Map<Long, int[]> normalized = new HashMap<>();
        for (Map.Entry<Long, int[]> entry : map.entries().entrySet()) {
            Vec3DInt pos = BlockMap.unpackKey(entry.getKey());
            int lx = pos.x() - minX;
            int ly = pos.y() - minY;
            int lz = pos.z() - minZ;
            normalized.put(BlockMapBlockAccess.packKey(lx, ly, lz), entry.getValue());
        }

        localBlocks = normalized;
        dims = Vec3DInt.from(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        computeEffectiveCamera(normalized);
        dirty = true;
    }

    public int getTexture() {
        return (fboFailed || texId == -1) ? -1 : texId;
    }

    public boolean hasContent() {
        if (renderMode == RenderMode.SKELETON) return localSkeleton != null && !localSkeleton.roots.isEmpty();
        if (renderMode == RenderMode.CURVE) return localCurve != null && !localCurve.points.isEmpty();
        return localBlocks != null && !localBlocks.isEmpty();
    }

    /** Call each frame BEFORE 2D rendering begins. */
    public void maybeRebake() {
        if (!dirty || fboFailed) return;
        if (renderMode == RenderMode.BLOCKS && localBlocks == null) return;
        if (renderMode == RenderMode.SKELETON && (localSkeleton == null || localSkeleton.roots.isEmpty())) return;
        if (renderMode == RenderMode.CURVE && (localCurve == null || localCurve.points.isEmpty())) return;
        dirty = false;
        rebake();
    }

    private void ensureFbo() {
        if (fboId != -1) return;
        try {
            whiteLightmap = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteLightmap);
            ByteBuffer white = ByteBuffer.allocateDirect(4);
            white.put((byte) 0xFF)
                    .put((byte) 0xFF)
                    .put((byte) 0xFF)
                    .put((byte) 0xFF)
                    .flip();
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, white);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            fboId = EXTFramebufferObject.glGenFramebuffersEXT();
            texId = GL11.glGenTextures();
            int depthId = EXTFramebufferObject.glGenRenderbuffersEXT();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    TEX_SIZE,
                    TEX_SIZE,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, depthId);
            EXTFramebufferObject.glRenderbufferStorageEXT(
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, TEX_SIZE, TEX_SIZE);
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);

            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                    GL11.GL_TEXTURE_2D,
                    texId,
                    0);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(
                    EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                    EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    depthId);

            int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

            if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                fboFailed = true;
                failReason = "FBO incomplete: 0x" + Integer.toHexString(status);
            }
        } catch (Exception e) {
            fboFailed = true;
            failReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private void rebake() {
        ensureFbo();
        if (fboFailed || fboId == -1) return;
        if (renderMode == RenderMode.SKELETON) {
            rebakeSkeleton();
            return;
        }
        if (renderMode == RenderMode.CURVE) {
            rebakeCurve();
            return;
        }
        if (localBlocks == null || dims == null) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        Tessellator tess = Tessellator.instance;
        boolean tessStarted = false;
        Minecraft mc = Minecraft.getMinecraft();
        int savedAO = mc.gameSettings.ambientOcclusion;

        try {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            GL11.glViewport(0, 0, TEX_SIZE, TEX_SIZE);
            GL11.glClearColor(0.05f, 0.05f, 0.07f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GLU.gluPerspective(45f, 1f, 0.1f, 1000f);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            Vec3DFloat center =
                    effectiveCenter != null ? effectiveCenter : dims.toFloat().times(0.5f);
            float span = effectiveSpan > 0 ? effectiveSpan : dims.toFloat().length();
            float dist = (span * 0.6f + 1f) / zoom;
            double elevRad = Math.toRadians(elev);
            double azimRad = Math.toRadians(azim);

            Vec3DFloat eye = center.plus(Vec3DFloat.from(
                    dist * (float) (Math.cos(elevRad) * Math.cos(azimRad)),
                    dist * (float) Math.sin(elevRad),
                    dist * (float) (Math.cos(elevRad) * Math.sin(azimRad))));

            GLU.gluLookAt(eye.x(), eye.y(), eye.z(), center.x(), center.y(), center.z(), 0f, 1f, 0f);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, whiteLightmap);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

            BlockMapBlockAccess bAccess = new BlockMapBlockAccess(localBlocks, dims);
            RenderBlocks rb = new RenderBlocks(bAccess);
            rb.useInventoryTint = false;
            rb.renderAllFaces = true;
            mc.gameSettings.ambientOcclusion = 0;

            tess.startDrawingQuads();
            tessStarted = true;

            for (Map.Entry<Long, int[]> entry : localBlocks.entrySet()) {
                long key = entry.getKey();
                int lx = (int) (key & 0x3FFFFFF);
                int ly = (int) ((key >> 26) & 0x3FFFFFF);
                int lz = (int) ((key >> 52) & 0x3FFFFFF);
                int[] bd = entry.getValue();
                if (bd != null) {
                    Block block = Block.getBlockById(bd[0]);
                    if (block != null && block != Blocks.air) {
                        rb.renderBlockByRenderType(block, lx, ly, lz);
                    }
                }
            }

            tess.draw();
            tessStarted = false;
        } catch (Exception e) {
            if (failReason == null) failReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (tessStarted) {
                try {
                    tess.draw();
                } catch (Exception ignored) {
                }
            }
            mc.gameSettings.ambientOcclusion = savedAO;
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void rebakeSkeleton() {
        if (localSkeleton == null || localSkeleton.roots.isEmpty()) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        try {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            GL11.glViewport(0, 0, TEX_SIZE, TEX_SIZE);
            GL11.glClearColor(0.05f, 0.05f, 0.07f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GLU.gluPerspective(45f, 1f, 0.1f, 1000f);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            Vec3DFloat center = effectiveCenter != null ? effectiveCenter : Vec3DFloat.from(0f, 0f, 0f);
            float span = effectiveSpan > 0 ? effectiveSpan : 10f;
            float dist = (span * 0.6f + 1f) / zoom;
            double elevRad = Math.toRadians(elev);
            double azimRad = Math.toRadians(azim);

            Vec3DFloat eye = center.plus(Vec3DFloat.from(
                    dist * (float) (Math.cos(elevRad) * Math.cos(azimRad)),
                    dist * (float) Math.sin(elevRad),
                    dist * (float) (Math.cos(elevRad) * Math.sin(azimRad))));

            GLU.gluLookAt(eye.x(), eye.y(), eye.z(), center.x(), center.y(), center.z(), 0f, 1f, 0f);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);

            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);
            for (SkeletonNode root : localSkeleton.roots) {
                drawSkeletonLines(root, 0);
            }
            GL11.glEnd();

            // Billboard vectors for circles
            Vec3DFloat forward = eye.minus(center).normalize();
            Vec3DFloat forwardCrossUp = forward.cross(Vec3DFloat.from(0f, 1f, 0f));
            Vec3DFloat billboardRight = forwardCrossUp.length() < 0.001f
                    ? forward.cross(Vec3DFloat.from(1f, 0f, 0f)).normalize()
                    : forwardCrossUp.normalize();
            Vec3DFloat billboardUp = billboardRight.cross(forward).normalize();

            // Draw circles at each node to visualise branch radius
            GL11.glLineWidth(1.0f);
            GL11.glBegin(GL11.GL_LINES);
            for (SkeletonNode root : localSkeleton.roots) {
                drawSkeletonCircles(root, billboardRight, billboardUp);
            }
            GL11.glEnd();
        } catch (Exception e) {
            if (failReason == null) failReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void drawSkeletonLines(SkeletonNode node, int depth) {
        float t = Math.max(0f, 1f - depth * 0.08f);
        GL11.glColor3f(0.4f + t * 0.4f, 0.6f + t * 0.2f, 1.0f);
        for (SkeletonNode child : node.children) {
            GL11.glVertex3f(node.position.x(), node.position.y(), node.position.z());
            GL11.glVertex3f(child.position.x(), child.position.y(), child.position.z());
            drawSkeletonLines(child, depth + 1);
        }
    }

    private void rebakeCurve() {
        if (localCurve == null || localCurve.points.isEmpty()) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        try {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
            GL11.glViewport(0, 0, TEX_SIZE, TEX_SIZE);
            GL11.glClearColor(0.05f, 0.05f, 0.07f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GLU.gluPerspective(45f, 1f, 0.1f, 1000f);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();

            Vec3DFloat center = effectiveCenter != null ? effectiveCenter : Vec3DFloat.from(0f, 0f, 0f);
            float span = effectiveSpan > 0 ? effectiveSpan : 10f;
            float dist = (span * 0.6f + 1f) / zoom;
            double elevRad = Math.toRadians(elev);
            double azimRad = Math.toRadians(azim);

            Vec3DFloat eye = center.plus(Vec3DFloat.from(
                    dist * (float) (Math.cos(elevRad) * Math.cos(azimRad)),
                    dist * (float) Math.sin(elevRad),
                    dist * (float) (Math.cos(elevRad) * Math.sin(azimRad))));

            GLU.gluLookAt(eye.x(), eye.y(), eye.z(), center.x(), center.y(), center.z(), 0f, 1f, 0f);

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);

            GL11.glLineWidth(2.0f);
            GL11.glColor3f(0.3f, 0.85f, 1.0f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (Vec3DFloat point : localCurve.points) {
                GL11.glVertex3f(point.x(), point.y(), point.z());
            }
            if (localCurve.closed) {
                Vec3DFloat first = localCurve.points.get(0);
                GL11.glVertex3f(first.x(), first.y(), first.z());
            }
            GL11.glEnd();

            // Draw sample points as small crosses
            float tickSize = effectiveSpan * 0.015f;
            GL11.glLineWidth(1.0f);
            GL11.glColor3f(1.0f, 0.6f, 0.2f);
            GL11.glBegin(GL11.GL_LINES);
            for (Vec3DFloat point : localCurve.points) {
                GL11.glVertex3f(point.x() - tickSize, point.y(), point.z());
                GL11.glVertex3f(point.x() + tickSize, point.y(), point.z());
                GL11.glVertex3f(point.x(), point.y(), point.z() - tickSize);
                GL11.glVertex3f(point.x(), point.y(), point.z() + tickSize);
            }
            GL11.glEnd();
        } catch (Exception e) {
            if (failReason == null) failReason = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void drawSkeletonCircles(SkeletonNode node, Vec3DFloat right, Vec3DFloat up) {
        float cx = node.position.x();
        float cy = node.position.y();
        float cz = node.position.z();
        float r = node.radius;
        GL11.glColor3f(1.0f, 0.75f, 0.35f);
        int segments = 10;
        float prevX = cx + right.x() * r;
        float prevY = cy + right.y() * r;
        float prevZ = cz + right.z() * r;
        for (int seg = 1; seg <= segments; seg++) {
            double angle = 2 * Math.PI * seg / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float nextX = cx + (right.x() * cos + up.x() * sin) * r;
            float nextY = cy + (right.y() * cos + up.y() * sin) * r;
            float nextZ = cz + (right.z() * cos + up.z() * sin) * r;
            GL11.glVertex3f(prevX, prevY, prevZ);
            GL11.glVertex3f(nextX, nextY, nextZ);
            prevX = nextX;
            prevY = nextY;
            prevZ = nextZ;
        }
        for (SkeletonNode child : node.children) drawSkeletonCircles(child, right, up);
    }
}
