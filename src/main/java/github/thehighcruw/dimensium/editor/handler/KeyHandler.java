/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import github.thehighcruw.dimensium.Dimensium;
import github.thehighcruw.dimensium.DimensiumEditorMode;
import github.thehighcruw.dimensium.editor.freecam.FreecamState;
import github.thehighcruw.dimensium.editor.overlay.EditingModeScreen;
import github.thehighcruw.dimensium.editor.overlay.GuiDimensiumOverlay;
import github.thehighcruw.dimensium.editor.overlay.OverlayRenderer;
import github.thehighcruw.dimensium.editor.overlay.ViewState;
import github.thehighcruw.dimensium.editor.tool.ActiveDragState;
import github.thehighcruw.dimensium.editor.tool.Tool;
import github.thehighcruw.dimensium.editor.tool.creating.modelling.ModellingToolState;
import github.thehighcruw.dimensium.editor.tool.creating.rock.PathToolState;
import github.thehighcruw.dimensium.editor.tool.creating.shape.ShapePlacementState;
import github.thehighcruw.dimensium.editor.tool.manipulating.move.MoveToolState;
import github.thehighcruw.dimensium.editor.tool.selecting.SelectedBlockState;
import github.thehighcruw.dimensium.editor.tool.selecting.box.BoxSelectToolState;
import github.thehighcruw.dimensium.editor.tool.state.ClipboardPlacementState;
import github.thehighcruw.dimensium.editor.window.imgui.ImGuiManager;
import github.thehighcruw.dimensium.editor.window.popup.BlueprintBrowserPopup;
import github.thehighcruw.dimensium.editor.window.popup.ConflictPopup;
import github.thehighcruw.dimensium.editor.window.popup.CreateBlueprintPopup;
import github.thehighcruw.dimensium.editor.window.popup.SettingsModal;
import github.thehighcruw.dimensium.shared.BlockSender;
import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import github.thehighcruw.dimensium.shared.util.RenderUtils;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.tool.BuilderToolState.Phase;
import github.thehighcruw.dimensium.tool.ChangeProposal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        int key = Keyboard.getEventKey();
        boolean down = Keyboard.getEventKeyState();
        char ch = Keyboard.getEventCharacter();

        // Feed all key events to ImGui before any early-return.
        ImGuiManager.INSTANCE.addKeyEvent(key, down);
        if (down && ch >= 32 && ch != 127) {
            ImGuiManager.INSTANCE.addChar(ch);
        }

        if (down && SettingsModal.INSTANCE.captureKeybind(key)) return;

        // ImGui owns the keyboard when any modal/popup is focused — don't process game keys.
        if (ImGuiManager.INSTANCE.wantCaptureKeyboard()) return;

        if (!down) return;

        // Log key presses for the "Show Key Presses" overlay.
        if (DimensiumEditorMode.INSTANCE.isActive() && ViewState.INSTANCE.showKeyPresses) {
            StringBuilder keyLabel = new StringBuilder();
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
                keyLabel.append("Ctrl+");
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
                keyLabel.append("Shift+");
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU))
                keyLabel.append("Alt+");
            keyLabel.append(Keyboard.getKeyName(key));
            ViewState.INSTANCE.logKey(keyLabel.toString());
        }

        Minecraft mc = Minecraft.getMinecraft();

        // ── Toggle overlay (RShift) — requires creative mode ─────────────────
        if (key == Dimensium.toggleDimensium.getKeyCode()
                && !OverlayRenderer.picker.isOpen()
                && !CreateBlueprintPopup.INSTANCE.isOpen()
                && !BlueprintBrowserPopup.INSTANCE.isOpen()) {
            if (OverlayRenderer.isNotCreative()) return;
            OverlayRenderer.picker.close();
            if (DimensiumEditorMode.INSTANCE.isActive()) {
                FreecamState.INSTANCE.deactivate();
                ShapePlacementState.INSTANCE.cancel();
                ClipboardPlacementState.INSTANCE.cancel();
                DimensiumEditorMode.INSTANCE.toggle();
                if (mc.thePlayer != null) mc.thePlayer.setInvisible(false);
                mc.displayGuiScreen(null);
            } else {
                DimensiumEditorMode.INSTANCE.toggle();
                FreecamState.INSTANCE.activate();
                if (mc.thePlayer != null) mc.thePlayer.setInvisible(true);
                mc.displayGuiScreen(new EditingModeScreen());
            }
            return;
        }

        // Popups use ImGui for keyboard input; wantCaptureKeyboard() above handles blocking.

        // ── Overlay-only bindings ─────────────────────────────────────────────
        if (DimensiumEditorMode.INSTANCE.isActive()) {
            int mods = currentMods();

            // Confirm — box selection gizmo phase, shape placement, clipboard paste, path, or modelling.
            if (matches(key, mods, Dimensium.actionConfirm, Dimensium.actionConfirmMods)
                    || key == Keyboard.KEY_NUMPADENTER) {
                SelectionState bxConfSel = SelectionState.INSTANCE;
                if (bxConfSel.boxConfirmed && DimensiumEditorMode.INSTANCE.selectedTool == Tool.SELECT) {
                    GuiDimensiumOverlay.commitBoxSelection(bxConfSel, BoxSelectToolState.INSTANCE);
                    return;
                }
                if (ShapePlacementState.INSTANCE.active) {
                    GuiDimensiumOverlay.confirmPlacement();
                    return;
                }
                if (ClipboardPlacementState.INSTANCE.active) {
                    GuiDimensiumOverlay.confirmClipboardPlacement();
                    return;
                }
                if (DimensiumEditorMode.INSTANCE.selectedTool == Tool.PATH) {
                    GuiDimensiumOverlay.applyPath();
                    return;
                }
                if (DimensiumEditorMode.INSTANCE.selectedTool == Tool.MODELLING) {
                    GuiDimensiumOverlay.applyModelling();
                    return;
                }
            }

            // Escape — close conflict popup first if open.
            if (key == Keyboard.KEY_ESCAPE && ConflictPopup.INSTANCE.isOpen()) {
                ConflictPopup.INSTANCE.close();
                return;
            }

            // Escape — cancel any active proposal, then placement (keep overlay open).
            if (key == Keyboard.KEY_ESCAPE) {
                if (ActiveDragState.INSTANCE.activeDrag != null) {
                    ChangeProposal.cancel();
                    return;
                }
                if (BuilderToolState.INSTANCE.fillPreview != null) {
                    BuilderToolState.INSTANCE.fillPreview = null;
                    return;
                }
                if (ShapePlacementState.INSTANCE.active) {
                    ShapePlacementState.INSTANCE.cancel();
                    return;
                }
                if (ClipboardPlacementState.INSTANCE.active) {
                    ClipboardPlacementState.INSTANCE.cancel();
                    return;
                }
                DimensiumEditorMode.INSTANCE.selectedTool = Tool.POINTER;
                return;
            }

            // Undo.
            if (matches(key, mods, Dimensium.actionUndo, Dimensium.actionUndoMods)) {
                EditorActions.undo();
                return;
            }

            // Redo.
            if (matches(key, mods, Dimensium.actionRedo, Dimensium.actionRedoMods)) {
                EditorActions.redo();
                return;
            }

            // Tool shortcuts.
            Tool switched = toolForKey(key, mods);
            if (switched != null) {
                DimensiumEditorMode.INSTANCE.selectedTool = switched;
                return;
            }

            SelectionState sel = SelectionState.INSTANCE;

            // Copy.
            if (matches(key, mods, Dimensium.actionCopy, Dimensium.actionCopyMods)) {
                EditorActions.copy();
                return;
            }

            // Cut.
            if (matches(key, mods, Dimensium.actionCut, Dimensium.actionCutMods)) {
                EditorActions.cut();
                return;
            }

            // Erase — also accepts Backspace as an alias.
            if (matches(key, mods, Dimensium.actionErase, Dimensium.actionEraseMods) || key == Keyboard.KEY_BACK) {
                PathToolState pts = PathToolState.INSTANCE;
                if (DimensiumEditorMode.INSTANCE.selectedTool == Tool.PATH
                        && pts.selectedIndex >= 0
                        && !pts.points.isEmpty()) {
                    pts.removeCurrentPoint();
                    return;
                }
                ModellingToolState mts = ModellingToolState.INSTANCE;
                if (DimensiumEditorMode.INSTANCE.selectedTool == Tool.MODELLING && mts.selectedPointObj() != null) {
                    mts.removeSelectedPoint();
                    return;
                }
                if (sel.hasSelection()) {
                    BlockSender.sendChunked(SelectionOps.selectionToAirOps(sel), I18n.format("dimensium.action.erase"));
                    sel.clearSelection();
                }
                return;
            }

            // Paste.
            if (matches(key, mods, Dimensium.actionPaste, Dimensium.actionPasteMods)) {
                ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
                if (cps.active) {
                    GuiDimensiumOverlay.confirmClipboardPlacement();
                } else if (sel.clipboard != null) {
                    MovingObjectPosition mop = RenderUtils.raycastAtCursor();
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                        cps.start(sel, Vec3DInt.from(mop.blockX, mop.blockY, mop.blockZ));
                    }
                }
                return;
            }

            // Save blueprint.
            if (matches(key, mods, Dimensium.actionSaveBlueprint, Dimensium.actionSaveBlueprintMods)) {
                EditorActions.saveBlueprint();
                return;
            }

            // Blueprint browser.
            if (matches(key, mods, Dimensium.actionBlueprintBrowser, Dimensium.actionBlueprintBrowserMods)) {
                BlueprintBrowserPopup.INSTANCE.open();
                return;
            }

            // Open settings.
            if (matches(key, mods, Dimensium.actionSettings, Dimensium.actionSettingsMods)) {
                SettingsModal.INSTANCE.toggle();
                return;
            }

            // Gizmo nudge.
            if (handleGizmoNudge(key, mods)) return;

            // Fill.
            if (matches(key, mods, Dimensium.actionFill, Dimensium.actionFillMods)) {
                if (sel.hasSelection()) {
                    Block paint = SelectedBlockState.INSTANCE.getPaintBlock();
                    int meta = SelectedBlockState.INSTANCE.getPaintMeta();
                    int id = Block.getIdFromBlock(paint);
                    List<int[]> ops = new ArrayList<>(sel.size());
                    for (long packed : sel.getSelectedBlocks()) {
                        ops.add(new int[] {
                            SelectionState.unpack(packed).x(),
                            SelectionState.unpack(packed).y(),
                            SelectionState.unpack(packed).z(),
                            id,
                            meta
                        });
                    }
                    BlockSender.sendChunked(ops, I18n.format("dimensium.action.fill"));
                }
                return;
            }
        }

        if (!DimensiumEditorMode.INSTANCE.isBuilderToolsActive()) return;

        // ── Number keys 1-9: exit builder tools, switch slot ─────────────────
        if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_9) {
            DimensiumEditorMode.INSTANCE.exitBuilderTools();
            return;
        }

        // ── Escape: cancel current phase or exit mode ─────────────────────────
        if (key == Keyboard.KEY_ESCAPE) {
            BuilderToolState bts = BuilderToolState.INSTANCE;
            if (bts.phase != Phase.IDLE) {
                bts.resetPhase();
                SelectionState.INSTANCE.clearSelection();
            } else {
                if (mc.thePlayer != null) mc.thePlayer.inventory.currentItem = 8;
                DimensiumEditorMode.INSTANCE.exitBuilderTools();
            }
        }
    }

    private Tool toolForKey(int key, int mods) {
        for (Map.Entry<Tool, KeyBinding> entry : Dimensium.toolKeybinds.entrySet()) {
            int requiredMods = Dimensium.toolKeybindMods.getOrDefault(entry.getKey(), 0);
            if (matches(key, mods, entry.getValue(), requiredMods)) return entry.getKey();
        }
        return null;
    }

    private static int currentMods() {
        return InputState.currentMods();
    }

    private static boolean matches(int key, int mods, KeyBinding binding, int requiredMods) {
        return key == binding.getKeyCode() && mods == requiredMods;
    }

    /**
     * Nudges the active translation gizmo by 1 block if a nudge key is pressed.
     * Direction is relative to the freecam's horizontal facing for XZ nudges.
     * Returns true if a nudge key was consumed.
     */
    private boolean handleGizmoNudge(int key, int mods) {
        int[] delta = nudgeDelta(key, mods);
        if (delta == null) return false;

        Tool tool = DimensiumEditorMode.INSTANCE.selectedTool;

        if (tool == Tool.SHAPE && ShapePlacementState.INSTANCE.active) {
            ShapePlacementState sps = ShapePlacementState.INSTANCE;
            sps.anchor = sps.anchor.plus(Vec3DInt.from(delta[0], delta[1], delta[2]));
            sps.anchorF = Vec3DFloat.from(sps.anchor.x(), sps.anchor.y(), sps.anchor.z());
            sps.invalidateGhost();
            sps.rebuildIfNeeded();
            return true;
        }

        if (ClipboardPlacementState.INSTANCE.active) {
            ClipboardPlacementState cps = ClipboardPlacementState.INSTANCE;
            cps.anchor = cps.anchor.plus(Vec3DInt.from(delta[0], delta[1], delta[2]));
            cps.anchorF = Vec3DFloat.from(cps.anchor.x(), cps.anchor.y(), cps.anchor.z());
            cps.rebuildPreview();
            return true;
        }

        if (tool == Tool.MOVE && MoveToolState.INSTANCE.active) {
            MoveToolState mts = MoveToolState.INSTANCE;
            mts.delta = mts.delta.plus(Vec3DFloat.from(delta[0], delta[1], delta[2]));
            mts.invalidateGhost();
            mts.rebuildIfNeeded();
            return true;
        }

        if (tool == Tool.PATH) {
            PathToolState pts = PathToolState.INSTANCE;
            PathToolState.PathPoint pt = pts.selectedPoint();
            if (pt != null) {
                pt.pos = pt.pos.plus(Vec3DInt.from(delta[0], delta[1], delta[2]));
                pts.getAxisTranslationGizmo().reset();
                pts.invalidatePath();
                return true;
            }
        }

        if (tool == Tool.MODELLING) {
            ModellingToolState modts = ModellingToolState.INSTANCE;
            ModellingToolState.ModelPoint pt = modts.selectedPointObj();
            if (pt != null) {
                modts.rows
                        .get(modts.selectedRow)
                        .set(
                                modts.selectedPoint,
                                new ModellingToolState.ModelPoint(
                                        pt.pos().plus(Vec3DInt.from(delta[0], delta[1], delta[2]))));
                modts.getAxisTranslationGizmo().reset();
                modts.invalidate();
                return true;
            }
        }

        return false;
    }

    /**
     * Returns [dx, dy, dz] for the nudge key, or null if key is not a nudge key.
     * XZ directions are snapped to the cardinal axis closest to the camera's horizontal facing.
     */
    private static int[] nudgeDelta(int key, int mods) {
        boolean fwd = matches(key, mods, Dimensium.gizmoNudgeForward, Dimensium.gizmoNudgeForwardMods);
        boolean bwd = matches(key, mods, Dimensium.gizmoNudgeBackward, Dimensium.gizmoNudgeBackwardMods);
        boolean rgt = matches(key, mods, Dimensium.gizmoNudgeRight, Dimensium.gizmoNudgeRightMods);
        boolean lft = matches(key, mods, Dimensium.gizmoNudgeLeft, Dimensium.gizmoNudgeLeftMods);
        boolean up = matches(key, mods, Dimensium.gizmoNudgeUp, Dimensium.gizmoNudgeUpMods);
        boolean dwn = matches(key, mods, Dimensium.gizmoNudgeDown, Dimensium.gizmoNudgeDownMods);

        if (!fwd && !bwd && !rgt && !lft && !up && !dwn) return null;

        if (up) return new int[] {0, 1, 0};
        if (dwn) return new int[] {0, -1, 0};

        Entity cam = FreecamState.INSTANCE.cameraEntity;
        float yaw = cam != null ? cam.rotationYaw : 0f;
        // Snap yaw to nearest 90°: 0=south(+Z), 1=west(-X), 2=north(-Z), 3=east(+X)
        int q = Math.round(yaw / 90f) & 3;
        // Cardinal forward vectors per quadrant
        int[] qfx = {0, -1, 0, 1};
        int[] qfz = {1, 0, -1, 0};
        // Cardinal right vectors: right = direction faced after turning right (yaw+90).
        // Formula: (-cos(yaw), -sin(yaw)) in (x,z). Per quadrant:
        // south→west, west→north, north→east, east→south
        int[] qrx = {-1, 0, 1, 0};
        int[] qrz = {0, -1, 0, 1};

        // Flip Canvas mirrors the horizontal screen axis, inverting the effective L/R direction.
        int lrSign = ViewState.INSTANCE.flipCanvas ? -1 : 1;

        if (fwd) return new int[] {qfx[q], 0, qfz[q]};
        if (bwd) return new int[] {-qfx[q], 0, -qfz[q]};
        if (rgt) return new int[] {lrSign * qrx[q], 0, lrSign * qrz[q]};
        return new int[] {-lrSign * qrx[q], 0, -lrSign * qrz[q]};
    }
}
