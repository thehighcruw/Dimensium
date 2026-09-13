/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

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
