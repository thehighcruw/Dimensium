package github.thehighcruw.dimensium.tool.mask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class MaskSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private MaskSerializer() {}

    public static File saveFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "dimensium_masks.json");
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public static void save(List<MaskEntry> entries) {
        JsonObject root = new JsonObject();
        root.add("entries", serializeEntries(entries));
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(saveFile()), StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonArray serializeEntries(List<MaskEntry> entries) {
        JsonArray arr = new JsonArray();
        for (MaskEntry entry : entries) arr.add(serializeEntry(entry));
        return arr;
    }

    private static JsonElement serializeEntry(MaskEntry entry) {
        JsonObject obj = new JsonObject();
        if (entry instanceof ToolMask) {
            ToolMask mask = (ToolMask) entry;
            obj.addProperty("type", "mask");
            obj.addProperty("name", mask.getName());
            if (mask.getRoot() != null) obj.add("root", serializeNode(mask.getRoot()));
        } else if (entry instanceof MaskFolder) {
            MaskFolder folder = (MaskFolder) entry;
            obj.addProperty("type", "folder");
            obj.addProperty("name", folder.getName());
            obj.add("entries", serializeEntries(folder.entries));
        }
        return obj;
    }

    private static JsonElement serializeNode(MaskNode node) {
        JsonObject obj = new JsonObject();
        if (node instanceof OrNode) {
            obj.addProperty("t", "OR");
            obj.add("c", serializeChildren(((LogicNode) node).children));
        } else if (node instanceof AndNode) {
            obj.addProperty("t", "AND");
            obj.add("c", serializeChildren(((LogicNode) node).children));
        } else if (node instanceof NotNode) {
            obj.addProperty("t", "NOT");
            obj.add("c", serializeChildren(((LogicNode) node).children));
        } else if (node instanceof OffsetNode) {
            OffsetNode n = (OffsetNode) node;
            obj.addProperty("t", "OFFSET");
            obj.addProperty("dx", n.dx);
            obj.addProperty("dy", n.dy);
            obj.addProperty("dz", n.dz);
            obj.add("c", serializeChildren(n.children));
        } else if (node instanceof BlockMask) {
            BlockMask n = (BlockMask) node;
            obj.addProperty("t", "Block");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
        } else if (node instanceof AboveMask) {
            AboveMask n = (AboveMask) node;
            obj.addProperty("t", "Above");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
        } else if (node instanceof BelowMask) {
            BelowMask n = (BelowMask) node;
            obj.addProperty("t", "Below");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
        } else if (node instanceof NearMask) {
            NearMask n = (NearMask) node;
            obj.addProperty("t", "Near");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
            obj.addProperty("r", n.radius);
        } else if (node instanceof NeighbourMask) {
            NeighbourMask n = (NeighbourMask) node;
            obj.addProperty("t", "Neighbour");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
        } else if (node instanceof AdjacentMask) {
            AdjacentMask n = (AdjacentMask) node;
            obj.addProperty("t", "Adjacent");
            obj.addProperty("id", n.blockId);
            obj.addProperty("meta", n.meta);
        } else if (node instanceof YMask) {
            YMask n = (YMask) node;
            obj.addProperty("t", "Y");
            obj.addProperty("op", n.op.name());
            obj.addProperty("v", n.value);
        } else if (node instanceof AngleMask) {
            AngleMask n = (AngleMask) node;
            obj.addProperty("t", "Angle");
            obj.addProperty("a", n.angle);
            obj.addProperty("r", n.range);
        } else if (node instanceof InSelectionMask) {
            obj.addProperty("t", "InSelection");
        } else if (node instanceof CanSeeSkyMask) {
            obj.addProperty("t", "CanSeeSky");
        } else if (node instanceof SurfaceMask) {
            obj.addProperty("t", "Surface");
        }
        return obj;
    }

    private static JsonArray serializeChildren(List<MaskNode> children) {
        JsonArray arr = new JsonArray();
        for (MaskNode child : children) arr.add(serializeNode(child));
        return arr;
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    public static void load(List<MaskEntry> entries) {
        File file = saveFile();
        if (!file.exists()) return;
        try (InputStreamReader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(r)
                .getAsJsonObject();
            entries.clear();
            JsonArray arr = root.getAsJsonArray("entries");
            if (arr != null) {
                for (JsonElement el : arr) {
                    MaskEntry entry = deserializeEntry(el.getAsJsonObject());
                    if (entry != null) entries.add(entry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MaskEntry deserializeEntry(JsonObject obj) {
        String type = obj.has("type") ? obj.get("type")
            .getAsString() : "";
        String name = obj.has("name") ? obj.get("name")
            .getAsString() : "Unnamed";
        if ("mask".equals(type)) {
            ToolMask mask = new ToolMask(name);
            if (obj.has("root")) {
                MaskNode root = deserializeNode(obj.getAsJsonObject("root"));
                if (root != null) mask.setRoot(root);
            }
            return mask;
        } else if ("folder".equals(type)) {
            MaskFolder folder = new MaskFolder(name);
            if (obj.has("entries")) {
                for (JsonElement el : obj.getAsJsonArray("entries")) {
                    MaskEntry child = deserializeEntry(el.getAsJsonObject());
                    if (child != null) folder.entries.add(child);
                }
            }
            return folder;
        }
        return null;
    }

    private static MaskNode deserializeNode(JsonObject obj) {
        if (!obj.has("t")) return null;
        String t = obj.get("t")
            .getAsString();
        switch (t) {
            case "OR": {
                OrNode n = new OrNode();
                loadChildren(n, obj);
                return n;
            }
            case "AND": {
                AndNode n = new AndNode();
                loadChildren(n, obj);
                return n;
            }
            case "NOT": {
                NotNode n = new NotNode();
                loadChildren(n, obj);
                return n;
            }
            case "OFFSET": {
                OffsetNode n = new OffsetNode(
                    obj.has("dx") ? obj.get("dx")
                        .getAsInt() : 0,
                    obj.has("dy") ? obj.get("dy")
                        .getAsInt() : -1,
                    obj.has("dz") ? obj.get("dz")
                        .getAsInt() : 0);
                loadChildren(n, obj);
                return n;
            }
            case "Block":
                return new BlockMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt());
            case "Above":
                return new AboveMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt());
            case "Below":
                return new BelowMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt());
            case "Near":
                return new NearMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt(),
                    obj.has("r") ? obj.get("r")
                        .getAsInt() : 3);
            case "Neighbour":
                return new NeighbourMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt());
            case "Adjacent":
                return new AdjacentMask(
                    obj.get("id")
                        .getAsInt(),
                    obj.get("meta")
                        .getAsInt());
            case "Y": {
                YMask.Op op = YMask.Op.EQUAL;
                try {
                    op = YMask.Op.valueOf(
                        obj.get("op")
                            .getAsString());
                } catch (Exception ignored) {}
                return new YMask(
                    op,
                    obj.has("v") ? obj.get("v")
                        .getAsInt() : 64);
            }
            case "Angle":
                return new AngleMask(
                    obj.has("a") ? obj.get("a")
                        .getAsFloat() : 0f,
                    obj.has("r") ? obj.get("r")
                        .getAsFloat() : 10f);
            case "InSelection":
                return new InSelectionMask();
            case "CanSeeSky":
                return new CanSeeSkyMask();
            case "Surface":
                return new SurfaceMask();
            default:
                return null;
        }
    }

    private static void loadChildren(LogicNode node, JsonObject obj) {
        if (!obj.has("c")) return;
        for (JsonElement el : obj.getAsJsonArray("c")) {
            MaskNode child = deserializeNode(el.getAsJsonObject());
            if (child != null) node.children.add(child);
        }
    }
}
