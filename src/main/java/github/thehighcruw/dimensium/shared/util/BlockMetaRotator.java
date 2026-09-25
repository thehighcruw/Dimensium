/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

import github.thehighcruw.dimensium.shared.math.Mat3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;

/** Rotates block metadata to match a rotation applied to the block's position. */
public class BlockMetaRotator {

    private BlockMetaRotator() {}

    /**
     * Returns the metadata value that best matches the given block's orientation after
     * applying the specified rotation. Angles are in degrees; each axis is rounded to
     * the nearest 90° multiple before the direction remap is computed.
     */
    public static int rotate(Block block, int meta, float pitchDeg, float yawDeg, float rollDeg) {
        float roundedPitch = Math.round(pitchDeg / 90f) * 90f;
        float roundedYaw = Math.round(yawDeg / 90f) * 90f;
        float roundedRoll = Math.round(rollDeg / 90f) * 90f;
        if (roundedPitch == 0f && roundedYaw == 0f && roundedRoll == 0f) return meta;
        return rotate(block, meta, Mat3DFloat.fromEulerDeg(roundedPitch, roundedYaw, roundedRoll));
    }

    /** Like {@link #rotate(Block, int, float, float, float)} but accepts a pre-built matrix (no angle rounding). */
    public static int rotate(Block block, int meta, Mat3DFloat rotation) {
        if (block instanceof BlockStairs) return rotateStairs(meta, rotation);
        if (block instanceof BlockFurnace
                || block instanceof BlockChest
                || block instanceof BlockEnderChest
                || block instanceof BlockLadder
                || block instanceof BlockSign) return rotateFurnaceFacing(meta, rotation);
        if (block instanceof BlockDispenser
                || block instanceof BlockDropper
                || block instanceof BlockPistonBase
                || block instanceof BlockPistonExtension) return rotateDispenserFacing(meta, rotation);
        if (block instanceof BlockHopper) return rotateHopperFacing(meta, rotation);
        if (block instanceof BlockPumpkin) return rotatePumpkinFacing(meta, rotation);
        if (block instanceof BlockDoor) return rotateDoorFacing(meta, rotation);
        if (block instanceof BlockTrapDoor) return rotateTrapDoorFacing(meta, rotation);
        if (block instanceof BlockFenceGate) return rotateFenceGateFacing(meta, rotation);
        if (block instanceof BlockBed) return rotateBedFacing(meta, rotation);
        if (block instanceof BlockLog) return rotateLogAxis(meta, rotation);
        if (block instanceof BlockQuartz) return rotateQuartzAxis(meta, rotation);
        if (block instanceof BlockTorch || block instanceof BlockRedstoneTorch)
            return rotateTorchFacing(meta, rotation);
        if (block instanceof BlockVine) return rotateVineFaces(meta, rotation);
        if (block instanceof BlockButton) return rotateButtonFacing(meta, rotation);
        return meta;
    }

    // ── Direction helpers ──────────────────────────────────────────────────────

    // face6 indices: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
    // These match the MC Facing offsets: DOWN(-Y), UP(+Y), NORTH(-Z), SOUTH(+Z), WEST(-X), EAST(+X)
    private static final Vec3DFloat[] FACE6_VECS = {
        Vec3DFloat.from(0, -1, 0),
        Vec3DFloat.from(0, 1, 0),
        Vec3DFloat.from(0, 0, -1),
        Vec3DFloat.from(0, 0, 1),
        Vec3DFloat.from(-1, 0, 0),
        Vec3DFloat.from(1, 0, 0)
    };

    // horiz4 indices: SOUTH=0, WEST=1, NORTH=2, EAST=3
    private static final Vec3DFloat[] HORIZ4_VECS = {
        Vec3DFloat.from(0, 0, 1), Vec3DFloat.from(-1, 0, 0), Vec3DFloat.from(0, 0, -1), Vec3DFloat.from(1, 0, 0)
    };

    private static int rotateFace6(int face, Mat3DFloat rotation) {
        return nearestFace6(rotation.mul(FACE6_VECS[face]));
    }

    private static int nearestFace6(Vec3DFloat vector) {
        float absX = Math.abs(vector.x()), absY = Math.abs(vector.y()), absZ = Math.abs(vector.z());
        if (absY >= absX && absY >= absZ) return vector.y() < 0 ? 0 : 1;
        if (absZ >= absX) return vector.z() < 0 ? 2 : 3;
        return vector.x() < 0 ? 4 : 5;
    }

    private static int nearestHorizFace6(Vec3DFloat vector) {
        float absX = Math.abs(vector.x()), absZ = Math.abs(vector.z());
        if (absZ >= absX) return vector.z() < 0 ? 2 : 3;
        return vector.x() < 0 ? 4 : 5;
    }

    private static int rotateHoriz4(int direction, Mat3DFloat rotation) {
        return nearestHoriz4(rotation.mul(HORIZ4_VECS[direction]));
    }

    private static int nearestHoriz4(Vec3DFloat vector) {
        float absX = Math.abs(vector.x()), absZ = Math.abs(vector.z());
        if (absZ >= absX) return vector.z() > 0 ? 0 : 2;
        return vector.x() < 0 ? 1 : 3;
    }

    // ── Block-specific rotators ────────────────────────────────────────────────

    // Stairs: meta bits 0-1 = facing (0=east, 1=west, 2=south, 3=north); bit 2 = upside-down
    private static int rotateStairs(int meta, Mat3DFloat rotation) {
        int[] stairsToFace6 = {5, 4, 3, 2};
        int[] face6ToStairs = {-1, -1, 3, 2, 1, 0};

        Vec3DFloat facingVec = FACE6_VECS[stairsToFace6[meta & 3]];
        float upSign = (meta & 4) != 0 ? -1f : 1f;

        Vec3DFloat rotFacing = rotation.mul(facingVec);
        Vec3DFloat rotUp = rotation.mul(Vec3DFloat.from(0, upSign, 0));

        int upsideDown;
        if (Math.abs(rotUp.y()) >= Math.abs(rotFacing.y())) {
            upsideDown = rotUp.y() < 0 ? 4 : 0;
        } else {
            upsideDown = rotFacing.y() > 0 ? 4 : 0;
        }

        float absX = Math.abs(rotFacing.x()), absZ = Math.abs(rotFacing.z());
        int newFace6;
        if (absX > 0.001f || absZ > 0.001f) {
            newFace6 = nearestHorizFace6(rotFacing);
        } else {
            newFace6 = nearestHorizFace6(Vec3DFloat.from(-rotUp.x(), 0f, -rotUp.z()));
        }

        return upsideDown | face6ToStairs[newFace6];
    }

    // Furnace / Chest / EnderChest / Ladder / WallSign: meta 2=north, 3=south, 4=west, 5=east
    private static int rotateFurnaceFacing(int meta, Mat3DFloat rotation) {
        if (meta < 2 || meta > 5) return meta;
        int rotatedFace = rotateFace6(meta, rotation);
        if (rotatedFace < 2) {
            rotatedFace = nearestHorizFace6(rotation.mul(FACE6_VECS[meta]));
        }
        return rotatedFace;
    }

    // Dispenser / Dropper / Piston: bits 0-2 = facing (same as face6: 0=down, 1=up, 2-5=NSWE);
    // bit 3 = triggered/extended
    private static int rotateDispenserFacing(int meta, Mat3DFloat rotation) {
        int face = meta & 7;
        if (face > 5) return meta;
        return (meta & 8) | rotateFace6(face, rotation);
    }

    // Hopper: bits 0-2 = facing (0=down, 2=north, 3=south, 4=west, 5=east; 1=invalid);
    // bit 3 = disabled
    private static int rotateHopperFacing(int meta, Mat3DFloat rotation) {
        int face = meta & 7;
        if (face == 1 || face > 5) return meta;
        int rotatedFace = rotateFace6(face, rotation);
        // hopper cannot face up; remap up → down
        if (rotatedFace == 1) rotatedFace = 0;
        return (meta & 8) | rotatedFace;
    }

    // Pumpkin / Jack-o-lantern: meta 0=south, 1=west, 2=north, 3=east
    // Encoding matches horiz4 (S=0, W=1, N=2, E=3) exactly.
    private static int rotatePumpkinFacing(int meta, Mat3DFloat rotation) {
        return (meta & ~3) | rotateHoriz4(meta & 3, rotation);
    }

    // Door (bottom half only): bits 0-1 = facing (0=east, 1=south, 2=west, 3=north);
    // bit 3 marks top half — top half is not rotated (hinge metadata depends on neighbours).
    private static int rotateDoorFacing(int meta, Mat3DFloat rotation) {
        if ((meta & 8) != 0) return meta;
        // door facing → horiz4: E→3, S→0, W→1, N→2
        int[] doorToH4 = {3, 0, 1, 2};
        int[] h4ToDoor = {1, 2, 3, 0};
        int rotatedH4 = rotateHoriz4(doorToH4[meta & 3], rotation);
        return (meta & ~3) | h4ToDoor[rotatedH4];
    }

    // Trapdoor: bits 0-1 = hinge side (0=south, 1=north, 2=east, 3=west);
    // bit 2 = top-half; bit 3 = open
    private static int rotateTrapDoorFacing(int meta, Mat3DFloat rotation) {
        // trapdoor → horiz4: S→0, N→2, E→3, W→1
        int[] trapdoorToH4 = {0, 2, 3, 1};
        int[] h4ToTrapdoor = {0, 3, 1, 2};
        int rotatedH4 = rotateHoriz4(trapdoorToH4[meta & 3], rotation);
        return (meta & ~3) | h4ToTrapdoor[rotatedH4];
    }

    // Fence gate: bits 0-1 = facing (0=south, 1=west, 2=north, 3=east);
    // bit 2 = open; bit 3 = powered.
    // Encoding matches horiz4 exactly.
    private static int rotateFenceGateFacing(int meta, Mat3DFloat rotation) {
        return (meta & ~3) | rotateHoriz4(meta & 3, rotation);
    }

    // Bed (foot block): bits 0-1 = facing (0=south, 1=west, 2=north, 3=east);
    // bit 2 = occupied; bit 3 = head.
    // Encoding matches horiz4 exactly.
    private static int rotateBedFacing(int meta, Mat3DFloat rotation) {
        return (meta & ~3) | rotateHoriz4(meta & 3, rotation);
    }

    // Log: bits 0-1 = wood type; bits 2-3 = axis (0=Y, 1=X, 2=Z, 3=all-bark)
    private static int rotateLogAxis(int meta, Mat3DFloat rotation) {
        int axis = (meta >> 2) & 3;
        if (axis == 3) return meta;
        int rotatedAxis = rotateAxis(axis, rotation);
        return (meta & 3) | (rotatedAxis << 2);
    }

    // Quartz pillar: meta 2=Y axis, 3=X axis, 4=Z axis (1=default, 0=default)
    private static int rotateQuartzAxis(int meta, Mat3DFloat rotation) {
        if (meta < 2 || meta > 4) return meta;
        // quartz meta → axis index: 2→0(Y), 3→1(X), 4→2(Z)
        int axis = meta - 2;
        int rotatedAxis = rotateAxis(axis, rotation);
        return rotatedAxis + 2;
    }

    // Axis indices: Y=0, X=1, Z=2
    private static final Vec3DFloat[] AXIS_VECS = {
        Vec3DFloat.from(0, 1, 0), Vec3DFloat.from(1, 0, 0), Vec3DFloat.from(0, 0, 1)
    };

    private static int rotateAxis(int axis, Mat3DFloat rotation) {
        Vec3DFloat rotated = rotation.mul(AXIS_VECS[axis]);
        float absX = Math.abs(rotated.x()), absY = Math.abs(rotated.y()), absZ = Math.abs(rotated.z());
        if (absY >= absX && absY >= absZ) return 0;
        if (absX >= absZ) return 1;
        return 2;
    }

    // Torch / RedstoneTorch (wall-mounted):
    // meta 1=east, 2=west, 3=south, 4=north, 5=floor (unchanged)
    private static int rotateTorchFacing(int meta, Mat3DFloat rotation) {
        if (meta == 5 || meta < 1 || meta > 4) return meta;
        // meta → face6: 1→E(5), 2→W(4), 3→S(3), 4→N(2)
        int[] torchToFace6 = {-1, 5, 4, 3, 2};
        int[] face6ToTorch = {-1, -1, 4, 3, 2, 1};
        int rotatedFace = rotateFace6(torchToFace6[meta], rotation);
        if (rotatedFace < 2) {
            rotatedFace = nearestHorizFace6(rotation.mul(FACE6_VECS[torchToFace6[meta]]));
        }
        return face6ToTorch[rotatedFace];
    }

    // Vine: bitmask — bit0(1)=south, bit1(2)=west, bit2(4)=north, bit3(8)=east
    private static int rotateVineFaces(int meta, Mat3DFloat rotation) {
        // horiz4 → vine bitmask: S(0)→1, W(1)→2, N(2)→4, E(3)→8
        int[] h4ToVineBit = {1, 2, 4, 8};
        int result = 0;
        for (int direction = 0; direction < 4; direction++) {
            if ((meta & h4ToVineBit[direction]) != 0) {
                result |= h4ToVineBit[rotateHoriz4(direction, rotation)];
            }
        }
        return result;
    }

    // Button (wall-mounted): meta 1=east, 2=west, 3=south, 4=north; bit3(8)=powered
    // meta 0 (floor/down) and 5 (ceiling/up) are left unchanged.
    private static int rotateButtonFacing(int meta, Mat3DFloat rotation) {
        int face = meta & 7;
        if (face < 1 || face > 4) return meta;
        // meta → face6: 1→E(5), 2→W(4), 3→S(3), 4→N(2)
        int[] buttonToFace6 = {-1, 5, 4, 3, 2};
        int[] face6ToButton = {-1, -1, 4, 3, 2, 1};
        int rotatedFace = rotateFace6(buttonToFace6[face], rotation);
        if (rotatedFace < 2) {
            rotatedFace = nearestHorizFace6(rotation.mul(FACE6_VECS[buttonToFace6[face]]));
        }
        return (meta & 8) | face6ToButton[rotatedFace];
    }
}
