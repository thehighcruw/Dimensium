package github.thehighcruw.dimensium.tool.mask;

import java.util.ArrayList;
import java.util.List;

public abstract class LogicNode extends MaskNode {

    public final List<MaskNode> children = new ArrayList<>();

    @Override
    public List<MaskNode> children() {
        return children;
    }

    @Override
    public boolean isLogic() {
        return true;
    }
}
