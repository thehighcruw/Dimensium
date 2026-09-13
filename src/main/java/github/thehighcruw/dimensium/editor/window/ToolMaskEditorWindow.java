/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.window;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.DimensiumConfig;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.tool.mask.AboveMask;
import github.thehighcruw.dimensium.editor.tool.mask.AdjacentMask;
import github.thehighcruw.dimensium.editor.tool.mask.AndNode;
import github.thehighcruw.dimensium.editor.tool.mask.AngleMask;
import github.thehighcruw.dimensium.editor.tool.mask.BelowMask;
import github.thehighcruw.dimensium.editor.tool.mask.BlockMask;
import github.thehighcruw.dimensium.editor.tool.mask.CanSeeSkyMask;
import github.thehighcruw.dimensium.editor.tool.mask.InSelectionMask;
import github.thehighcruw.dimensium.editor.tool.mask.LogicNode;
import github.thehighcruw.dimensium.editor.tool.mask.MaskNode;
import github.thehighcruw.dimensium.editor.tool.mask.NearMask;
import github.thehighcruw.dimensium.editor.tool.mask.NeighbourMask;
import github.thehighcruw.dimensium.editor.tool.mask.NotNode;
import github.thehighcruw.dimensium.editor.tool.mask.OffsetNode;
import github.thehighcruw.dimensium.editor.tool.mask.OrNode;
import github.thehighcruw.dimensium.editor.tool.mask.SurfaceMask;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMask;
import github.thehighcruw.dimensium.editor.tool.mask.ToolMaskRegistry;
import github.thehighcruw.dimensium.editor.tool.mask.YMask;
import github.thehighcruw.dimensium.editor.window.imgui.DeferredItemRender;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiWindow;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

@SideOnly(Side.CLIENT)
public class ToolMaskEditorWindow extends ImGuiWindow {

    public static final ToolMaskEditorWindow INSTANCE = new ToolMaskEditorWindow();

    private static final String WINDOW_ID = "###tool_mask_editor";

    private boolean open = false;
    private ToolMask editingMask = null;
    private MaskNode selectedNode = null;

    // Drag-drop state
    private MaskNode dragNode = null;
    private List<MaskNode> dragNodeParentList = null;
    private int dragNodeIndex = -1;

    private final ImString newMaskName = new ImString(64);
    private final ImString maskSearchFilter = new ImString(64);
    private boolean maskSearchFocusPending = false;

    private ToolMaskEditorWindow() {}

    public void open() {
        setOpen(true);
    }

    public void setOpen(boolean value) {
        open = value;
        DimensiumConfig.setWindowToolMaskEditorOpen(value);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public void renderImGui() {
        if (!open) return;

        float scale = ImGuiManager.INSTANCE.getUIScale();
        ImBoolean pOpen = new ImBoolean(true);

        ImGui.setNextWindowSize(420f * scale, 520f * scale, ImGuiCond.FirstUseEver);
        ImGui.begin(I18n.format("dimensium.mask.editor.title") + WINDOW_ID, pOpen, ImGuiWindowFlags.None);
        captureBounds();
        if (pOpen.get()) {
            renderMaskSelector(scale);
            ImGui.separator();
            if (editingMask != null) {
                renderMaskStringBar();
                ImGui.separator();
                renderTree(scale);
            }
        }
        ImGui.end();
        if (!pOpen.get()) {
            setOpen(false);
            ToolMaskRegistry.INSTANCE.save();
        }
    }

    private void renderMaskSelector(float scale) {
        ToolMaskRegistry registry = ToolMaskRegistry.INSTANCE;
        List<ToolMask> all = registry.allMasks();

        String currentName = editingMask != null ? editingMask.getName()
            : I18n.format("dimensium.mask.editor.select_prompt");
        ImGui.setNextItemWidth(200f * scale);
        if (ImGui.beginCombo("##mask_select", currentName)) {
            if (ImGui.isWindowAppearing()) {
                maskSearchFilter.set("");
                maskSearchFocusPending = true;
            }
            if (maskSearchFocusPending) {
                ImGui.setKeyboardFocusHere();
                maskSearchFocusPending = false;
            }
            ImGui.setNextItemWidth(-1f);
            ImGui
                .inputTextWithHint("##mask_search", I18n.format("dimensium.mask.editor.search_hint"), maskSearchFilter);
            String filter = maskSearchFilter.get()
                .toLowerCase(java.util.Locale.ROOT);
            for (ToolMask m : all) {
                if (!filter.isEmpty() && !m.getName()
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(filter)) continue;
                boolean sel = (m == editingMask);
                if (ImGui.selectable(m.getName(), sel)) {
                    editingMask = m;
                    selectedNode = null;
                }
            }
            ImGui.endCombo();
        }

        ImGui.sameLine();
        if (ImGui.button(I18n.format("dimensium.mask.editor.new"))) {
            ImGui.openPopup("##new_mask_popup");
        }

        if (ImGui.beginPopup("##new_mask_popup")) {
            ImGui.text(I18n.format("dimensium.mask.editor.new_name"));
            ImGui.setNextItemWidth(180f * scale);
            ImGui.inputText("##new_mask_name", newMaskName);
            if (ImGui.button(I18n.format("dimensium.mask.editor.create"))) {
                String name = newMaskName.get()
                    .trim();
                if (!name.isEmpty()) {
                    editingMask = registry.createMask(name);
                    selectedNode = null;
                    newMaskName.set("");
                }
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }
    }

    private void renderMaskStringBar() {
        String str = editingMask.toMaskString();
        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.15f, 0.15f, 0.15f, 1f);
        ImGui.setNextItemWidth(-1f);
        ImGui.inputText("##mask_str", new ImString(str, str.length() + 1), imgui.flag.ImGuiInputTextFlags.ReadOnly);
        ImGui.popStyleColor();
    }

    private void renderTree(float scale) {
        float treeHeight = ImGui.getContentRegionAvailY() - 50f * scale;
        if (treeHeight < 80f * scale) treeHeight = 80f * scale;
        float paletteW = 110f * scale;

        // Palette panel
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.10f, 0.10f, 0.10f, 1f);
        if (ImGui.beginChild("##palette", paletteW, treeHeight, true)) {
            renderPalette();
        }
        ImGui.endChild();
        ImGui.popStyleColor();

        ImGui.sameLine();

        // Tree panel
        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.12f, 0.12f, 0.12f, 1f);
        if (ImGui.beginChild("##mask_tree", 0f, treeHeight, true)) {
            MaskNode root = editingMask.getRoot();
            if (root != null) {
                renderNode(root, null, -1, scale);
            }
        }
        ImGui.endChild();
        ImGui.popStyleColor();

        renderNodeToolbar(scale);
    }

    private static final String[] PALETTE_LOGIC = { "OR", "AND", "NOT", "OFFSET" };
    private static final String[] PALETTE_MASKS = { "Block", "Above", "Below", "Near", "Neighbour", "Adjacent", "Y",
        "Angle", "In Selection", "Can See Sky", "Surface" };

    private void renderPalette() {
        ImGui.textDisabled(I18n.format("dimensium.mask.editor.palette_logic"));
        for (String type : PALETTE_LOGIC) renderPaletteItem(type);
        ImGui.spacing();
        ImGui.textDisabled(I18n.format("dimensium.mask.editor.palette_masks"));
        for (String type : PALETTE_MASKS) renderPaletteItem(type);
    }

    private void renderPaletteItem(String type) {
        ImGui.selectable(type);
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload("MASK_PALETTE", type.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ImGui.text(type);
            ImGui.endDragDropSource();
        }
    }

    private MaskNode createNodeForType(byte[] payload) {
        String type = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        int airId = Block.getIdFromBlock(Blocks.air);
        return switch (type) {
            case "OR" -> new OrNode();
            case "AND" -> new AndNode();
            case "NOT" -> new NotNode();
            case "OFFSET" -> new OffsetNode(0, -1, 0);
            case "Block" -> new BlockMask(1, -1);
            case "Above" -> new AboveMask(airId, -1);
            case "Below" -> new BelowMask(airId, -1);
            case "Near" -> new NearMask(airId, -1, 3);
            case "Neighbour" -> new NeighbourMask(airId, -1);
            case "Adjacent" -> new AdjacentMask(airId, -1);
            case "Y" -> new YMask(YMask.Op.GREATER_EQ, 64);
            case "Angle" -> new AngleMask(0f, 10f);
            case "In Selection" -> new InSelectionMask();
            case "Can See Sky" -> new CanSeeSkyMask();
            case "Surface" -> new SurfaceMask();
            default -> null;
        };
    }

    private void renderNode(MaskNode node, List<MaskNode> parentList, int indexInParent, float scale) {
        ImGui.pushID(System.identityHashCode(node));

        if (!node.isLogic()) {
            renderLeafNode(node, parentList, indexInParent, scale);
        } else {
            renderLogicNode((LogicNode) node, parentList, indexInParent, scale);
        }

        ImGui.popID();
    }

    private void renderLeafNode(MaskNode node, List<MaskNode> parentList, int indexInParent, float scale) {
        boolean selected = selectedNode == node;
        if (selected) ImGui.pushStyleColor(ImGuiCol.Header, 0.2f, 0.5f, 0.2f, 1f);

        float rowH = ImGui.getFrameHeight();
        // Save row origin before selectable so we can overlay icon + text on top
        ImVec2 rowOrigin = ImGui.getCursorScreenPos();
        boolean clicked = ImGui.selectable("##leaf_sel", selected, 0, ImGui.getContentRegionAvailX(), rowH);

        // renderDragDrop MUST be immediately after the interactive item (selectable)
        renderDragDrop(node, parentList, indexInParent);
        renderLeafContextMenu(node, parentList, scale);

        if (selected) ImGui.popStyleColor();

        if (clicked) {
            selectedNode = node;
            // Block-based nodes: open block picker on click
            if (isBlockBased(node)) {
                int currentId = getBlockId(node);
                if (currentId == 0) {
                    OverlayRenderer.picker.openSelectAir(stack -> applyBlockPick(node, stack));
                } else {
                    OverlayRenderer.picker.open(stack -> applyBlockPick(node, stack), blockItemStack(node));
                }
            }
        }

        // Overlay icon + label on the selectable row via the draw list
        float pad = 4f * scale;
        float cx = rowOrigin.x + pad;
        float cy = rowOrigin.y;
        ItemStack iconStack = blockItemStack(node);
        if (iconStack != null) {
            float iconSize = rowH - 2f * scale;
            DeferredItemRender.schedule(iconStack, cx, cy + scale, iconSize);
            cx += iconSize + pad;
        }
        ImGui.getWindowDrawList()
            .addText(
                cx,
                cy + (rowH - ImGui.getTextLineHeight()) * 0.5f,
                ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                node.displayName());
    }

    private static boolean isBlockBased(MaskNode node) {
        return node instanceof BlockMask || node instanceof AboveMask
            || node instanceof BelowMask
            || node instanceof NearMask
            || node instanceof NeighbourMask
            || node instanceof AdjacentMask;
    }

    private static int getBlockId(MaskNode node) {
        if (node instanceof BlockMask) return ((BlockMask) node).blockId;
        if (node instanceof AboveMask) return ((AboveMask) node).blockId;
        if (node instanceof BelowMask) return ((BelowMask) node).blockId;
        if (node instanceof NearMask) return ((NearMask) node).blockId;
        if (node instanceof NeighbourMask) return ((NeighbourMask) node).blockId;
        if (node instanceof AdjacentMask) return ((AdjacentMask) node).blockId;
        return -1;
    }

    private static void applyBlockPick(MaskNode node, ItemStack stack) {
        // null stack = Air (blockId 0, meta 0)
        int blockId = 0;
        int meta = 0;
        if (stack != null) {
            Block block = Block.getBlockFromItem(stack.getItem());
            blockId = block != null ? Block.getIdFromBlock(block) : Item.getIdFromItem(stack.getItem());
            meta = stack.getItemDamage();
        }
        if (node instanceof BlockMask) {
            ((BlockMask) node).blockId = blockId;
            ((BlockMask) node).meta = meta;
        } else if (node instanceof AboveMask) {
            ((AboveMask) node).blockId = blockId;
            ((AboveMask) node).meta = meta;
        } else if (node instanceof BelowMask) {
            ((BelowMask) node).blockId = blockId;
            ((BelowMask) node).meta = meta;
        } else if (node instanceof NearMask) {
            ((NearMask) node).blockId = blockId;
            ((NearMask) node).meta = meta;
        } else if (node instanceof NeighbourMask) {
            ((NeighbourMask) node).blockId = blockId;
            ((NeighbourMask) node).meta = meta;
        } else if (node instanceof AdjacentMask) {
            ((AdjacentMask) node).blockId = blockId;
            ((AdjacentMask) node).meta = meta;
        }
        ToolMaskRegistry.INSTANCE.save();
    }

    /** Returns an ItemStack for the block referenced by block-based mask nodes, or null if not a block node. */
    private static ItemStack blockItemStack(MaskNode node) {
        int blockId = -1;
        int meta = 0;
        if (node instanceof BlockMask) {
            blockId = ((BlockMask) node).blockId;
            meta = Math.max(0, ((BlockMask) node).meta);
        } else if (node instanceof AboveMask) {
            blockId = ((AboveMask) node).blockId;
            meta = Math.max(0, ((AboveMask) node).meta);
        } else if (node instanceof BelowMask) {
            blockId = ((BelowMask) node).blockId;
            meta = Math.max(0, ((BelowMask) node).meta);
        } else if (node instanceof NearMask) {
            blockId = ((NearMask) node).blockId;
            meta = Math.max(0, ((NearMask) node).meta);
        } else if (node instanceof NeighbourMask) {
            blockId = ((NeighbourMask) node).blockId;
            meta = Math.max(0, ((NeighbourMask) node).meta);
        } else if (node instanceof AdjacentMask) {
            blockId = ((AdjacentMask) node).blockId;
            meta = Math.max(0, ((AdjacentMask) node).meta);
        }
        if (blockId < 0) return null;
        if (blockId == 0) return null; // Air: no icon, display name handles it
        Block block = Block.getBlockById(blockId);
        if (block == null || block == Blocks.air) return null;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return null;
        return new ItemStack(item, 1, meta);
    }

    private void renderLogicNode(LogicNode node, List<MaskNode> parentList, int indexInParent, float scale) {
        float leftPad = 14f * scale;
        float bracketX;
        float bracketY0;
        float bracketY1;

        // Capture top position before rendering
        ImVec2 topPos = ImGui.getCursorScreenPos();
        bracketX = topPos.x;
        bracketY0 = topPos.y;

        boolean selected = selectedNode == node;

        ImGui.indent(leftPad);

        // Node header button
        if (selected) ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.5f, 0.2f, 1f);
        if (ImGui.button(node.displayName() + "##hdr")) {
            selectedNode = node;
        }
        if (selected) ImGui.popStyleColor();

        renderDragDrop(node, parentList, indexInParent);
        renderLogicContextMenu(node, parentList);

        // Children
        for (int i = 0; i < node.children.size(); i++) {
            renderNode(node.children.get(i), node.children, i, scale);
        }

        // Drop target at end of children list
        renderChildDropTarget(node);

        ImGui.unindent(leftPad);

        ImVec2 bottomPos = ImGui.getCursorScreenPos();
        bracketY1 = bottomPos.y - 2f * scale;

        // Draw C-bracket using draw list
        ImDrawList dl = ImGui.getWindowDrawList();
        float bx = bracketX + 3f * scale;
        float bxRight = bracketX + leftPad - 2f * scale;
        float thick = 1.5f;
        int col = ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1f);
        dl.addLine(bx, bracketY0, bxRight, bracketY0, col, thick);
        dl.addLine(bx, bracketY0, bx, bracketY1, col, thick);
        dl.addLine(bx, bracketY1, bxRight, bracketY1, col, thick);
    }

    private void renderDragDrop(MaskNode node, List<MaskNode> parentList, int indexInParent) {
        if (parentList == null) return;

        if (ImGui.beginDragDropSource()) {
            dragNode = node;
            dragNodeParentList = parentList;
            dragNodeIndex = indexInParent;
            ImGui.setDragDropPayload("MASK_NODE", new byte[] { 1 });
            ImGui.text(node.displayName());
            ImGui.endDragDropSource();
        }

        if (ImGui.beginDragDropTarget()) {
            byte[] nodePayload = ImGui.acceptDragDropPayload("MASK_NODE");
            if (nodePayload != null && dragNode != null && dragNode != node) {
                applyDrop(parentList, indexInParent);
            }
            byte[] palPayload = ImGui.acceptDragDropPayload("MASK_PALETTE");
            if (palPayload != null) {
                MaskNode newNode = createNodeForType(palPayload);
                if (newNode != null) parentList.add(indexInParent, newNode);
            }
            ImGui.endDragDropTarget();
        }
    }

    private void renderChildDropTarget(LogicNode target) {
        // Invisible drop zone at bottom of children list
        ImGui.pushID("##drop_bottom_" + System.identityHashCode(target));
        ImGui.dummy(ImGui.getContentRegionAvailX(), 4f);
        if (ImGui.beginDragDropTarget()) {
            byte[] nodePayload = ImGui.acceptDragDropPayload("MASK_NODE");
            if (nodePayload != null && dragNode != null && dragNode != target) {
                if (dragNodeParentList != null) dragNodeParentList.remove(dragNode);
                target.children.add(dragNode);
                dragNode = null;
            }
            byte[] palPayload = ImGui.acceptDragDropPayload("MASK_PALETTE");
            if (palPayload != null) {
                MaskNode newNode = createNodeForType(palPayload);
                if (newNode != null) target.children.add(newNode);
            }
            ImGui.endDragDropTarget();
        }
        ImGui.popID();
    }

    private void applyDrop(List<MaskNode> targetParent, int targetIndex) {
        if (dragNodeParentList == null) return;
        dragNodeParentList.remove(dragNode);
        int insertAt = targetIndex;
        if (targetParent == dragNodeParentList && insertAt > dragNodeIndex) insertAt--;
        if (insertAt < 0) insertAt = 0;
        if (insertAt > targetParent.size()) insertAt = targetParent.size();
        targetParent.add(insertAt, dragNode);
        dragNode = null;
    }

    private void renderLogicContextMenu(LogicNode node, List<MaskNode> parentList) {
        if (ImGui.beginPopupContextItem("##lctx")) {
            renderAddChildMenu(node);
            if (parentList != null) {
                ImGui.separator();
                if (ImGui.menuItem(I18n.format("dimensium.mask.editor.delete_node"))) {
                    parentList.remove(node);
                    if (selectedNode == node) selectedNode = null;
                }
            }
            ImGui.endPopup();
        }
    }

    private void renderLeafContextMenu(MaskNode node, List<MaskNode> parentList, float scale) {
        if (ImGui.beginPopupContextItem("##lctx")) {
            renderLeafEditor(node, scale);
            if (parentList != null) {
                ImGui.separator();
                if (ImGui.menuItem(I18n.format("dimensium.mask.editor.delete_node"))) {
                    parentList.remove(node);
                    if (selectedNode == node) selectedNode = null;
                }
            }
            ImGui.endPopup();
        }
    }

    private void renderAddChildMenu(LogicNode parent) {
        if (ImGui.beginMenu(I18n.format("dimensium.mask.editor.add_logic"))) {
            if (ImGui.menuItem("OR")) parent.children.add(new OrNode());
            if (ImGui.menuItem("AND")) parent.children.add(new AndNode());
            if (ImGui.menuItem("NOT")) parent.children.add(new NotNode());
            if (ImGui.menuItem("OFFSET")) parent.children.add(new OffsetNode(0, -1, 0));
            ImGui.endMenu();
        }
        if (ImGui.beginMenu(I18n.format("dimensium.mask.editor.add_mask"))) {
            int airId = Block.getIdFromBlock(Blocks.air);
            if (ImGui.menuItem("Block")) parent.children.add(new BlockMask(1, -1));
            if (ImGui.menuItem("Above")) parent.children.add(new AboveMask(airId, -1));
            if (ImGui.menuItem("Below")) parent.children.add(new BelowMask(airId, -1));
            if (ImGui.menuItem("Near")) parent.children.add(new NearMask(airId, -1, 3));
            if (ImGui.menuItem("Neighbour")) parent.children.add(new NeighbourMask(airId, -1));
            if (ImGui.menuItem("Adjacent")) parent.children.add(new AdjacentMask(airId, -1));
            if (ImGui.menuItem("Y")) parent.children.add(new YMask(YMask.Op.GREATER_EQ, 64));
            if (ImGui.menuItem("Angle")) parent.children.add(new AngleMask(0f, 10f));
            if (ImGui.menuItem("In Selection")) parent.children.add(new InSelectionMask());
            if (ImGui.menuItem("Can See Sky")) parent.children.add(new CanSeeSkyMask());
            if (ImGui.menuItem("Surface")) parent.children.add(new SurfaceMask());
            ImGui.endMenu();
        }
    }

    private void renderLeafEditor(MaskNode node, float scale) {
        float w = 120f * scale;
        if (node instanceof BlockMask) {
            renderBlockIdEditor(((BlockMask) node), w);
        } else if (node instanceof AboveMask) {
            renderBlockIdEditor2(((AboveMask) node), w);
        } else if (node instanceof BelowMask) {
            renderBlockIdEditor3(((BelowMask) node), w);
        } else if (node instanceof NearMask) {
            renderNearEditor(((NearMask) node), w);
        } else if (node instanceof NeighbourMask) {
            renderNeighbourEditor(((NeighbourMask) node), w);
        } else if (node instanceof AdjacentMask) {
            renderAdjacentEditor(((AdjacentMask) node), w);
        } else if (node instanceof YMask) {
            renderYEditor(((YMask) node), w);
        } else if (node instanceof AngleMask) {
            renderAngleEditor(((AngleMask) node), w);
        } else if (node instanceof OffsetNode) {
            renderOffsetEditor(((OffsetNode) node), w);
        }
    }

    private void renderBlockIdEditor(BlockMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##b", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##b", meta)) m.meta = Math.max(-1, meta.get());
    }

    private void renderBlockIdEditor2(AboveMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##a", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##a", meta)) m.meta = Math.max(-1, meta.get());
    }

    private void renderBlockIdEditor3(BelowMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##bw", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##bw", meta)) m.meta = Math.max(-1, meta.get());
    }

    private void renderNearEditor(NearMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##n", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##n", meta)) m.meta = Math.max(-1, meta.get());
        ImInt r = new ImInt(m.radius);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Radius##n", r)) m.radius = Math.max(1, r.get());
    }

    private void renderNeighbourEditor(NeighbourMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##nb", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##nb", meta)) m.meta = Math.max(-1, meta.get());
    }

    private void renderAdjacentEditor(AdjacentMask m, float w) {
        ImInt id = new ImInt(m.blockId);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("ID##adj", id)) m.blockId = Math.max(0, id.get());
        ImInt meta = new ImInt(m.meta);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("Meta (-1=any)##adj", meta)) m.meta = Math.max(-1, meta.get());
    }

    private void renderYEditor(YMask m, float w) {
        String opLabel = m.displayName()
            .split(" ")[1];
        if (ImGui.button(opLabel + "##yop")) {
            m.op = m.nextOp();
        }
        ImGui.sameLine();
        ImInt v = new ImInt(m.value);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("##yval", v)) m.value = v.get();
    }

    private void renderAngleEditor(AngleMask m, float w) {
        float[] angle = { m.angle };
        ImGui.setNextItemWidth(w);
        if (ImGui.sliderFloat("Angle##ang", angle, 0f, 90f)) m.angle = angle[0];
        float[] range = { m.range };
        ImGui.setNextItemWidth(w);
        if (ImGui.sliderFloat("Range##ang", range, 0f, 45f)) m.range = range[0];
    }

    private void renderOffsetEditor(OffsetNode m, float w) {
        ImInt dx = new ImInt(m.dx), dy = new ImInt(m.dy), dz = new ImInt(m.dz);
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("dX##off", dx)) m.dx = dx.get();
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("dY##off", dy)) m.dy = dy.get();
        ImGui.setNextItemWidth(w);
        if (ImGui.inputInt("dZ##off", dz)) m.dz = dz.get();
    }

    private void renderNodeToolbar(float scale) {
        if (editingMask == null) return;

        if (ImGui.button(I18n.format("dimensium.mask.editor.clear_tree"))) {
            editingMask.setRoot(new AndNode());
            selectedNode = null;
        }

        // Inline editor for selected non-block leaf nodes
        if (selectedNode != null && !selectedNode.isLogic() && !isBlockBased(selectedNode)) {
            ImGui.separator();
            ImGui.textDisabled(selectedNode.displayName());
            renderLeafEditor(selectedNode, scale);
        } else if (selectedNode != null && selectedNode.isLogic()) {
            ImGui.sameLine();
            ImGui.textDisabled("| " + selectedNode.displayName());
        }
    }
}
