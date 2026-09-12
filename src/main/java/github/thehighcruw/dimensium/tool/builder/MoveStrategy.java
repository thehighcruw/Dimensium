package github.thehighcruw.dimensium.tool.builder;

import java.util.ArrayList;
import java.util.List;

import github.thehighcruw.dimensium.handler.BlockSender;
import github.thehighcruw.dimensium.handler.SelectionOps;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

public class MoveStrategy implements BuilderToolStrategy {

    @Override
    public boolean needsCapture() {
        return true;
    }

    @Override
    public void confirm(BuilderToolState bts, SelectionState sel) {
        int ox = sel.minX(), oy = sel.minY(), oz = sel.minZ();
        int dx = bts.offsetX, dy = bts.offsetY, dz = bts.offsetZ;

        // Snapshot air ops now — clearSelection() runs right after confirm() returns.
        final List<int[]> airOps = SelectionOps.selectionToAirOps(sel);
        final SelectionState selSnap = sel;
        BlockSender.sendChunkedLazy(() -> {
            List<int[]> ops = new ArrayList<>(
                airOps.size() + (selSnap.clipboard == null ? 0 : selSnap.clipboard.size()));
            ops.addAll(airOps);
            ops.addAll(SelectionOps.clipboardToPlacements(selSnap, ox + dx, oy + dy, oz + dz));
            return ops;
        }, "Move");
    }
}
