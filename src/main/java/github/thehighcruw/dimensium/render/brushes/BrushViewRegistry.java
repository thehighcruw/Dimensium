package github.thehighcruw.dimensium.render.brushes;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.tool.Tool;
import github.thehighcruw.dimensium.tool.state.FreehandToolState;
import github.thehighcruw.dimensium.tool.state.PainterToolState;

@SideOnly(Side.CLIENT)
public final class BrushViewRegistry {

    private static final int[][] FACE_DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static final BrushView DEFAULT = (mc, wx, wy, wz) -> mc.theWorld.getBlock(wx, wy, wz) != Blocks.air;

    private static final Map<Tool, BrushView> VIEWS = new EnumMap<>(Tool.class);

    static {
        VIEWS.put(Tool.PAINTER, (mc, wx, wy, wz) -> {
            if (mc.theWorld.getBlock(wx, wy, wz) == Blocks.air) return false;
            if (PainterToolState.INSTANCE.painterMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
            return true;
        });

        VIEWS.put(Tool.FREEHAND_DRAW, (mc, wx, wy, wz) -> {
            if (!FreehandToolState.INSTANCE.freehandReplaceSolid && mc.theWorld.getBlock(wx, wy, wz) != Blocks.air)
                return false;
            if (FreehandToolState.INSTANCE.freehandMaskSurface) return hasAirNeighbor(mc, wx, wy, wz);
            return true;
        });

        VIEWS.put(Tool.STAMP, DEFAULT);
        VIEWS.put(Tool.SCULPT_DRAW, DEFAULT);
        VIEWS.put(Tool.NOISE, DEFAULT);
        VIEWS.put(Tool.ROCK, DEFAULT);
        VIEWS.put(Tool.GRADIENT, DEFAULT);
        VIEWS.put(Tool.SMOOTH, DEFAULT);
        VIEWS.put(Tool.WELD, DEFAULT);
        VIEWS.put(Tool.MELT, DEFAULT);
        VIEWS.put(Tool.ROUGHEN, DEFAULT);
        VIEWS.put(Tool.SHATTER, DEFAULT);
        VIEWS.put(Tool.DISTORT, DEFAULT);
        VIEWS.put(Tool.FREEHAND_SELECT, DEFAULT);
        VIEWS.put(Tool.RULER, new RulerBrushView());
        // SHAPE, FILL, ELEVATION, etc. — no standard brush preview
    }

    /** Returns the view for a tool, or null if the tool has no brush preview. */
    public static BrushView get(Tool tool) {
        return VIEWS.get(tool);
    }

    public static boolean hasBrushPreview(Tool tool) {
        return VIEWS.containsKey(tool);
    }

    private static boolean hasAirNeighbor(Minecraft mc, int wx, int wy, int wz) {
        for (int[] n : FACE_DIRS) {
            if (mc.theWorld.getBlock(wx + n[0], wy + n[1], wz + n[2]) == Blocks.air) return true;
        }
        return false;
    }

    private BrushViewRegistry() {}
}
