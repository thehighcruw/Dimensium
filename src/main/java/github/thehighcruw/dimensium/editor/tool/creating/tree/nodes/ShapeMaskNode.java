/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.nodes;

import github.thehighcruw.dimensium.editor.pipeline.BlockMap;
import github.thehighcruw.dimensium.editor.pipeline.NodeParams;
import github.thehighcruw.dimensium.editor.pipeline.NodeSchema;
import github.thehighcruw.dimensium.editor.pipeline.PipelineContext;
import github.thehighcruw.dimensium.editor.pipeline.PipelineNode;
import github.thehighcruw.dimensium.editor.pipeline.PortType;
import github.thehighcruw.dimensium.editor.pipeline.PortValues;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;

/**
 * Fills a mathematical solid at the pipeline origin. The shape type and its parameters are
 * configured through the node UI — only the relevant parameters for the selected shape are shown.
 */
public class ShapeMaskNode implements PipelineNode {

    public static final String ID = "shape_mask";

    public static final int SHAPE_SPHERE = 0;
    public static final int SHAPE_ELLIPSOID = 1;
    public static final int SHAPE_BOX = 2;
    public static final int SHAPE_CYLINDER = 3;
    public static final int SHAPE_CONE = 4;
    public static final int SHAPE_TORUS = 5;
    public static final int SHAPE_PYRAMID = 6;

    public static final int AXIS_Y = 0;
    public static final int AXIS_X = 1;
    public static final int AXIS_Z = 2;

    public static final int TIP_UP = 0;
    public static final int TIP_DOWN = 1;

    private static final List<int[]> DEFAULT_PALETTE = Arrays.asList(new int[] {1, 0});

    private static final String TYPE_KEY = "shape.type";

    private static final NodeSchema SCHEMA = new NodeSchema()
            .enumParam(
                    TYPE_KEY,
                    SHAPE_SPHERE,
                    "dimensium.ui.pipeline.shape_type",
                    "Sphere",
                    "Ellipsoid",
                    "Box",
                    "Cylinder",
                    "Cone",
                    "Torus",
                    "Pyramid")
            // Sphere
            .visibleWhen(TYPE_KEY, SHAPE_SPHERE)
            .floatParam("sphere.radius", 8.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_sphere_radius")
            // Ellipsoid
            .visibleWhen(TYPE_KEY, SHAPE_ELLIPSOID)
            .floatParam("ellipsoid.radiusX", 8.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_ellipsoid_radius_x")
            .visibleWhen(TYPE_KEY, SHAPE_ELLIPSOID)
            .floatParam("ellipsoid.radiusY", 6.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_ellipsoid_radius_y")
            .visibleWhen(TYPE_KEY, SHAPE_ELLIPSOID)
            .floatParam("ellipsoid.radiusZ", 8.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_ellipsoid_radius_z")
            // Box
            .visibleWhen(TYPE_KEY, SHAPE_BOX)
            .intParam("box.width", 10, 1, 80, "dimensium.ui.pipeline.shape_box_width")
            .visibleWhen(TYPE_KEY, SHAPE_BOX)
            .intParam("box.height", 10, 1, 80, "dimensium.ui.pipeline.shape_box_height")
            .visibleWhen(TYPE_KEY, SHAPE_BOX)
            .intParam("box.depth", 10, 1, 80, "dimensium.ui.pipeline.shape_box_depth")
            // Cylinder
            .visibleWhen(TYPE_KEY, SHAPE_CYLINDER)
            .floatParam("cyl.radius", 4.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_cyl_radius")
            .visibleWhen(TYPE_KEY, SHAPE_CYLINDER)
            .intParam("cyl.height", 8, 1, 80, "dimensium.ui.pipeline.shape_cyl_height")
            .visibleWhen(TYPE_KEY, SHAPE_CYLINDER)
            .enumParam("cyl.axis", AXIS_Y, "dimensium.ui.pipeline.shape_cyl_axis", "Y (vertical)", "X", "Z")
            // Cone
            .visibleWhen(TYPE_KEY, SHAPE_CONE)
            .floatParam("cone.baseRadius", 5.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_cone_base_radius")
            .visibleWhen(TYPE_KEY, SHAPE_CONE)
            .intParam("cone.height", 8, 1, 80, "dimensium.ui.pipeline.shape_cone_height")
            .visibleWhen(TYPE_KEY, SHAPE_CONE)
            .enumParam("cone.tipDir", TIP_UP, "dimensium.ui.pipeline.shape_cone_tip_dir", "Tip Up", "Tip Down")
            // Torus
            .visibleWhen(TYPE_KEY, SHAPE_TORUS)
            .floatParam("torus.majorRadius", 8.0f, 1.0f, 40.0f, "dimensium.ui.pipeline.shape_torus_major_radius")
            .visibleWhen(TYPE_KEY, SHAPE_TORUS)
            .floatParam("torus.minorRadius", 2.5f, 0.5f, 20.0f, "dimensium.ui.pipeline.shape_torus_minor_radius")
            // Pyramid
            .visibleWhen(TYPE_KEY, SHAPE_PYRAMID)
            .intParam("pyr.baseWidth", 10, 2, 80, "dimensium.ui.pipeline.shape_pyr_base_width")
            .visibleWhen(TYPE_KEY, SHAPE_PYRAMID)
            .intParam("pyr.baseDepth", 10, 2, 80, "dimensium.ui.pipeline.shape_pyr_base_depth")
            .visibleWhen(TYPE_KEY, SHAPE_PYRAMID)
            .intParam("pyr.height", 8, 1, 80, "dimensium.ui.pipeline.shape_pyr_height")
            .visibleWhen(TYPE_KEY, SHAPE_PYRAMID)
            .enumParam("pyr.tipDir", TIP_UP, "dimensium.ui.pipeline.shape_pyr_tip_dir", "Tip Up", "Tip Down")
            // Palette (always visible)
            .paletteParam("shape.palette", DEFAULT_PALETTE, "dimensium.ui.pipeline.shape_palette")
            .description("dimensium.ui.pipeline.node.shape_mask.desc")
            .optionalInputPort("origin", PortType.VEC3)
            .outputPort("blocks", PortType.BLOCK_MAP);

    @Override
    public void apply(PortValues inputs, PortValues outputs, NodeParams params, PipelineContext context) {
        Vec3DInt center = inputs.get("origin", Vec3DInt.class);
        if (center == null) center = context.origin;

        int shapeType = params.getInt(TYPE_KEY, SHAPE_SPHERE);
        List<int[]> palette = params.getPalette("shape.palette", DEFAULT_PALETTE);
        Random rand = new Random(context.nodeSeed(0));

        BlockMap map = new BlockMap();
        switch (shapeType) {
            case SHAPE_SPHERE:
                fillSphere(map, center, params, rand, palette);
                break;
            case SHAPE_ELLIPSOID:
                fillEllipsoid(map, center, params, rand, palette);
                break;
            case SHAPE_BOX:
                fillBox(map, center, params, rand, palette);
                break;
            case SHAPE_CYLINDER:
                fillCylinder(map, center, params, rand, palette);
                break;
            case SHAPE_CONE:
                fillCone(map, center, params, rand, palette);
                break;
            case SHAPE_TORUS:
                fillTorus(map, center, params, rand, palette);
                break;
            case SHAPE_PYRAMID:
                fillPyramid(map, center, params, rand, palette);
                break;
            default:
                break;
        }
        outputs.set("blocks", map);
    }

    private void fillSphere(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        float radius = params.getFloat("sphere.radius", 8.0f);
        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;
        Vec3DInt.forEachInclusive(center.minus(r), center.plus(r), pos -> {
            float dx = pos.x() - center.x();
            float dy = pos.y() - center.y();
            float dz = pos.z() - center.z();
            if (dx * dx + dy * dy + dz * dz <= rSq) put(map, pos, rand, palette);
        });
    }

    private void fillEllipsoid(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        float rx = Math.max(0.01f, params.getFloat("ellipsoid.radiusX", 8.0f));
        float ry = Math.max(0.01f, params.getFloat("ellipsoid.radiusY", 6.0f));
        float rz = Math.max(0.01f, params.getFloat("ellipsoid.radiusZ", 8.0f));
        int maxR = (int) Math.ceil(Math.max(rx, Math.max(ry, rz)));
        Vec3DInt.forEachInclusive(center.minus(maxR), center.plus(maxR), pos -> {
            float nx = (pos.x() - center.x()) / rx;
            float ny = (pos.y() - center.y()) / ry;
            float nz = (pos.z() - center.z()) / rz;
            if (nx * nx + ny * ny + nz * nz <= 1.0f) put(map, pos, rand, palette);
        });
    }

    private void fillBox(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        int halfW = params.getInt("box.width", 10) / 2;
        int halfH = params.getInt("box.height", 10) / 2;
        int halfD = params.getInt("box.depth", 10) / 2;
        Vec3DInt min = center.minus(new Vec3DInt(halfW, halfH, halfD));
        Vec3DInt max = center.plus(new Vec3DInt(halfW, halfH, halfD));
        Vec3DInt.forEachInclusive(min, max, pos -> put(map, pos, rand, palette));
    }

    private void fillCylinder(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        float radius = params.getFloat("cyl.radius", 4.0f);
        int height = params.getInt("cyl.height", 8);
        int axis = params.getInt("cyl.axis", AXIS_Y);
        int r = (int) Math.ceil(radius);
        int halfHeight = height / 2;
        float rSq = radius * radius;

        int minX = center.x() - (axis == AXIS_X ? halfHeight : r);
        int minY = center.y() - (axis == AXIS_Y ? halfHeight : r);
        int minZ = center.z() - (axis == AXIS_Z ? halfHeight : r);
        int maxX = center.x() + (axis == AXIS_X ? halfHeight : r);
        int maxY = center.y() + (axis == AXIS_Y ? halfHeight : r);
        int maxZ = center.z() + (axis == AXIS_Z ? halfHeight : r);

        Vec3DInt.forEachInclusive(Vec3DInt.from(minX, minY, minZ), Vec3DInt.from(maxX, maxY, maxZ), pos -> {
            int dx = pos.x() - center.x();
            int dy = pos.y() - center.y();
            int dz = pos.z() - center.z();
            float radialDistSq;
            int axialDist;
            if (axis == AXIS_Y) {
                radialDistSq = dx * dx + dz * dz;
                axialDist = Math.abs(dy);
            } else if (axis == AXIS_X) {
                radialDistSq = dy * dy + dz * dz;
                axialDist = Math.abs(dx);
            } else {
                radialDistSq = dx * dx + dy * dy;
                axialDist = Math.abs(dz);
            }
            if (radialDistSq <= rSq && axialDist <= halfHeight) put(map, pos, rand, palette);
        });
    }

    private void fillCone(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        float baseRadius = params.getFloat("cone.baseRadius", 5.0f);
        int height = params.getInt("cone.height", 8);
        int tipDir = params.getInt("cone.tipDir", TIP_UP);
        int r = (int) Math.ceil(baseRadius);
        Vec3DInt min = center.minus(new Vec3DInt(r, tipDir == TIP_UP ? 0 : height, r));
        Vec3DInt max = center.plus(new Vec3DInt(r, tipDir == TIP_UP ? height : 0, r));
        Vec3DInt.forEachInclusive(min, max, pos -> {
            int heightOffset = tipDir == TIP_UP ? pos.y() - center.y() : center.y() - pos.y();
            if (heightOffset < 0 || heightOffset > height) return;
            float radiusAtSlice = baseRadius * (1.0f - (float) heightOffset / height);
            int dx = pos.x() - center.x();
            int dz = pos.z() - center.z();
            if (dx * dx + dz * dz <= radiusAtSlice * radiusAtSlice) put(map, pos, rand, palette);
        });
    }

    private void fillTorus(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        float majorRadius = params.getFloat("torus.majorRadius", 8.0f);
        float minorRadius = params.getFloat("torus.minorRadius", 2.5f);
        int bound = (int) Math.ceil(majorRadius + minorRadius);
        int minorBound = (int) Math.ceil(minorRadius);
        float minorRSq = minorRadius * minorRadius;
        Vec3DInt.forEachInclusive(
                center.minus(new Vec3DInt(bound, minorBound, bound)),
                center.plus(new Vec3DInt(bound, minorBound, bound)),
                pos -> {
                    float dx = pos.x() - center.x();
                    float dy = pos.y() - center.y();
                    float dz = pos.z() - center.z();
                    float ringDist = (float) Math.sqrt(dx * dx + dz * dz) - majorRadius;
                    if (ringDist * ringDist + dy * dy <= minorRSq) put(map, pos, rand, palette);
                });
    }

    private void fillPyramid(BlockMap map, Vec3DInt center, NodeParams params, Random rand, List<int[]> palette) {
        int baseWidth = params.getInt("pyr.baseWidth", 10);
        int baseDepth = params.getInt("pyr.baseDepth", 10);
        int height = params.getInt("pyr.height", 8);
        int tipDir = params.getInt("pyr.tipDir", TIP_UP);
        int halfW = baseWidth / 2;
        int halfD = baseDepth / 2;
        Vec3DInt min = center.minus(new Vec3DInt(halfW, tipDir == TIP_UP ? 0 : height, halfD));
        Vec3DInt max = center.plus(new Vec3DInt(halfW, tipDir == TIP_UP ? height : 0, halfD));
        Vec3DInt.forEachInclusive(min, max, pos -> {
            int heightOffset = tipDir == TIP_UP ? pos.y() - center.y() : center.y() - pos.y();
            if (heightOffset < 0 || heightOffset > height) return;
            float fraction = (float) heightOffset / height;
            float halfWAtSlice = halfW * (1.0f - fraction);
            float halfDAtSlice = halfD * (1.0f - fraction);
            int dx = pos.x() - center.x();
            int dz = pos.z() - center.z();
            if (Math.abs(dx) <= halfWAtSlice && Math.abs(dz) <= halfDAtSlice) put(map, pos, rand, palette);
        });
    }

    private static void put(BlockMap map, Vec3DInt pos, Random rand, List<int[]> palette) {
        int[] entry = palette.get(rand.nextInt(palette.size()));
        map.put(pos, Block.getBlockById(entry[0]), entry[1]);
    }

    @Override
    public NodeSchema schema() {
        return SCHEMA;
    }
}
