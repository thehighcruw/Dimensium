/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/** Persisted MRU list of picked blocks. */
@SideOnly(Side.CLIENT)
public class RecentBlockHistory {

    public static final int MAX = 20;

    private static final LinkedList<ItemStack> history = new LinkedList<>();
    private static boolean loaded = false;

    public static void add(ItemStack stack) {
        ensureLoaded();
        // remove earlier occurrence of same block+meta
        history.removeIf(s -> s.getItem() == stack.getItem() && s.getItemDamage() == stack.getItemDamage());
        history.addFirst(stack.copy());
        if (history.size() > MAX) history.removeLast();
        save();
    }

    public static List<ItemStack> get() {
        ensureLoaded();
        return Collections.unmodifiableList(history);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static void ensureLoaded() {
        if (!loaded) {
            load();
            loaded = true;
        }
    }

    private static File saveFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "dimensium_recent_blocks.txt");
    }

    private static void save() {
        try (PrintWriter w =
                new PrintWriter(new OutputStreamWriter(new FileOutputStream(saveFile()), StandardCharsets.UTF_8))) {
            for (ItemStack s : history) {
                Block b = Block.getBlockFromItem(s.getItem());
                if (b == null) continue;
                String name = Block.blockRegistry.getNameForObject(b);
                if (name == null) continue;
                w.println(name + ":" + s.getItemDamage());
            }
        } catch (IOException ignored) {
        }
    }

    private static void load() {
        history.clear();
        File f = saveFile();
        if (!f.exists()) return;
        try (BufferedReader r =
                new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null && history.size() < MAX) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int lastColon = line.lastIndexOf(':');
                if (lastColon < 1) continue;
                String blockName = line.substring(0, lastColon);
                int meta = Integer.parseInt(line.substring(lastColon + 1));
                Block b = (Block) Block.blockRegistry.getObject(blockName);
                if (b == null || b == Blocks.air) continue;
                history.addLast(new ItemStack(b, 1, meta));
            }
        } catch (IOException | NumberFormatException ignored) {
        }
    }
}
