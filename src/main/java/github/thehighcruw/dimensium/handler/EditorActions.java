/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.handler;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.history.ClientEditHistory;
import github.thehighcruw.dimensium.render.popup.ClipboardWindow;
import github.thehighcruw.dimensium.render.popup.ConflictPopup;
import github.thehighcruw.dimensium.tool.state.SelectedBlockState;
import github.thehighcruw.dimensium.tool.state.SelectionState;

@SideOnly(Side.CLIENT)
public final class EditorActions {

    private EditorActions() {}

    public static void undo() {
        ClientEditHistory history = ClientEditHistory.INSTANCE;
        List<int[]> before = history.peekUndo();
        if (before == null) return;
        int[][] expected = history.peekUndoExpected();
        int mismatches = countMismatches(expected);
        if (mismatches > 0) {
            ConflictPopup.INSTANCE.show(mismatches, () -> {
                history.commitUndo();
                BlockSender.sendChunkedSkipHistory(before);
            }, history::commitUndo);
        } else {
            history.commitUndo();
            BlockSender.sendChunkedSkipHistory(before);
        }
    }

    public static void redo() {
        ClientEditHistory history = ClientEditHistory.INSTANCE;
        List<int[]> after = history.peekRedo();
        if (after == null) return;
        int[][] expected = history.peekRedoExpected();
        int mismatches = countMismatches(expected);
        if (mismatches > 0) {
            ConflictPopup.INSTANCE.show(mismatches, () -> {
                history.commitRedo();
                BlockSender.sendChunkedSkipHistory(after);
            }, history::commitRedo);
        } else {
            history.commitRedo();
            BlockSender.sendChunkedSkipHistory(after);
        }
    }

    public static void copy() {
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.hasSelection()) sel.captureFromWorld(Minecraft.getMinecraft().theWorld);
    }

    public static void cut() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        sel.captureFromWorld(Minecraft.getMinecraft().theWorld);
        BlockSender.sendChunked(SelectionOps.selectionToAirOps(sel), I18n.format("dimensium.action.cut"));
        sel.clearSelection();
    }

    public static void drain() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        BlockSender.sendChunked(SelectionOps.drainOps(sel, world), I18n.format("dimensium.action.op.drain"));
    }

    public static void fillNearest() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        BlockSender
            .sendChunked(SelectionOps.fillNearestOps(sel, world), I18n.format("dimensium.action.op.fill_nearest"));
    }

    public static void hollow() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        BlockSender.sendChunked(SelectionOps.hollowOps(sel), I18n.format("dimensium.action.op.hollow"));
    }

    public static void fillGaps() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        SelectedBlockState sbs = SelectedBlockState.INSTANCE;
        BlockSender.sendChunked(
            SelectionOps.fillGapsOps(sel, world, sbs.getPaintBlock(), sbs.getPaintMeta()),
            I18n.format("dimensium.action.op.fill_gaps"));
    }

    public static void simulateGravity() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        BlockSender.sendChunked(
            SelectionOps.simulateGravityOps(sel, world),
            I18n.format("dimensium.action.op.simulate_gravity"));
    }

    public static void triggerUpdates() {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        World world = Minecraft.getMinecraft().theWorld;
        BlockSender.sendChunked(
            SelectionOps.triggerUpdatesOps(sel, world),
            I18n.format("dimensium.action.op.trigger_updates"));
    }

    public static void generateColourField(int includeMask) {
        SelectionState sel = SelectionState.INSTANCE;
        if (!sel.hasSelection()) return;
        BlockSender.sendChunked(
            SelectionOps.generateColourFieldOps(sel, includeMask),
            I18n.format("dimensium.action.op.colour_field"));
    }

    public static void saveBlueprint() {
        SelectionState sel = SelectionState.INSTANCE;
        if (sel.clipboard == null || sel.clipboard.isEmpty()) return;
        ClipboardWindow.INSTANCE.requestBlueprintOpen();
    }

    public static int countMismatches(int[][] expected) {
        if (expected == null) return 0;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return 0;
        int count = 0;
        for (int[] b : expected) {
            int curId = Block.getIdFromBlock(mc.theWorld.getBlock(b[0], b[1], b[2]));
            int curMeta = mc.theWorld.getBlockMetadata(b[0], b[1], b[2]);
            if (curId != b[3] || curMeta != b[4]) count++;
        }
        return count;
    }
}
