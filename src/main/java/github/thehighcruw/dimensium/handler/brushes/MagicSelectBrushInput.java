package github.thehighcruw.dimensium.handler.brushes;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.freecam.FreecamState;
import github.thehighcruw.dimensium.handler.KeyConstants;
import github.thehighcruw.dimensium.render.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.tool.state.MagicSelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public class MagicSelectBrushInput implements BrushInput {

    @Override
    public boolean requiresBlockTarget() {
        return false;
    }

    @Override
    public boolean onMouseClick(int button, Minecraft mc, MovingObjectPosition ignored) {
        if (button != KeyConstants.RMB) return false;
        FreecamState fs = FreecamState.INSTANCE;
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        MovingObjectPosition mop = GuiDimensiumOverlay
            .raycastFromMouse((int) fs.cursorX, (int) fs.cursorY, sr.getScaledWidth(), sr.getScaledHeight());
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        SelectionState sel = SelectionState.INSTANCE;
        MagicSelectToolState ts = MagicSelectToolState.INSTANCE;
        Set<Long> flooded = SelectionState.floodFill(
            mc.theWorld,
            mop.blockX,
            mop.blockY,
            mop.blockZ,
            ts.magicSelectLimit,
            ts.magicSelectRange,
            ts.magicSelectSurface,
            ts.magicSelectCorners,
            ts.magicCompareType,
            ts.magicDirection);
        sel.applyOp(ToolMaskRegistry.INSTANCE.filterSelection(flooded), SelectToolState.INSTANCE.booleanOp);
        sel.pendingPos1 = false;
        return true;
    }
}
