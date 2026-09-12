/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PaletteRegistry {

    public static final PaletteRegistry INSTANCE = new PaletteRegistry();

    private final List<PaletteCategory> categories = new ArrayList<>();

    public static class PaletteCategory {

        public String name;
        public final List<ItemStack> blocks = new ArrayList<>();

        public PaletteCategory(String name) {
            this.name = name;
        }
    }

    private PaletteRegistry() {}

    public List<PaletteCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public void addCategory(String name) {
        categories.add(new PaletteCategory(name));
        save();
    }

    public void removeCategory(int index) {
        if (index < 0 || index >= categories.size()) return;
        categories.remove(index);
        save();
    }

    public void renameCategory(int index, String name) {
        if (index < 0 || index >= categories.size()) return;
        categories.get(index).name = name;
        save();
    }

    public void moveUp(int index) {
        if (index <= 0 || index >= categories.size()) return;
        Collections.swap(categories, index, index - 1);
        save();
    }

    public void moveDown(int index) {
        if (index < 0 || index >= categories.size() - 1) return;
        Collections.swap(categories, index, index + 1);
        save();
    }

    public void addBlock(int categoryIndex, ItemStack stack) {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) return;
        List<ItemStack> blocks = categories.get(categoryIndex).blocks;
        blocks.removeIf(s -> s.getItem() == stack.getItem() && s.getItemDamage() == stack.getItemDamage());
        blocks.add(stack.copy());
        save();
    }

    public void removeBlock(int categoryIndex, int blockIndex) {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) return;
        List<ItemStack> blocks = categories.get(categoryIndex).blocks;
        if (blockIndex < 0 || blockIndex >= blocks.size()) return;
        blocks.remove(blockIndex);
        save();
    }

    public void replaceBlock(int categoryIndex, int blockIndex, ItemStack stack) {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) return;
        List<ItemStack> blocks = categories.get(categoryIndex).blocks;
        if (blockIndex < 0 || blockIndex >= blocks.size()) return;
        blocks.set(blockIndex, stack.copy());
        save();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private File saveFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "dimensium_palette.dat");
    }

    public void save() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList catList = new NBTTagList();
        for (PaletteCategory cat : categories) {
            NBTTagCompound catTag = new NBTTagCompound();
            catTag.setString("name", cat.name);
            NBTTagList blockList = new NBTTagList();
            for (ItemStack s : cat.blocks) {
                Block b = Block.getBlockFromItem(s.getItem());
                if (b == null) continue;
                String blockName = (String) Block.blockRegistry.getNameForObject(b);
                if (blockName == null) continue;
                NBTTagCompound blockTag = new NBTTagCompound();
                blockTag.setString("block", blockName);
                blockTag.setInteger("meta", s.getItemDamage());
                blockList.appendTag(blockTag);
            }
            catTag.setTag("blocks", blockList);
            catList.appendTag(catTag);
        }
        root.setTag("categories", catList);
        try (FileOutputStream fos = new FileOutputStream(saveFile())) {
            CompressedStreamTools.writeCompressed(root, fos);
        } catch (IOException ignored) {}
    }

    public void load() {
        categories.clear();
        File file = saveFile();
        if (!file.exists()) return;
        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            NBTTagList catList = root.getTagList("categories", 10);
            for (int i = 0; i < catList.tagCount(); i++) {
                NBTTagCompound catTag = catList.getCompoundTagAt(i);
                PaletteCategory cat = new PaletteCategory(catTag.getString("name"));
                NBTTagList blockList = catTag.getTagList("blocks", 10);
                for (int j = 0; j < blockList.tagCount(); j++) {
                    NBTTagCompound blockTag = blockList.getCompoundTagAt(j);
                    String blockName = blockTag.getString("block");
                    int meta = blockTag.getInteger("meta");
                    Block b = (Block) Block.blockRegistry.getObject(blockName);
                    if (b == null || b == Blocks.air) continue;
                    cat.blocks.add(new ItemStack(b, 1, meta));
                }
                categories.add(cat);
            }
        } catch (IOException ignored) {}
    }
}
