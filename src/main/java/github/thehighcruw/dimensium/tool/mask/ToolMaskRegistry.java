/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.mask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ToolMaskRegistry {

    public static final ToolMaskRegistry INSTANCE = new ToolMaskRegistry();

    private ToolMaskRegistry() {}

    public final List<MaskEntry> entries = new ArrayList<>();
    private ToolMask activeMask = null;

    public ToolMask getActiveMask() {
        return activeMask;
    }

    public void setActiveMask(ToolMask mask) {
        activeMask = mask;
    }

    /** Filter a packed-coordinate selection set by the active mask. Returns same set if no mask set. */
    public Set<Long> filterSelection(Set<Long> keys) {
        if (activeMask == null) return keys;
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return keys;
        Set<Long> result = new HashSet<>(keys.size());
        for (long key : keys) {
            int x = github.thehighcruw.dimensium.tool.state.SelectionState.unpackX(key);
            int y = github.thehighcruw.dimensium.tool.state.SelectionState.unpackY(key);
            int z = github.thehighcruw.dimensium.tool.state.SelectionState.unpackZ(key);
            if (activeMask.test(world, x, y, z)) result.add(key);
        }
        return result;
    }

    /** Filter ops by the active mask. Returns same list if no mask set. */
    public List<int[]> filter(List<int[]> ops) {
        if (activeMask == null) return ops;
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return ops;
        List<int[]> result = new ArrayList<>(ops.size());
        for (int[] op : ops) {
            if (activeMask.test(world, op[0], op[1], op[2])) result.add(op);
        }
        return result;
    }

    public ToolMask createMask(String baseName) {
        ToolMask mask = new ToolMask(uniqueName(baseName));
        entries.add(mask);
        save();
        return mask;
    }

    public MaskFolder createFolder(String baseName) {
        MaskFolder folder = new MaskFolder(uniqueName(baseName));
        entries.add(folder);
        save();
        return folder;
    }

    public void remove(MaskEntry entry) {
        if (!entries.remove(entry)) removeFromFolders(entries, entry);
        if (entry instanceof ToolMask && activeMask == entry) activeMask = null;
        if (entry instanceof MaskFolder) clearActiveMaskIfInFolder((MaskFolder) entry);
        save();
    }

    private boolean removeFromFolders(List<MaskEntry> list, MaskEntry target) {
        for (MaskEntry e : list) {
            if (e instanceof MaskFolder) {
                MaskFolder f = (MaskFolder) e;
                if (f.entries.remove(target)) return true;
                if (removeFromFolders(f.entries, target)) return true;
            }
        }
        return false;
    }

    private void clearActiveMaskIfInFolder(MaskFolder folder) {
        if (activeMask == null) return;
        for (MaskEntry e : folder.entries) {
            if (e == activeMask) {
                activeMask = null;
                return;
            }
            if (e instanceof MaskFolder) clearActiveMaskIfInFolder((MaskFolder) e);
        }
    }

    /** Find the list that directly contains the given entry (null = not found). */
    public List<MaskEntry> findParentList(MaskEntry target) {
        return findParentList(entries, target);
    }

    private List<MaskEntry> findParentList(List<MaskEntry> list, MaskEntry target) {
        for (MaskEntry e : list) {
            if (e == target) return list;
            if (e instanceof MaskFolder) {
                List<MaskEntry> found = findParentList(((MaskFolder) e).entries, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Collect all ToolMask instances (top-level and inside folders). */
    public List<ToolMask> allMasks() {
        List<ToolMask> out = new ArrayList<>();
        collectMasks(entries, out);
        return out;
    }

    private void collectMasks(List<MaskEntry> list, List<ToolMask> out) {
        for (MaskEntry e : list) {
            if (e instanceof ToolMask) out.add((ToolMask) e);
            else if (e instanceof MaskFolder) collectMasks(((MaskFolder) e).entries, out);
        }
    }

    // ── Unique name ───────────────────────────────────────────────────────────

    private String uniqueName(String base) {
        Set<String> existing = allNames();
        if (!existing.contains(base)) return base;
        for (int i = 1;; i++) {
            String candidate = base + " (" + i + ")";
            if (!existing.contains(candidate)) return candidate;
        }
    }

    private Set<String> allNames() {
        Set<String> names = new HashSet<>();
        collectNames(entries, names);
        return names;
    }

    private void collectNames(List<MaskEntry> list, Set<String> names) {
        for (MaskEntry e : list) {
            names.add(e.getName());
            if (e instanceof MaskFolder) collectNames(((MaskFolder) e).entries, names);
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void save() {
        MaskSerializer.save(entries);
    }

    public void load() {
        MaskSerializer.load(entries);
        // Re-validate active mask (might have been loaded from disk).
        if (activeMask != null && !allMasks().contains(activeMask)) activeMask = null;
    }
}
