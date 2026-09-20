/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Injects {@link ViewportTileHook#afterProjectionSetup()} immediately after every
 * {@code Project.gluPerspective} call in {@code EntityRenderer.setupCameraTransform}.
 *
 * The hook captures the untiled projection tangents for raycasting and then replaces
 * the projection matrix with an off-axis tile transform so that the full framebuffer
 * covers only the viewport panel's angular extent — giving supersampled quality.
 */
public class EntityRendererTransformer implements IClassTransformer {

    private static final String ENTITY_RENDERER_CLASS = "net.minecraft.client.renderer.EntityRenderer";
    private static final String SETUP_CAMERA_TRANSFORM_MCP = "setupCameraTransform";
    private static final String SETUP_CAMERA_TRANSFORM_OBF = "a";
    private static final String SETUP_CAMERA_TRANSFORM_DESC = "(FI)V";

    private static final String GLU_PROJECT_OWNER = "org/lwjgl/util/glu/Project";
    private static final String GLU_PERSPECTIVE_NAME = "gluPerspective";
    private static final String GLU_PERSPECTIVE_DESC = "(FFFF)V";

    private static final String HOOK_OWNER = "github/thehighcruw/dimensium/asm/ViewportTileHook";
    private static final String HOOK_METHOD = "afterProjectionSetup";
    private static final String HOOK_DESC = "()V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if (!transformedName.equals(ENTITY_RENDERER_CLASS)) return bytes;

        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);

        boolean patched = false;
        for (MethodNode method : classNode.methods) {
            if (!method.desc.equals(SETUP_CAMERA_TRANSFORM_DESC)) continue;
            if (!method.name.equals(SETUP_CAMERA_TRANSFORM_MCP) && !method.name.equals(SETUP_CAMERA_TRANSFORM_OBF))
                continue;

            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() != Opcodes.INVOKESTATIC) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.owner.equals(GLU_PROJECT_OWNER)
                        || !call.name.equals(GLU_PERSPECTIVE_NAME)
                        || !call.desc.equals(GLU_PERSPECTIVE_DESC)) continue;

                method.instructions.insert(
                        insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_METHOD, HOOK_DESC, false));
                patched = true;
                break;
            }

            if (patched) break;
        }

        if (!patched) return bytes;

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
