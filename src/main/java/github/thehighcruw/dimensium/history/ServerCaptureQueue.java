package github.thehighcruw.dimensium.history;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.thehighcruw.dimensium.network.PacketCaptureResponse;
import github.thehighcruw.dimensium.util.PerfTrace;

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
        final int minX, minY, minZ;
        final int width, height, depth;
        final List<int[]> results = new ArrayList<>();
        int cursor = 0;

        CaptureJob(EntityPlayerMP player, int txId, World world, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
            this.player = player;
            this.txId = txId;
            this.world = world;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.width = maxX - minX + 1;
            this.height = maxY - minY + 1;
            this.depth = maxZ - minZ + 1;
        }

        int total() {
            return width * height * depth;
        }

        int[] posAt(int idx) {
            int lz = idx % depth;
            int ly = (idx / depth) % height;
            int lx = idx / (depth * height);
            return new int[] { minX + lx, minY + ly, minZ + lz };
        }
    }

    public static void enqueue(EntityPlayerMP player, int txId, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ) {
        queue.add(new CaptureJob(player, txId, player.worldObj, minX, minY, minZ, maxX, maxY, maxZ));
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
            int[] pos = job.posAt(job.cursor++);
            Block blk = job.world.getBlock(pos[0], pos[1], pos[2]);
            if (blk != null && blk != Blocks.air) {
                int meta = job.world.getBlockMetadata(pos[0], pos[1], pos[2]);
                job.results.add(new int[] { pos[0], pos[1], pos[2], Block.getIdFromBlock(blk), meta });
            }
            read++;
        }
        PerfTrace.pop();

        if (job.cursor >= job.total()) {
            queue.poll();
            PerfTrace.push("sendCaptureResponse results=" + job.results.size());
            PacketCaptureResponse.sendChunked(
                job.player,
                job.txId,
                job.minX,
                job.minY,
                job.minZ,
                job.width,
                job.height,
                job.depth,
                job.results);
            PerfTrace.pop();
        }
        PerfTrace.end(10);
    }
}
