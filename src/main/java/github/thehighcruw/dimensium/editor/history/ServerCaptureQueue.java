/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.history;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.thehighcruw.dimensium.network.PacketCaptureResponse;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.PerfTrace;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Reads a bounding-box region of the world across multiple server ticks and
 * sends the result back to the requesting client as a PacketCaptureResponse.
 * Prevents a single-tick freeze when capturing large selections.
 */
public class ServerCaptureQueue {

    public static final ServerCaptureQueue INSTANCE = new ServerCaptureQueue();

    private static final int READS_PER_TICK = 1000;

    private static final Queue<CaptureJob> queue = new LinkedList<>();

    private static final class CaptureJob {

        final EntityPlayerMP player;
        final int txId;
        final World world;
        final Vec3DInt origin;
        final Vec3DInt dims;
        final List<int[]> results = new ArrayList<>();
        int cursor = 0;

        CaptureJob(EntityPlayerMP player, int txId, World world, Vec3DInt min, Vec3DInt max) {
            this.player = player;
            this.txId = txId;
            this.world = world;
            this.origin = min;
            this.dims = max.minus(min).plus(1);
        }

        int total() {
            return dims.product();
        }

        Vec3DInt posAt(int idx) {
            int lz = idx % dims.z();
            int ly = (idx / dims.z()) % dims.y();
            int lx = idx / (dims.z() * dims.y());
            return origin.plus(lx, ly, lz);
        }
    }

    public static void enqueue(
            EntityPlayerMP player, int txId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        queue.add(new CaptureJob(
                player, txId, player.worldObj, Vec3DInt.from(minX, minY, minZ), Vec3DInt.from(maxX, maxY, maxZ)));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CaptureJob job = queue.peek();
        if (job == null) return;

        PerfTrace.begin("[SERVER] captureRead cursor=" + job.cursor + "/" + job.total());
        int read = 0;
        PerfTrace.push("worldReads");
        while (job.cursor < job.total() && read < READS_PER_TICK) {
            Vec3DInt pos = job.posAt(job.cursor++);
            Block blk = job.world.getBlock(pos.x(), pos.y(), pos.z());
            if (blk != null && blk != Blocks.air) {
                int meta = job.world.getBlockMetadata(pos.x(), pos.y(), pos.z());
                job.results.add(new int[] {pos.x(), pos.y(), pos.z(), Block.getIdFromBlock(blk), meta});
            }
            read++;
        }
        PerfTrace.pop();

        if (job.cursor >= job.total()) {
            queue.poll();
            PerfTrace.push("sendCaptureResponse results=" + job.results.size());
            PacketCaptureResponse.sendChunked(job.player, job.txId, job.origin, job.dims, job.results);
            PerfTrace.pop();
        }
        PerfTrace.end(10);
    }
}
