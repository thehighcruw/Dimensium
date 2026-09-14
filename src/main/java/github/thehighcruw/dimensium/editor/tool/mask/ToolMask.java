/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import net.minecraft.world.World;

public class ToolMask implements MaskEntry {

    private String name;
    private MaskNode root;

    public ToolMask(String name) {
        this.name = name;
        this.root = new AndNode();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public MaskNode getRoot() {
        return root;
    }

    public void setRoot(MaskNode root) {
        this.root = root;
    }

    public boolean test(World world, int x, int y, int z) {
        return root == null || root.test(world, x, y, z);
    }

    public String toMaskString() {
        return root == null ? "(empty)" : nodeToString(root);
    }

    private String nodeToString(MaskNode node) {
        if (!node.isLogic()) return node.displayName();
        StringBuilder sb = new StringBuilder(node.displayName()).append("{");
        for (int i = 0; i < node.children().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nodeToString(node.children().get(i)));
        }
        return sb.append("}").toString();
    }
}
