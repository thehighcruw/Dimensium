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
    private final int[] metaballBlendRadius = new int[1];

    public ShapeSection(ShapeToolState state) {
        this.state = state;
    }

    private static boolean isUniformShape(ShapeType type) {
        return type == ShapeType.SPHERE
                || type == ShapeType.OCTAHEDRON
                || type == ShapeType.SUPERSPHERE
                || type == ShapeType.DODECAHEDRON
                || type == ShapeType.ICOSAHEDRON;
    }

    private static boolean isXzSymmetricShape(ShapeType type) {
        return type == ShapeType.CYLINDER || type == ShapeType.CONE || type == ShapeType.PYRAMID;
    }

    private static boolean is2dRadialShape(ShapeType type) {
        return type == ShapeType.DISK || type == ShapeType.SUPERELLIPSE || type == ShapeType.REGULAR_POLYGON;
    }

    @Override
    public void render() {
        ImGui.text(I18n.format("dimensium.ui.section.shape"));
        ImGui.separator();

        boolean is3D = true;
        for (ShapeType shape : SHAPES_2D) {
            if (state.shapeType == shape) {
                is3D = false;
                break;
            }
        }
        categoryIdx.set(is3D ? 0 : 1);
        String[] cats = {I18n.format("dimensium.ui.shape.cat.3d"), I18n.format("dimensium.ui.shape.cat.2d")};
        if (ImGui.combo(I18n.format("dimensium.ui.shape.category") + "##shape_cat", categoryIdx, cats)) {
            state.shapeType = categoryIdx.get() == 0 ? SHAPES_3D[0] : SHAPES_2D[0];
            state.shapeSeparateAxes = false;
            is3D = categoryIdx.get() == 0;
        }

        ShapeType[] currentShapes = is3D ? SHAPES_3D : SHAPES_2D;
        String[] shapeLabels = new String[currentShapes.length];
        for (int index = 0; index < currentShapes.length; index++) {
            shapeLabels[index] = I18n.format(currentShapes[index].label);
        }
        int currentIndex = 0;
        for (int index = 0; index < currentShapes.length; index++) {
            if (currentShapes[index] == state.shapeType) {
                currentIndex = index;
                break;
            }
        }
        shapeIdx.set(currentIndex);
        if (ImGui.combo(I18n.format("dimensium.ui.shape.type") + "##shape_type", shapeIdx, shapeLabels)) {
            ShapeType selected = currentShapes[shapeIdx.get()];
            if (selected != state.shapeType) {
                state.shapeType = selected;
                state.shapeSeparateAxes = false;
            }
        }

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.dimensions"));
        ImGui.separator();

        renderDimensionSliders();

        ImGui.spacing();
        ImGui.text(I18n.format("dimensium.ui.section.options"));
        ImGui.separator();

        renderOptions();
    }

    private void renderDimensionSliders() {
        ShapeType type = state.shapeType;

        if (type == ShapeType.TORUS) {
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
            return;
        }

        if (type == ShapeType.TUBE) {
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
            return;
        }

        if (type == ShapeType.ARCHIMEDEAN_SPIRAL) {
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
            return;
        }

        if (type == ShapeType.CUBOID) {
            if (!state.shapeSeparateAxes) {
                width[0] = state.shapeWidth;
                if (ImGui.sliderInt(
                        I18n.format("dimensium.ui.shape.size") + "##shape_w",
                        width,
                        ShapeToolState.DIM_MIN,
                        ShapeToolState.DIM_MAX)) {
                    state.shapeWidth = width[0];
                }
            } else {
                renderWidthSlider();
                renderHeightSlider();
                renderDepthSlider();
            }
            renderSeparateAxesToggle();
            return;
        }

        if (type == ShapeType.PLANE) {
            renderWidthSlider();
            renderDepthSlider();
            return;
        }

        if (isUniformShape(type)) {
            if (!state.shapeSeparateAxes) {
                width[0] = state.shapeWidth;
                if (ImGui.sliderInt(
                        I18n.format("dimensium.ui.shape.size") + "##shape_w",
                        width,
                        ShapeToolState.DIM_MIN,
                        ShapeToolState.DIM_MAX)) {
                    state.shapeWidth = width[0];
                }
            } else {
                renderWidthSlider();
                renderHeightSlider();
                renderDepthSlider();
            }
            renderSeparateAxesToggle();
            return;
        }

        if (isXzSymmetricShape(type)) {
            renderWidthSlider();
            renderHeightSlider();
            if (state.shapeSeparateAxes) {
                renderDepthSlider();
            }
            renderSeparateAxesToggle();
            return;
        }

        if (is2dRadialShape(type)) {
            if (!state.shapeSeparateAxes) {
                width[0] = state.shapeWidth;
                if (ImGui.sliderInt(
                        I18n.format("dimensium.ui.shape.size") + "##shape_w",
                        width,
                        ShapeToolState.DIM_MIN,
                        ShapeToolState.DIM_MAX)) {
                    state.shapeWidth = width[0];
                }
            } else {
                renderWidthSlider();
                renderDepthSlider();
            }
            renderSeparateAxesToggle();
        }
    }

    private void renderWidthSlider() {
        width[0] = state.shapeWidth;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.shape.width") + "##shape_w",
                width,
                ShapeToolState.DIM_MIN,
                ShapeToolState.DIM_MAX)) {
            state.shapeWidth = width[0];
        }
    }

    private void renderHeightSlider() {
        height[0] = state.shapeHeight;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.shape.height") + "##shape_h",
                height,
                ShapeToolState.DIM_MIN,
                ShapeToolState.DIM_MAX)) {
            state.shapeHeight = height[0];
        }
    }

    private void renderDepthSlider() {
        depth[0] = state.shapeDepth;
        if (ImGui.sliderInt(
                I18n.format("dimensium.ui.shape.depth") + "##shape_d",
                depth,
                ShapeToolState.DIM_MIN,
                ShapeToolState.DIM_MAX)) {
            state.shapeDepth = depth[0];
        }
    }

    private void renderSeparateAxesToggle() {
        ImBoolean cbSepAxes = new ImBoolean(state.shapeSeparateAxes);
        if (ImGui.checkbox(I18n.format("dimensium.ui.shape.separate_axes") + "##shape_sep", cbSepAxes)) {
            state.shapeSeparateAxes = cbSepAxes.get();
        }
    }

    private void renderOptions() {
        ShapeType type = state.shapeType;

        if (type == ShapeType.CUBOID
                || type == ShapeType.SPHERE
                || type == ShapeType.CYLINDER
                || type == ShapeType.CONE
                || type == ShapeType.DISK
                || type == ShapeType.SUPERELLIPSE) {
            ImBoolean cbHollow = new ImBoolean(state.shapeHollow);
            if (ImGui.checkbox(I18n.format("dimensium.ui.shape.hollow") + "##shape_hollow", cbHollow)) {
                state.shapeHollow = cbHollow.get();
            }
        }

        if (type == ShapeType.SUPERSPHERE || type == ShapeType.SUPERELLIPSE) {
            exponent[0] = state.shapeExponent;
            if (ImGui.sliderFloat(
                    I18n.format("dimensium.ui.shape.exponent") + "##shape_exp",
                    exponent,
                    ShapeToolState.EXPONENT_MIN,
                    ShapeToolState.EXPONENT_MAX)) {
                state.shapeExponent = exponent[0];
            }
        }

        if (type == ShapeType.REGULAR_POLYGON) {
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

        ImBoolean cbUseStairs = new ImBoolean(state.useStairsAndSlabs);
        if (ImGui.checkbox(I18n.format("dimensium.ui.shape.use_stairs_and_slabs") + "##shape_stairs", cbUseStairs)) {
            state.useStairsAndSlabs = cbUseStairs.get();
            ShapePlacementState.INSTANCE.invalidateGhost();
        }

        ImBoolean cbMetaballBlend = new ImBoolean(state.metaballBlend);
        if (ImGui.checkbox(I18n.format("dimensium.ui.shape.metaball_blend") + "##shape_metaball", cbMetaballBlend)) {
            state.metaballBlend = cbMetaballBlend.get();
        }
        if (state.metaballBlend) {
            metaballBlendRadius[0] = state.metaballBlendRadius;
            if (ImGui.sliderInt(
                    I18n.format("dimensium.ui.shape.metaball_blend_radius") + "##shape_metaball_r",
                    metaballBlendRadius,
                    ShapeToolState.METABALL_BLEND_RADIUS_MIN,
                    ShapeToolState.METABALL_BLEND_RADIUS_MAX)) {
                state.metaballBlendRadius = metaballBlendRadius[0];
            }
        }
    }
}
