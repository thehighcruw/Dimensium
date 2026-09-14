/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.shared.math.Vec3DDouble;

@SideOnly(Side.CLIENT)
public class FreecamUtils {

    public static final double REACH = 512.0;

    public static Vec3 lookVec(EntityPlayer player) {
        FreecamEntity cam = FreecamState.INSTANCE.cameraEntity;
        if (cam != null) return camForward(cam);
        return player.getLookVec();
    }

    public static MovingObjectPosition rayTrace(Minecraft mc, double reach) {
        FreecamEntity cam = FreecamState.INSTANCE.cameraEntity;
        if (cam != null) {
            Vec3 dir = camForward(cam);
            Vec3 start = Vec3.createVectorHelper(cam.posX, cam.posY, cam.posZ);
            Vec3 end = Vec3.createVectorHelper(
                cam.posX + dir.xCoord * reach,
                cam.posY + dir.yCoord * reach,
                cam.posZ + dir.zCoord * reach);
            return mc.theWorld.rayTraceBlocks(start, end, false);
        }
        return mc.thePlayer.rayTrace(reach, 1.0f);
    }

    /** Forward unit vector from yaw/pitch of the given camera entity. */
    public static Vec3 camForward(FreecamEntity cam) {
        double yaw = Math.toRadians(cam.rotationYaw);
        double pitch = Math.toRadians(cam.rotationPitch);
        return Vec3
            .createVectorHelper(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    /**
     * Orthonormal camera basis from yaw/pitch angles in degrees.
     * Returns [fwd, rgt, up] as Vec3DDouble.
     */
    public static Vec3DDouble[] cameraBasis(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        return new Vec3DDouble[] { Vec3DDouble.from(-sy * cp, -sp, cy * cp), Vec3DDouble.from(cy, 0, sy),
            Vec3DDouble.from(-sy * sp, cp, cy * sp) };
    }
}
