/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window.viewport.world;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;

/**
 * Shader-compatible line rendering.
 *
 * GL_LINES are invisible through OptiFine's gbuffer shader pipeline because
 * many shader packs' gbuffers_basic program discards or zeros out line
 * primitives. This class replaces every GL_LINES draw with camera-facing
 * billboard quads that are always visible.
 *
 * Usage pattern per draw call:
 * 1. Determine the eye (camera) position in the CURRENT GL local frame:
 * - If no glTranslated is active: setEyeForTranslation(0, 0, 0)
 * - If glTranslated(tx,ty,tz) is active: setEyeForTranslation(tx,ty,tz)
 * - If rotation is also active: compute eye via setEyeRotated(...)
 * 2. Set GL color with glColor4f (no texture needed).
 * 3. Call drawBox / drawWireframeCache / addSegment.
 *
 * All addSegment calls must be wrapped in tessellator startDrawingQuads/draw.
 * The drawBox / drawWireframeCache helpers handle that internally.
 */
public class WorldLines {

    // Half-widths in world units for each usage context.
    // At typical building distances (5–30 blocks) these appear as 1–3 pixel lines.
    static final float W_THIN = 0.018f; // crease / selection wireframes
    static final float W_SEL = 0.028f; // selection box outlines
    static final float W_GIZMO = 0.038f; // gizmo arrow shafts and rings
    static final float W_HOT = 0.060f; // hover highlight glow pass

    // Eye (camera) position in the CURRENT GL local coordinate frame.
    // Must be set via setEyeForTranslation / setEye before any draw call.
    static Vec3DDouble eye = Vec3DDouble.ZERO;

    /**
     * Set eye from the active glTranslated translation vector.
     * If glTranslated(t) is the innermost active transform, the camera
     * sits at local -t. For no active translate, pass Vec3DDouble.ZERO.
     */
    static void setEyeForTranslation(Vec3DDouble translation) {
        eye = translation.negate();
    }

    /** Set eye directly in local coordinate space. */
    static void setEye(Vec3DDouble eyePos) {
        eye = eyePos;
    }

    /**
     * Set eye for a glTranslated + glRotatef context (gizmo renders).
     * R is the row-major 3x3 rotation from ShapeMath.buildRotationMatrix.
     * tx/ty/tz are the glTranslated arguments (localOrigin - cameraWorld).
     * Camera in translated-then-rotated local space = R^T * (-tx,-ty,-tz).
     */
    static void setEyeRotated(Mat3DFloat R, double tx, double ty, double tz) {
        eye = R.mulTransposeD(-tx, -ty, -tz);
    }

    /**
     * Emit a camera-facing billboard quad for one segment from A to B.
     * The tessellator must already be in startDrawingQuads mode.
     * halfW is the half-width of the quad in world units.
     */
    /** Disable cull face before a batch of addSegment calls (call once before startDrawingQuads). */
    static void prepareSegmentBatch() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    static void addSegment(Tessellator t, Vec3DDouble a, Vec3DDouble b, float halfW) {
        addSegment(t, a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), halfW);
    }

    static void addSegment(Tessellator t, double ax, double ay, double az, double bx, double by, double bz,
        float halfW) {
        // Eye-to-midpoint direction
        Vec3DDouble eyeToMid = Vec3DDouble
            .from((ax + bx) * 0.5 - eye.x(), (ay + by) * 0.5 - eye.y(), (az + bz) * 0.5 - eye.z());

        // Segment direction (normalised)
        Vec3DDouble seg = Vec3DDouble.from(bx - ax, by - ay, bz - az);
        double segLen = seg.length();
        if (segLen < 1e-9) return;
        Vec3DDouble segDir = seg.divide(segLen);

        // Perpendicular = cross(segDir, eyeToMid) — points "up" relative to camera
        Vec3DDouble perp = segDir.cross(eyeToMid);
        double pl = perp.length();
        if (pl < 1e-9) {
            // Segment points directly at camera — choose any perpendicular
            perp = Math.abs(segDir.x()) < 0.9 ? Vec3DDouble.from(0, segDir.z(), -segDir.y())
                : Vec3DDouble.from(segDir.z(), 0, -segDir.x());
            pl = perp.length();
            if (pl < 1e-9) return;
        }
        Vec3DDouble pw = perp.times(halfW / pl);
        double px = pw.x(), py = pw.y(), pz = pw.z();

        // CW-from-camera winding — works whether GL_FRONT_FACE is CW or CCW
        // when combined with glDisable(GL_CULL_FACE) below.
        t.addVertex(ax - px, ay - py, az - pz);
        t.addVertex(ax + px, ay + py, az + pz);
        t.addVertex(bx + px, by + py, bz + pz);
        t.addVertex(bx - px, by - py, bz - pz);
    }

    /**
     * Draw a 12-edge wireframe box as billboard quads.
     * Handles tessellator setup/teardown.
     */
    static void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        // Bottom ring
        addSegment(t, x1, y1, z1, x2, y1, z1, WorldLines.W_SEL);
        addSegment(t, x2, y1, z1, x2, y1, z2, WorldLines.W_SEL);
        addSegment(t, x2, y1, z2, x1, y1, z2, WorldLines.W_SEL);
        addSegment(t, x1, y1, z2, x1, y1, z1, WorldLines.W_SEL);
        // Top ring
        addSegment(t, x1, y2, z1, x2, y2, z1, WorldLines.W_SEL);
        addSegment(t, x2, y2, z1, x2, y2, z2, WorldLines.W_SEL);
        addSegment(t, x2, y2, z2, x1, y2, z2, WorldLines.W_SEL);
        addSegment(t, x1, y2, z2, x1, y2, z1, WorldLines.W_SEL);
        // Vertical edges
        addSegment(t, x1, y1, z1, x1, y2, z1, WorldLines.W_SEL);
        addSegment(t, x2, y1, z1, x2, y2, z1, WorldLines.W_SEL);
        addSegment(t, x2, y1, z2, x2, y2, z2, WorldLines.W_SEL);
        addSegment(t, x1, y1, z2, x1, y2, z2, WorldLines.W_SEL);
        t.draw();
    }

    /**
     * Draw wireframe from float[](x1,y1,z1,x2,y2,z2,...) as billboard quads.
     * Handles tessellator setup/teardown and batching.
     */
    static void drawWireframeCache(Tessellator t, float[] verts) {
        if (verts == null || verts.length < 6) return;
        GL11.glDisable(GL11.GL_CULL_FACE);
        t.startDrawingQuads();
        int batched = 0;
        for (int i = 0; i + 5 < verts.length; i += 6) {
            addSegment(
                t,
                verts[i],
                verts[i + 1],
                verts[i + 2],
                verts[i + 3],
                verts[i + 4],
                verts[i + 5],
                WorldLines.W_THIN);
            if (++batched % 2048 == 0) {
                t.draw();
                t.startDrawingQuads();
            }
        }
        t.draw();
    }

    /**
     * Draw wireframe from int[](x1,y1,z1,x2,y2,z2,...) with a world-space offset
     * subtracted from each vertex (for SelectionRenderer's cached int[] wireframe).
     * Eye must already be set to (0,0,0) since the result is camera-relative.
     */
    static void drawIntWireframeCache(int[] verts, Vec3DDouble offset) {
        if (verts == null || verts.length < 6) return;
        GL11.glDisable(GL11.GL_CULL_FACE);
        Tessellator.instance.startDrawingQuads();
        int batched = 0;
        for (int i = 0; i + 5 < verts.length; i += 6) {
            addSegment(
                Tessellator.instance,
                verts[i] - offset.x(),
                verts[i + 1] - offset.y(),
                verts[i + 2] - offset.z(),
                verts[i + 3] - offset.x(),
                verts[i + 4] - offset.y(),
                verts[i + 5] - offset.z(),
                WorldLines.W_THIN);
            if (++batched % 2048 == 0) {
                Tessellator.instance.draw();
                Tessellator.instance.startDrawingQuads();
            }
        }
        Tessellator.instance.draw();
    }
}
