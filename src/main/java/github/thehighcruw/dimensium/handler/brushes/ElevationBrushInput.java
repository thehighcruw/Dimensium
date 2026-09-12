package github.thehighcruw.dimensium.handler.brushes;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.render.MenuBar;
import github.thehighcruw.dimensium.render.OverlayRenderer;
import github.thehighcruw.dimensium.tool.BrushApplicator;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.ElevationToolState;

@SideOnly(Side.CLIENT)
public class ElevationBrushInput implements BrushInput {

    public static final ElevationBrushInput INSTANCE = new ElevationBrushInput();

    private long lastNano = 0;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastZ = Integer.MIN_VALUE;
    private boolean dragActive = false;
    private int lastFreehandX = Integer.MIN_VALUE;

    private ElevationBrushInput() {}

    @Override
    public boolean usesDragLoop() {
        return true;
    }

    @Override
    public boolean onDragTick(Minecraft mc, int sw, int sh) {
        FreecamState fs = FreecamState.INSTANCE;
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (fs.rmbDragging || altDown) return true;

        if (!Mouse.isButtonDown(KeyConstants.RMB)) {
            if (dragActive) {
                List<int[]> ops = ChangeProposal.flush();
                if (!ops.isEmpty()) BlockSender.sendChunked(
                    ops,
                    I18n.format(
                        "dimensium.action.elevation",
                        I18n.format(ElevationToolState.INSTANCE.elevationMode.label)));
                dragActive = false;
                lastNano = 0;
                lastX = Integer.MIN_VALUE;
                BrushApplicator.clearElevAccum();
            }
            lastFreehandX = Integer.MIN_VALUE;
            return true;
        }

        int sf = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        int mx = (int) fs.cursorX3d, my = (int) fs.cursorY;
        if (mx * sf < OverlayRenderer.toolPanel.currentW || my * sf < (int) MenuBar.INSTANCE.height()) return true;

        MovingObjectPosition mop = GuiDimensiumOverlay.raycastFromMouse(mx, my, sw, sh);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return true;

        ElevationToolState es = ElevationToolState.INSTANCE;
        if (!dragActive) {
            ChangeProposal.startDrag(ToolMaskRegistry.INSTANCE.getActiveMask());
            BrushApplicator.clearElevAccum();
            dragActive = true;
        }
        boolean isOnce = es.elevationApply == ElevationToolState.ElevationApply.ONCE;
        if (isOnce) {
            if (mop.blockX == lastX && mop.blockY == lastY && mop.blockZ == lastZ) return true;
            lastX = mop.blockX;
            lastY = mop.blockY;
            lastZ = mop.blockZ;
        } else {
            long intervalNano = (long) (1e9 / Math.max(0.1, es.elevationRate));
            long now = System.nanoTime();
            if (now - lastNano < intervalNano) return true;
            lastNano = now;
        }
        BrushApplicator.applyTool(mc.theWorld, mop.blockX, mop.blockY, mop.blockZ);
        lastFreehandX = mop.blockX;
        return true;
    }

    public boolean isPaintDragging() {
        return lastFreehandX != Integer.MIN_VALUE;
    }
}
