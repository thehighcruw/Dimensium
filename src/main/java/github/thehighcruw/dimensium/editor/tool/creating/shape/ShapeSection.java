/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.shape;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.editor.tool.ToolSection;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapeToolState.ShapeType;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.resources.I18n;

@SideOnly(Side.CLIENT)
public class ShapeSection implements ToolSection {

    private static final ShapeType[] SHAPES_3D = {
        ShapeType.CUBOID,
        ShapeType.SPHERE,
        ShapeType.CYLINDER,
        ShapeType.PYRAMID,
        ShapeType.CONE,
        ShapeType.TORUS,
        ShapeType.OCTAHEDRON,
        ShapeType.SUPERSPHERE,
        ShapeType.TUBE,
        ShapeType.DODECAHEDRON,
        ShapeType.ICOSAHEDRON
    };
    private static final ShapeType[] SHAPES_2D = {
        ShapeType.DISK, ShapeType.PLANE, ShapeType.SUPERELLIPSE, ShapeType.REGULAR_POLYGON, ShapeType.ARCHIMEDEAN_SPIRAL
    };

    private final ShapeToolState state;
    private final ImInt categoryIdx = new ImInt();
    private final ImInt shapeIdx = new ImInt();
    private final int[] width = new int[1];
    private final int[] height = new int[1];
    private final int[] depth = new int[1];
    private final float[] exponent = new float[1];
    private final int[] polygonSides = new int[1];
    private final float[] spiralSpacing = new float[1];
    private final float[] spiralTurns = new float[1];
    private final int[] torusRing = new int[1];
    private final int[] torusRingZ = new int[1];
    private final int[] torusTube = new int[1];
    private final int[] wallThickness = new int[1];

    public ShapeSection(ShapeToolState state) {
        this.state = state;
    }

    @Override
    public void render() {
        ImGui.text(I18n.format("dimensium.ui.section.shape"));
        ImGui.separator();

        boolean is3D = true;
        for (ShapeType s : SHAPES_2D) {
            if (state.shapeType == s) {
                is3D = false;
                break;
            }
        }
        categoryIdx.set(is3D ? 0 : 1);
        String[] cats = {I18n.format("dimensium.ui.shape.cat.3d"), I18n.format("dimensium.ui.shape.cat.2d")};
        if (ImGui.combo(I18n.format("dimensium.ui.shape.category") + "##shape_cat", categoryIdx, cats)) {
            if (categoryIdx.get() == 0) state.shapeType = SHAPES_3D[0];
            else state.shapeType = SHAPES_2D[0];
            is3D = categoryIdx.get() == 0;
        }

        ShapeType[] currentShapes = is3D ? SHAPES_3D : SHAPES_2D;
        String[] shapeLabels = new String[currentShapes.length];
        for (int i = 0; i < currentShapes.length; i++) shapeLabels[i] = I18n.format(currentShapes[i].label);
        int curIdx = 0;
        for (int i = 0; i < currentShapes.length; i++) if (currentShapes[i] == state.shapeType) curIdx = i;
        shapeIdx.set(curIdx);
        if (ImGui.combo(I18n.format("dimensium.ui.shape.type") + "##shape_type", shapeIdx, shapeLabels)) {
            state.shapeType = currentShapes[shapeIdx.get()];
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.dimensions"));
        ImGui.separator();

        if (state.shapeType != ShapeType.TORUS && state.shapeType != ShapeType.TUBE) {
            width[0] = state.shapeWidth;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.width") + "##shape_w",
                    width,
                    ShapeToolState.DIM_MIN,
                    ShapeToolState.DIM_MAX)) {
                state.shapeWidth = width[0];
            }
            height[0] = state.shapeHeight;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.height") + "##shape_h",
                    height,
                    ShapeToolState.DIM_MIN,
                    ShapeToolState.DIM_MAX)) {
                state.shapeHeight = height[0];
            }
            depth[0] = state.shapeDepth;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.depth") + "##shape_d",
                    depth,
                    ShapeToolState.DIM_MIN,
                    ShapeToolState.DIM_MAX)) {
                state.shapeDepth = depth[0];
            }
        }

        if (state.shapeType == ShapeType.TORUS) {
            torusRing[0] = state.torusRingRadius;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.torus_ring") + "##shape_tr",
                    torusRing,
                    ShapeToolState.TORUS_RING_MIN,
                    ShapeToolState.TORUS_RING_MAX)) {
                state.torusRingRadius = torusRing[0];
            }
            torusTube[0] = state.torusTubeRadius;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.torus_tube") + "##shape_tt",
                    torusTube,
                    ShapeToolState.TORUS_TUBE_MIN,
                    ShapeToolState.TORUS_TUBE_MAX)) {
                state.torusTubeRadius = torusTube[0];
            }
        }

        if (state.shapeType == ShapeType.TUBE) {
            width[0] = state.shapeWidth;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.width") + "##shape_tube_w",
                    width,
                    ShapeToolState.DIM_MIN,
                    ShapeToolState.DIM_MAX)) {
                state.shapeWidth = width[0];
            }
            height[0] = state.shapeHeight;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.height") + "##shape_tube_h",
                    height,
                    ShapeToolState.DIM_MIN,
                    ShapeToolState.DIM_MAX)) {
                state.shapeHeight = height[0];
            }
            wallThickness[0] = state.tubeWallThickness;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.wall_thickness") + "##shape_wall",
                    wallThickness,
                    ShapeToolState.WALL_MIN,
                    ShapeToolState.WALL_MAX)) {
                state.tubeWallThickness = wallThickness[0];
            }
        }

        if (state.shapeType == ShapeType.ARCHIMEDEAN_SPIRAL) {
            spiralSpacing[0] = state.shapeSpiralSpacing;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.shape.spiral_spacing") + "##shape_ssp",
                    spiralSpacing,
                    ShapeToolState.SPIRAL_SPACING_MIN,
                    ShapeToolState.SPIRAL_SPACING_MAX)) {
                state.shapeSpiralSpacing = spiralSpacing[0];
            }
            spiralTurns[0] = state.shapeSpiralTurns;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.shape.spiral_turns") + "##shape_str",
                    spiralTurns,
                    ShapeToolState.SPIRAL_TURNS_MIN,
                    ShapeToolState.SPIRAL_TURNS_MAX)) {
                state.shapeSpiralTurns = spiralTurns[0];
            }
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.options"));
        ImGui.separator();

        if (state.shapeType == ShapeType.CUBOID
                || state.shapeType == ShapeType.SPHERE
                || state.shapeType == ShapeType.CYLINDER
                || state.shapeType == ShapeType.CONE) {
            ImBoolean cbHollow = new ImBoolean(state.shapeHollow);
            if (ImGui.checkbox(I18n.format("dimensium.ui.shape.hollow") + "##shape_hollow", cbHollow)) {
                state.shapeHollow = cbHollow.get();
            }
        }

        if (state.shapeType == ShapeType.SUPERSPHERE || state.shapeType == ShapeType.SUPERELLIPSE) {
            exponent[0] = state.shapeExponent;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.shape.exponent") + "##shape_exp",
                    exponent,
                    ShapeToolState.EXPONENT_MIN,
                    ShapeToolState.EXPONENT_MAX)) {
                state.shapeExponent = exponent[0];
            }
        }

        if (state.shapeType == ShapeType.SPHERE
                || state.shapeType == ShapeType.SUPERSPHERE
                || state.shapeType == ShapeType.SUPERELLIPSE) {
            ImBoolean cbSepAxes = new ImBoolean(state.shapeSeparateAxes);
            if (ImGui.checkbox(I18n.format("dimensium.ui.shape.separate_axes") + "##shape_sep", cbSepAxes)) {
                state.shapeSeparateAxes = cbSepAxes.get();
            }
        }

        if (state.shapeType == ShapeType.TORUS) {
            ImBoolean cbTorusSep = new ImBoolean(state.torusSeparateAxes);
            if (ImGui.checkbox(I18n.format("dimensium.ui.shape.torus_sep_axes") + "##shape_tsep", cbTorusSep)) {
                state.torusSeparateAxes = cbTorusSep.get();
            }
            if (state.torusSeparateAxes) {
                torusRingZ[0] = state.torusRingRadiusZ;
                if (ImGui.sliderInt(
                        I18n.format("dimensium.ui.shape.torus_ring_z") + "##shape_trz",
                        torusRingZ,
                        ShapeToolState.TORUS_RING_MIN,
                        ShapeToolState.TORUS_RING_MAX)) {
                    state.torusRingRadiusZ = torusRingZ[0];
                }
            }
        }

        if (state.shapeType == ShapeType.REGULAR_POLYGON) {
            polygonSides[0] = state.shapePolygonSides;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.polygon_sides") + "##shape_poly",
                    polygonSides,
                    ShapeToolState.POLYGON_SIDES_MIN,
                    ShapeToolState.POLYGON_SIDES_MAX)) {
                state.shapePolygonSides = polygonSides[0];
            }
        }

        ImBoolean cbKeepExisting = new ImBoolean(state.shapeKeepExisting);
        if (ImGui.checkbox(I18n.format("dimensium.ui.shape.keep_existing") + "##shape_keep", cbKeepExisting)) {
            state.shapeKeepExisting = cbKeepExisting.get();
        }
    }
}
