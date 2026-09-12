/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FreecamUtils {

    public static final double REACH = 512.0;

    public static Vec3 lookVec(EntityPlayer player) {
        FreecamEntity cam = FreecamState.INSTANCE.cameraEntity;
        if (cam != null) {
            double yaw = Math.toRadians(cam.rotationYaw);
            double pitch = Math.toRadians(cam.rotationPitch);
            return Vec3.createVectorHelper(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        }
        return player.getLookVec();
    }

    public static MovingObjectPosition rayTrace(Minecraft mc, double reach) {
        FreecamEntity cam = FreecamState.INSTANCE.cameraEntity;
        if (cam != null) {
            double yaw = Math.toRadians(cam.rotationYaw);
            double pitch = Math.toRadians(cam.rotationPitch);
            double dx = -Math.sin(yaw) * Math.cos(pitch);
            double dy = -Math.sin(pitch);
            double dz = Math.cos(yaw) * Math.cos(pitch);
            Vec3 start = Vec3.createVectorHelper(cam.posX, cam.posY, cam.posZ);
            Vec3 end = Vec3.createVectorHelper(cam.posX + dx * reach, cam.posY + dy * reach, cam.posZ + dz * reach);
            return mc.theWorld.rayTraceBlocks(start, end, false);
        }
        return mc.thePlayer.rayTrace(reach, 1.0f);
    }
}
