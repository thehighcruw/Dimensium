/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests the raycastFromMouse ray direction math in isolation (no Minecraft deps).
 *
 * MC conventions:
 * yaw=0 → looking +Z (south)
 * yaw=90 → looking -X (west)
 * pitch=0 → horizontal
 * pitch=90 → looking straight down
 *
 * Screen conventions:
 * mouseX=0 → left edge, mouseX=SW → right edge
 * mouseY=0 → top edge, mouseY=SH → bottom edge
 *
 * MC uses clockwise yaw, so ndcX = 1 - 2*mouseX/SW (flips horizontal).
 */
public class RaycastTest {

    private static final int SW = 800, SH = 600;
    private static final double FOV_Y_DEG = 70.0;
    private static final double TAN_HY = Math.tan(Math.toRadians(FOV_Y_DEG / 2.0));
    private static final double TAN_HX = TAN_HY * SW / (double) SH;

    private static double[] rayDir(double yawDeg, double pitchDeg, int mouseX, int mouseY) {
        double ndcX = 1.0 - (2.0 * mouseX / SW);
        double ndcY = 1.0 - (2.0 * mouseY / SH);
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double lookX = -Math.sin(yaw) * Math.cos(pitch);
        double lookY = -Math.sin(pitch);
        double lookZ = Math.cos(yaw) * Math.cos(pitch);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double upX = -Math.sin(yaw) * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = Math.cos(yaw) * Math.sin(pitch);
        double rdx = lookX + rightX * ndcX * TAN_HX + upX * ndcY * TAN_HY;
        double rdy = lookY + upY * ndcY * TAN_HY;
        double rdz = lookZ + rightZ * ndcX * TAN_HX + upZ * ndcY * TAN_HY;
        return new double[] {rdx, rdy, rdz};
    }

    private static final int CX = SW / 2, CY = SH / 2;
    private static final int TOP = CY - 100, BOT = CY + 100;
    private static final int LEFT = CX - 100, RIGHT = CX + 100;

    // ── pitch=0, yaw=0 (looking south / +Z, level) ───────────────────────────

    @Test
    public void southLevelCenterRayYNearZero() {
        double[] center = rayDir(0, 0, CX, CY);
        assertEquals(0.0, center[1], 0.01);
    }

    @Test
    public void southLevelCenterRayZPositive() {
        assertTrue(rayDir(0, 0, CX, CY)[2] > 0);
    }

    @Test
    public void southLevelCursorAboveRaysUpward() {
        double[] above = rayDir(0, 0, CX, TOP);
        double[] center = rayDir(0, 0, CX, CY);
        assertTrue(above[1] > 0);
        assertTrue(above[1] > center[1]);
    }

    @Test
    public void southLevelCursorBelowRaysDownward() {
        double[] below = rayDir(0, 0, CX, BOT);
        double[] center = rayDir(0, 0, CX, CY);
        assertTrue(below[1] < 0);
        assertTrue(below[1] < center[1]);
    }

    @Test
    public void southLevelCursorRightRaysWest() {
        // MC CW yaw: screen-right = west = -X when facing south
        double[] rgt = rayDir(0, 0, RIGHT, CY);
        double[] center = rayDir(0, 0, CX, CY);
        assertTrue(rgt[0] < 0);
        assertTrue(rgt[0] < center[0]);
    }

    @Test
    public void southLevelCursorLeftRaysEast() {
        double[] lft = rayDir(0, 0, LEFT, CY);
        double[] center = rayDir(0, 0, CX, CY);
        assertTrue(lft[0] > 0);
        assertTrue(lft[0] > center[0]);
    }

    // ── pitch=45, yaw=0 (looking south-downward at 45°) ─────────────────────

    @Test
    public void southDown45CenterRayDownward() {
        assertTrue(rayDir(0, 45, CX, CY)[1] < 0);
    }

    @Test
    public void southDown45CursorAboveLessDownward() {
        double[] ctr = rayDir(0, 45, CX, CY);
        double[] above = rayDir(0, 45, CX, TOP);
        assertTrue(above[1] > ctr[1]);
    }

    @Test
    public void southDown45CursorBelowMoreDownward() {
        double[] ctr = rayDir(0, 45, CX, CY);
        double[] below = rayDir(0, 45, CX, BOT);
        assertTrue(below[1] < ctr[1]);
    }

    // ── pitch=0, yaw=90 (looking west / -X) ──────────────────────────────────

    @Test
    public void westLevelCenterRayXNegative() {
        assertTrue(rayDir(90, 0, CX, CY)[0] < 0);
    }

    @Test
    public void westLevelCursorAboveRaysUpward() {
        assertTrue(rayDir(90, 0, CX, TOP)[1] > 0);
    }

    @Test
    public void westLevelCursorRightRaysNorth() {
        // Facing west (-X), turn right → north (-Z). Screen-right = -Z.
        assertTrue(rayDir(90, 0, RIGHT, CY)[2] < 0);
    }

    @Test
    public void westLevelCursorLeftRaysSouth() {
        assertTrue(rayDir(90, 0, LEFT, CY)[2] > 0);
    }
}
