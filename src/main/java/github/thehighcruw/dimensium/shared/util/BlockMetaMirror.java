/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.util;

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
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;

/** Mirrors block metadata across an axis-aligned plane. */
public class BlockMetaMirror {

    private BlockMetaMirror() {}

    /** Mirror across the YZ plane (negate X): east ↔ west. */
    public static int mirrorX(Block block, int meta) {
        if (block instanceof BlockStairs) return mirrorStairsX(meta);
        if (block instanceof BlockFurnace
                || block instanceof BlockChest
                || block instanceof BlockEnderChest
                || block instanceof BlockLadder
                || block instanceof BlockSign) return mirrorFurnaceFacingX(meta);
        if (block instanceof BlockDispenser
                || block instanceof BlockDropper
                || block instanceof BlockPistonBase
                || block instanceof BlockPistonExtension) return mirrorDispenserFacingX(meta);
        if (block instanceof BlockHopper) return mirrorHopperFacingX(meta);
        if (block instanceof BlockPumpkin) return mirrorHoriz4EW(meta & 3) | (meta & ~3);
        if (block instanceof BlockDoor) return mirrorDoorX(meta);
        if (block instanceof BlockTrapDoor) return mirrorTrapDoorX(meta);
        if (block instanceof BlockFenceGate) return mirrorHoriz4EW(meta & 3) | (meta & ~3);
        if (block instanceof BlockBed) return mirrorHoriz4EW(meta & 3) | (meta & ~3);
        if (block instanceof BlockTorch || block instanceof BlockRedstoneTorch) return mirrorTorchX(meta);
        if (block instanceof BlockVine) return mirrorVineX(meta);
        if (block instanceof BlockButton) return mirrorButtonX(meta);
        return meta;
    }

    /** Mirror across the XY plane (negate Z): north ↔ south. */
    public static int mirrorZ(Block block, int meta) {
        if (block instanceof BlockStairs) return mirrorStairsZ(meta);
        if (block instanceof BlockFurnace
                || block instanceof BlockChest
                || block instanceof BlockEnderChest
                || block instanceof BlockLadder
                || block instanceof BlockSign) return mirrorFurnaceFacingZ(meta);
        if (block instanceof BlockDispenser
                || block instanceof BlockDropper
                || block instanceof BlockPistonBase
                || block instanceof BlockPistonExtension) return mirrorDispenserFacingZ(meta);
        if (block instanceof BlockHopper) return mirrorHopperFacingZ(meta);
        if (block instanceof BlockPumpkin) return mirrorHoriz4NS(meta & 3) | (meta & ~3);
        if (block instanceof BlockDoor) return mirrorDoorZ(meta);
        if (block instanceof BlockTrapDoor) return mirrorTrapDoorZ(meta);
        if (block instanceof BlockFenceGate) return mirrorHoriz4NS(meta & 3) | (meta & ~3);
        if (block instanceof BlockBed) return mirrorHoriz4NS(meta & 3) | (meta & ~3);
        if (block instanceof BlockTorch || block instanceof BlockRedstoneTorch) return mirrorTorchZ(meta);
        if (block instanceof BlockVine) return mirrorVineZ(meta);
        if (block instanceof BlockButton) return mirrorButtonZ(meta);
        return meta;
    }

    // ── Stairs: bits 0-1 = facing (0=east, 1=west, 2=south, 3=north); bit 2 = upside-down ──

    private static int mirrorStairsX(int meta) {
        int facing = meta & 3;
        if (facing == 0) return (meta & ~3) | 1;
        if (facing == 1) return (meta & ~3) | 0;
        return meta;
    }

    private static int mirrorStairsZ(int meta) {
        int facing = meta & 3;
        if (facing == 2) return (meta & ~3) | 3;
        if (facing == 3) return (meta & ~3) | 2;
        return meta;
    }

    // ── Furnace / Chest / EnderChest / Ladder / WallSign: 2=north, 3=south, 4=west, 5=east ──

    private static int mirrorFurnaceFacingX(int meta) {
        if (meta == 4) return 5;
        if (meta == 5) return 4;
        return meta;
    }

    private static int mirrorFurnaceFacingZ(int meta) {
        if (meta == 2) return 3;
        if (meta == 3) return 2;
        return meta;
    }

    // ── Dispenser / Dropper / Piston: bits 0-2 = face6; bit 3 = triggered/extended ──

    private static int mirrorDispenserFacingX(int meta) {
        int face = meta & 7;
        if (face == 4) return (meta & 8) | 5;
        if (face == 5) return (meta & 8) | 4;
        return meta;
    }

    private static int mirrorDispenserFacingZ(int meta) {
        int face = meta & 7;
        if (face == 2) return (meta & 8) | 3;
        if (face == 3) return (meta & 8) | 2;
        return meta;
    }

    // ── Hopper: bits 0-2 = face6 (no up); bit 3 = disabled ──

    private static int mirrorHopperFacingX(int meta) {
        int face = meta & 7;
        if (face == 4) return (meta & 8) | 5;
        if (face == 5) return (meta & 8) | 4;
        return meta;
    }

    private static int mirrorHopperFacingZ(int meta) {
        int face = meta & 7;
        if (face == 2) return (meta & 8) | 3;
        if (face == 3) return (meta & 8) | 2;
        return meta;
    }

    // ── Horiz4 helpers (S=0, W=1, N=2, E=3) ──

    private static int mirrorHoriz4EW(int direction) {
        if (direction == 1) return 3;
        if (direction == 3) return 1;
        return direction;
    }

    private static int mirrorHoriz4NS(int direction) {
        if (direction == 0) return 2;
        if (direction == 2) return 0;
        return direction;
    }

    // ── Door: bottom bits 0-1 = facing (0=east, 1=south, 2=west, 3=north); top bit 0 = hinge ──
    // Any reflection changes chirality, so the hinge side always toggles on the top half.

    private static int mirrorDoorX(int meta) {
        if ((meta & 8) != 0) return meta ^ 1; // top half: toggle hinge
        int facing = meta & 3;
        if (facing == 0) return (meta & ~3) | 2;
        if (facing == 2) return (meta & ~3) | 0;
        return meta;
    }

    private static int mirrorDoorZ(int meta) {
        if ((meta & 8) != 0) return meta ^ 1; // top half: toggle hinge
        int facing = meta & 3;
        if (facing == 1) return (meta & ~3) | 3;
        if (facing == 3) return (meta & ~3) | 1;
        return meta;
    }

    // ── Trapdoor: bits 0-1 = hinge side (0=south, 1=north, 2=east, 3=west) ──

    private static int mirrorTrapDoorX(int meta) {
        int side = meta & 3;
        if (side == 2) return (meta & ~3) | 3;
        if (side == 3) return (meta & ~3) | 2;
        return meta;
    }

    private static int mirrorTrapDoorZ(int meta) {
        int side = meta & 3;
        if (side == 0) return (meta & ~3) | 1;
        if (side == 1) return (meta & ~3) | 0;
        return meta;
    }

    // ── Torch / RedstoneTorch: 1=east, 2=west, 3=south, 4=north, 5=floor ──

    private static int mirrorTorchX(int meta) {
        if (meta == 1) return 2;
        if (meta == 2) return 1;
        return meta;
    }

    private static int mirrorTorchZ(int meta) {
        if (meta == 3) return 4;
        if (meta == 4) return 3;
        return meta;
    }

    // ── Vine: bit0(1)=south, bit1(2)=west, bit2(4)=north, bit3(8)=east ──

    private static int mirrorVineX(int meta) {
        // swap west (bit1=2) and east (bit3=8)
        int west = (meta >> 1) & 1;
        int east = (meta >> 3) & 1;
        return (meta & ~(2 | 8)) | (west << 3) | (east << 1);
    }

    private static int mirrorVineZ(int meta) {
        // swap south (bit0=1) and north (bit2=4)
        int south = meta & 1;
        int north = (meta >> 2) & 1;
        return (meta & ~(1 | 4)) | (south << 2) | north;
    }

    // ── Button: 1=east, 2=west, 3=south, 4=north; bit3(8)=powered ──

    private static int mirrorButtonX(int meta) {
        int face = meta & 7;
        if (face == 1) return (meta & 8) | 2;
        if (face == 2) return (meta & 8) | 1;
        return meta;
    }

    private static int mirrorButtonZ(int meta) {
        int face = meta & 7;
        if (face == 3) return (meta & 8) | 4;
        if (face == 4) return (meta & 8) | 3;
        return meta;
    }
}
