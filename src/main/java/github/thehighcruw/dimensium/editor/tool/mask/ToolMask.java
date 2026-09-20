/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import net.minecraft.world.World;

public class ToolMask implements MaskEntry {

    private String name;
    private MaskNode root;
    private MaskRole role = MaskRole.BOTH;

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

    public MaskRole getRole() {
        return role;
    }

    public void setRole(MaskRole role) {
        this.role = role;
    }

    public boolean test(World world, Vec3DInt coord) {
        return root == null || root.test(world, coord);
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
