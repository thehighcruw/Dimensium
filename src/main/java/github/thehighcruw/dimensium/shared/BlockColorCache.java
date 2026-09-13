/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import codechicken.nei.api.ItemInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BlockColorCache {

    private static final Logger LOG = LogManager.getLogger("Dimensium/BlockColorCache");

    public static final BlockColorCache INSTANCE = new BlockColorCache();

    public static final int CAT_SOLID = 1;
    public static final int CAT_TRANSLUCENT = 2;
    public static final int CAT_TILE_ENTITY = 8;

    private static final double MAX_FACE_LAB_DELTA = 20.0;

    // Populated during init(): blockId*16+meta → avgRgb
    private final Map<Integer, Integer> colorByKey = new HashMap<>();
    private final Map<Integer, String> nameByBlock = new HashMap<>();
    private final List<int[]> allEntries = new ArrayList<>();
    private final List<int[]> colourFieldCandidates = new ArrayList<>();
    // All placeable blocks with colour data: int[] { blockId, meta, avgRgb, catBits }
    private final List<int[]> allEntriesWithCat = new ArrayList<>();

    // iconName → avgRgb, populated from GL atlas read
    private final Map<String, Integer> spriteRgbByName = new HashMap<>();
    // iconName → within-face RGB variance (summed over R/G/B channels), populated from GL atlas read
    private final Map<String, Float> spriteVarianceByName = new HashMap<>();
    // iconName → animated flag
    private final Map<String, Boolean> spriteAnimated = new HashMap<>();

    // Set when TextureStitchEvent.Post fires for the block atlas.
    private volatile TextureMap pendingAtlas = null;
    // Set after GL read + candidate build completes.
    private volatile boolean initialized = false;

    private BlockColorCache() {}

    // ── Event hook ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) return; // only block atlas
        pendingAtlas = (TextureMap) event.map;
        // Reset so we re-scan on resource reload (F3+T).
        initialized = false;
        spriteRgbByName.clear();
        spriteVarianceByName.clear();
        spriteAnimated.clear();
        colorByKey.clear();
        nameByBlock.clear();
        allEntries.clear();
        colourFieldCandidates.clear();
        allEntriesWithCat.clear();
        LOG.info("BlockColorCache: block atlas stitched, will scan on next init() call");
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Must be called from the render thread after the game is fully loaded.
     * Reads the GL texture atlas and builds all caches. Safe to call every frame —
     * exits immediately once initialized.
     */
    public synchronized void init() {
        if (initialized) return;
        TextureMap atlas = pendingAtlas;
        if (atlas == null) return; // atlas not stitched yet

        LOG.info("BlockColorCache init: reading GL atlas");
        readAtlasFromGL(atlas);
        LOG.info(
            "BlockColorCache: spriteRgbByName has {} entries ({} animated)",
            spriteRgbByName.size(),
            spriteAnimated.values()
                .stream()
                .filter(v -> v)
                .count());

        buildCandidates();
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int blockColor(int blockId, int meta) {
        Integer rgb = colorByKey.get(blockId * 16 + meta);
        return rgb != null ? rgb : -1;
    }

    public List<int[]> getColourFieldCandidates() {
        return colourFieldCandidates;
    }

    /**
     * Returns up to {@code limit} ItemStacks whose average block colour is closest
     * to {@code targetRgb} in CIE-LAB space.
     *
     * @param sameTexture if true, restrict pool to blocks with uniform face colours
     *                    (i.e. colourFieldCandidates), otherwise use all blocks
     */
    public List<ItemStack> findSimilarBlocks(int targetRgb, boolean fullCube, boolean solidOnly, boolean opaque,
        boolean sameTexture, int limit) {

        List<int[]> pool = sameTexture ? colourFieldCandidates : allEntriesWithCat;
        double[] targetLab = rgbToLab(targetRgb);
        // e[5..7] = precomputed LAB * 100; compare squared distance (no sqrt needed for ordering)
        final double tL = targetLab[0], tA = targetLab[1], tB = targetLab[2];

        return pool.stream()
            .filter(e -> {
                if (solidOnly && (e[3] & CAT_SOLID) == 0) return false;
                if (opaque && (e[3] & CAT_TRANSLUCENT) != 0) return false;
                if (fullCube) {
                    Block b = Block.getBlockById(e[0]);
                    if (b == null || !b.renderAsNormalBlock()) return false;
                }
                return true;
            })
            .sorted(Comparator.comparingDouble(e -> {
                double dL = tL - e[5] / 100.0;
                double dA = tA - e[6] / 100.0;
                double dB = tB - e[7] / 100.0;
                return dL * dL + dA * dA + dB * dB;
            }))
            .limit(limit)
            // Secondary sort: prefer uniform textures (lower pixel variance) over noisy ones
            .sorted(Comparator.comparingDouble(e -> (double) Float.intBitsToFloat(e[4])))
            .map(e -> {
                Block b = Block.getBlockById(e[0]);
                if (b == null) return null;
                Item item = Item.getItemFromBlock(b);
                if (item == null) return null;
                return new ItemStack(item, 1, e[1]);
            })
            .filter(s -> s != null && s.getItem() != null)
            .collect(Collectors.toList());
    }

    // ── Phase 1: read atlas pixels from GL ───────────────────────────────────

    @SuppressWarnings("unchecked")
    private void readAtlasFromGL(TextureMap atlas) {
        // Bind the atlas texture and read all pixels.
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        int atlasW = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int atlasH = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (atlasW <= 0 || atlasH <= 0) {
            LOG.warn("BlockColorCache: atlas size {}x{} — aborting", atlasW, atlasH);
            return;
        }
        LOG.info("BlockColorCache: atlas size {}x{}", atlasW, atlasH);

        IntBuffer buf = BufferUtils.createIntBuffer(atlasW * atlasH);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        int[] pixels = new int[atlasW * atlasH];
        buf.get(pixels);

        // Pixels from glGetTexImage are RGBA; repack to ARGB for consistency.
        for (int i = 0; i < pixels.length; i++) {
            int px = pixels[i];
            int r = (px) & 0xFF;
            int g = (px >> 8) & 0xFF;
            int b = (px >> 16) & 0xFF;
            int a = (px >> 24) & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        // Sample each registered sprite — field is package-private, access via reflection.
        Map<String, TextureAtlasSprite> sprites = getSpriteMap(atlas);
        if (sprites == null || sprites.isEmpty()) {
            LOG.warn("BlockColorCache: could not access sprite map, got {}", sprites);
            return;
        }

        for (Map.Entry<String, TextureAtlasSprite> entry : sprites.entrySet()) {
            String name = entry.getKey();
            TextureAtlasSprite sprite = entry.getValue();
            int ox = sprite.getOriginX();
            int oy = sprite.getOriginY();
            int sw = sprite.getIconWidth();
            int sh = sprite.getIconHeight();
            boolean animated = sprite.getFrameCount() > 1;
            spriteAnimated.put(name, animated);
            if (animated) continue;
            if (ox < 0 || oy < 0 || sw <= 0 || sh <= 0 || ox + sw > atlasW || oy + sh > atlasH) continue;

            long sumR = 0, sumG = 0, sumB = 0;
            long sumR2 = 0, sumG2 = 0, sumB2 = 0;
            long count = 0;
            for (int py = oy; py < oy + sh; py++) {
                for (int px2 = ox; px2 < ox + sw; px2++) {
                    int px = pixels[py * atlasW + px2];
                    int alpha = (px >> 24) & 0xFF;
                    if (alpha < 64) continue;
                    long pr = (px >> 16) & 0xFF;
                    long pg = (px >> 8) & 0xFF;
                    long pb = px & 0xFF;
                    sumR += pr;
                    sumG += pg;
                    sumB += pb;
                    sumR2 += pr * pr;
                    sumG2 += pg * pg;
                    sumB2 += pb * pb;
                    count++;
                }
            }
            if (count == 0) continue;
            int avgRgb = (int) (sumR / count) << 16 | (int) (sumG / count) << 8 | (int) (sumB / count);
            spriteRgbByName.put(name, avgRgb);
            // Var(X) = E[X²] - E[X]² — summed over R, G, B channels
            double avgR = sumR / (double) count;
            double avgGd = sumG / (double) count;
            double avgBd = sumB / (double) count;
            float variance = (float) (sumR2 / (double) count - avgR * avgR
                + sumG2 / (double) count
                - avgGd * avgGd
                + sumB2 / (double) count
                - avgBd * avgBd);
            spriteVarianceByName.put(name, variance);
        }
    }

    // ── Phase 2: build candidates using ItemInfo ──────────────────────────────

    private void buildCandidates() {
        List<ItemStack> allItems = collectPlaceableBlocks();
        LOG.info("buildCandidates: {} placeable stacks", allItems.size());

        Map<Integer, BitSet> seen = new HashMap<>();
        int skippedDupe = 0, processed = 0;
        int statNoFaces = 0, statAnimated = 0, statDeltaRejected = 0, statAdded = 0;

        for (ItemStack stack : allItems) {
            if (stack == null || stack.getItem() == null) continue;
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block == null || block == Blocks.air) continue;
            int meta = stack.getItemDamage();
            if (meta == OreDictionary.WILDCARD_VALUE) continue;

            int blockId = Block.getIdFromBlock(block);
            BitSet seenMetas = seen.computeIfAbsent(blockId, k -> new BitSet(16));
            if (seenMetas.get(meta)) {
                skippedDupe++;
                continue;
            }
            seenMetas.set(meta);

            if (!nameByBlock.containsKey(blockId)) {
                String regName = (String) Block.blockRegistry.getNameForObject(block);
                nameByBlock.put(blockId, formatName(regName));
            }

            try {
                int[] faceRgbs = new int[6];
                int validFaces = 0;
                boolean anim = false;
                long totalR = 0, totalG = 0, totalB = 0;
                float totalVariance = 0;

                for (int face = 0; face < 6; face++) {
                    IIcon icon = block.getIcon(face, meta);
                    if (icon == null) continue;
                    String iconName = icon.getIconName();
                    if (Boolean.TRUE.equals(spriteAnimated.get(iconName))) {
                        anim = true;
                        break;
                    }
                    Integer rgb = spriteRgbByName.get(iconName);
                    if (rgb == null) continue;
                    faceRgbs[validFaces] = rgb;
                    totalR += (rgb >> 16) & 0xFF;
                    totalG += (rgb >> 8) & 0xFF;
                    totalB += rgb & 0xFF;
                    Float fvar = spriteVarianceByName.get(iconName);
                    if (fvar != null) totalVariance += fvar;
                    validFaces++;
                }

                if (anim) {
                    statAnimated++;
                    continue;
                }
                if (validFaces == 0) {
                    statNoFaces++;
                    continue;
                }

                int avgRgb = (int) (totalR / validFaces) << 16 | (int) (totalG / validFaces) << 8
                    | (int) (totalB / validFaces);
                float avgVariance = totalVariance / validFaces;

                int key = blockId * 16 + meta;
                colorByKey.put(key, avgRgb);
                allEntries.add(new int[] { key, avgRgb });

                double[] avgLab = rgbToLab(avgRgb);
                int catBits = categoryOf(block, meta);
                int varBits = Float.floatToRawIntBits(avgVariance);
                int labL = (int) (avgLab[0] * 100);
                int labA = (int) (avgLab[1] * 100);
                int labB = (int) (avgLab[2] * 100);
                allEntriesWithCat.add(new int[] { blockId, meta, avgRgb, catBits, varBits, labL, labA, labB });

                double maxFaceDelta = 0;
                for (int i = 0; i < validFaces; i++) {
                    double[] fl = rgbToLab(faceRgbs[i]);
                    double d = Math.sqrt(sq(fl[0] - avgLab[0]) + sq(fl[1] - avgLab[1]) + sq(fl[2] - avgLab[2]));
                    if (d > maxFaceDelta) maxFaceDelta = d;
                }

                if (maxFaceDelta > MAX_FACE_LAB_DELTA) {
                    statDeltaRejected++;
                    continue;
                }

                colourFieldCandidates.add(new int[] { blockId, meta, avgRgb, catBits, varBits, labL, labA, labB });
                statAdded++;
            } catch (Throwable t) {
                LOG.warn("buildCandidates threw for block {} meta {}: {}", blockId, meta, t.toString());
            }
            processed++;
        }

        LOG.info("buildCandidates done: processed={} dupes={}", processed, skippedDupe);
        LOG.info(
            "  animated={} noFaces={} deltaRejected={} added={}",
            statAnimated,
            statNoFaces,
            statDeltaRejected,
            statAdded);
        LOG.info("  colorByKey.size={} candidates={}", colorByKey.size(), colourFieldCandidates.size());
    }

    private List<ItemStack> collectPlaceableBlocks() {
        List<ItemStack> result = new ArrayList<>();
        for (Item item : (Iterable<Item>) Item.itemRegistry) {
            if (item == null || !(item instanceof ItemBlock)) continue;

            List<ItemStack> permutations = new ArrayList<>();
            permutations.addAll(ItemInfo.itemOverrides.get(item));
            if (permutations.isEmpty()) {
                item.getSubItems(item, null, permutations);
            }
            permutations.addAll(ItemInfo.itemVariants.get(item));
            permutations.removeIf(
                s -> s == null || s.getItem() == null
                    || s.getItemDamage() == OreDictionary.WILDCARD_VALUE
                    || Block.getBlockFromItem(s.getItem()) == null
                    || Block.getBlockFromItem(s.getItem()) == Blocks.air);
            result.addAll(permutations);
        }
        addGT5Machines(result);
        return result;
    }

    private static void addGT5Machines(List<ItemStack> out) {
        try {
            gregtech.api.interfaces.metatileentity.IMetaTileEntity[] mtes = gregtech.api.GregTechAPI.METATILEENTITIES;
            Block blockMachines = gregtech.api.GregTechAPI.sBlockMachines;
            if (blockMachines == null) return;
            Item blockItem = Item.getItemFromBlock(blockMachines);
            if (blockItem == null) return;
            BitSet covered = new BitSet(Short.MAX_VALUE);
            for (ItemStack s : out) {
                if (s != null && s.getItem() == blockItem) covered.set(s.getItemDamage());
            }
            if (mtes == null) return;
            for (int i = 0; i < mtes.length; i++) {
                if (mtes[i] == null || covered.get(i)) continue;
                out.add(new ItemStack(blockItem, 1, i));
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static Map<String, TextureAtlasSprite> getSpriteMap(TextureMap atlas) {
        // Try known MCP field names first (available after stitch).
        for (String fieldName : new String[] { "mapUploadedSprites", "mapRegisteredSprites" }) {
            try {
                java.lang.reflect.Field f = TextureMap.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                Object val = f.get(atlas);
                if (val instanceof Map) {
                    Map<?, ?> raw = (Map<?, ?>) val;
                    if (!raw.isEmpty()) return (Map<String, TextureAtlasSprite>) raw;
                }
            } catch (Throwable ignored) {}
        }
        // GTNH/OptiFine may remap field names — scan all declared fields for a non-empty
        // Map whose first value is a TextureAtlasSprite.
        for (java.lang.reflect.Field f : TextureMap.class.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(atlas);
                if (!(val instanceof Map)) continue;
                Map<?, ?> raw = (Map<?, ?>) val;
                if (raw.isEmpty()) continue;
                Object firstVal = raw.values()
                    .iterator()
                    .next();
                if (firstVal instanceof TextureAtlasSprite) {
                    LOG.info("BlockColorCache: found sprite map via field '{}'", f.getName());
                    return (Map<String, TextureAtlasSprite>) raw;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int categoryOf(Block block, int meta) {
        boolean hasTe = block.hasTileEntity(meta);
        boolean opaque = block.isOpaqueCube();
        int cat = 0;
        if (hasTe) cat |= CAT_TILE_ENTITY;
        if (opaque) cat |= CAT_SOLID;
        if (!opaque && !hasTe) cat |= CAT_TRANSLUCENT;
        return cat;
    }

    private static double sq(double x) {
        return x * x;
    }

    public static double[] rgbToLab(int rgb) {
        double r = linearize(((rgb >> 16) & 0xFF) / 255.0);
        double g = linearize(((rgb >> 8) & 0xFF) / 255.0);
        double b = linearize((rgb & 0xFF) / 255.0);
        double x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b;
        double y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b;
        double z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b;
        double fx = labF(x / 0.95047), fy = labF(y / 1.0), fz = labF(z / 1.08883);
        return new double[] { 116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz) };
    }

    private static double linearize(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double labF(double t) {
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116.0;
    }

    private static String formatName(String registryName) {
        if (registryName == null) return "Unknown";
        int colon = registryName.indexOf(':');
        String plain = colon >= 0 ? registryName.substring(colon + 1) : registryName;
        String[] parts = plain.split("[_:]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
