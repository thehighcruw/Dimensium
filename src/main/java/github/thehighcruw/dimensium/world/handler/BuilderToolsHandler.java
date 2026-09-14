/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.handler;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.freecam.FreecamUtils;
import github.thehighcruw.dimensium.editor.handler.ExtrudeHelper;
import github.thehighcruw.dimensium.editor.tool.selecting.BooleanOp;
import github.thehighcruw.dimensium.network.PacketCaptureRequest;
import github.thehighcruw.dimensium.network.PacketHandler;
import github.thehighcruw.dimensium.shared.KeyConstants;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.world.tool.BuilderToolApplicator;
import github.thehighcruw.dimensium.world.tool.strategy.SmearStrategy;

@SideOnly(Side.CLIENT)
public class BuilderToolsHandler {

    private static final AtomicInteger CAPTURE_ID_GEN = new AtomicInteger(1);

    void handle(MouseEvent event, EntityPlayer player, Minecraft mc) {
        // Ignore all input while waiting for server capture response.
        if (BuilderToolState.INSTANCE.phase == Phase.CAPTURING) return;
        BuilderToolState bts = BuilderToolState.INSTANCE;

        if (bts.activeTool == BuilderTool.EXTRUDE) {
            applyExtrude(mc.theWorld);
            return;
        }

        MovingObjectPosition mop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
        boolean hitBlock = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

        if (event.button == KeyConstants.MMB && bts.phase == Phase.MANIPULATING) {
            WorldMouseHandler.updateAxisLock(bts);
            bts.nudgeOffset(1, FreecamUtils.lookVec(player));
            return;
        }

        // Hold RMB to anchor pos1; drag updates live preview; release commits pos2.
        if (event.button == KeyConstants.RMB && bts.phase == Phase.IDLE && hitBlock) {
            SelectionState sel = SelectionState.INSTANCE;
            sel.pendingPos1 = true;
            sel.pendingPos = Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ);
            sel.clearBlocks();
            bts.phase = Phase.SELECTING;
        }
    }

    void handleRelease(MouseEvent event, Minecraft mc) {
        if (event.button != KeyConstants.RMB) return;
        BuilderToolState bts = BuilderToolState.INSTANCE;

        if (bts.phase == Phase.MANIPULATING) {
            PerfTrace.begin("builder MANIPULATING release");
            PerfTrace.push("confirmOperation");
            confirmOperation(bts);
            PerfTrace.pop();
            PerfTrace.push("resetPhase");
            bts.resetPhase();
            PerfTrace.pop();
            PerfTrace.push("clearSelection");
            SelectionState.INSTANCE.clearSelection();
            PerfTrace.pop();
            PerfTrace.end(0);
            return;
        }

        if (bts.phase == Phase.SELECTING) {
            MovingObjectPosition mop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            SelectionState sel = SelectionState.INSTANCE;
            PerfTrace.begin("builder SELECTING release");
            PerfTrace.push("applyOp selSize=" + sel.size());
            sel.applyOp(
                SelectionState.aabbBlocks(
                    sel.pendingPos.x(),
                    sel.pendingPos.y(),
                    sel.pendingPos.z(),
                    mop.blockX,
                    mop.blockY,
                    mop.blockZ),
                BooleanOp.REPLACE);
            PerfTrace.pop();
            sel.pendingPos1 = false;

            if (!BuilderToolApplicator.needsCapture(bts.activeTool)) {
                PerfTrace.push("confirm");
                BuilderToolApplicator.confirm(bts.activeTool, bts, sel);
                PerfTrace.pop();
                bts.resetPhase();
                SelectionState.INSTANCE.clearSelection();
                PerfTrace.end(0);
                return;
            }

            // Request world capture from server — avoids blocking the client thread.
            // Phase transitions to MANIPULATING when PacketCaptureResponse arrives.
            PerfTrace.push("sendCaptureRequest");
            int captureId = CAPTURE_ID_GEN.incrementAndGet();
            PacketHandler.CHANNEL.sendToServer(
                new PacketCaptureRequest(
                    captureId,
                    sel.minX(),
                    sel.minY(),
                    sel.minZ(),
                    sel.maxX(),
                    sel.maxY(),
                    sel.maxZ()));
            bts.phase = Phase.CAPTURING;
            PerfTrace.pop();
            PerfTrace.end(0);
        }
    }

    private void confirmOperation(BuilderToolState bts) {
        if (SelectionState.INSTANCE.clipboard == null) return;
        BuilderToolApplicator.confirm(bts.activeTool, bts, SelectionState.INSTANCE);
    }

    /** Builds the smear preview proposal for the block under the cursor. Called each render frame. */
    public static void buildSmearPreview(Minecraft mc, SelectionState sel, BuilderToolState bts) {
        SmearStrategy.buildPreview(mc, sel, bts);
    }

    private void applyExtrude(World world) {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = FreecamUtils.rayTrace(mc, FreecamUtils.REACH);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        ExtrudeHelper.applyExtrudeAt(world, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
    }

}
