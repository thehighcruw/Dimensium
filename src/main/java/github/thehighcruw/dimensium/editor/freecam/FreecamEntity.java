/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.freecam;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class FreecamEntity extends EntityLivingBase {

    public FreecamEntity(World world) {
        super(world);
        noClip = true;
        isImmuneToFire = true;
        ignoreFrustumCheck = true;

        // MC setupCameraTransform adds yOffset to the eye position. Setting 1.62 aligns
        // the rendered camera with raycastFromMouse which uses entity.posY + getEyeHeight().
        yOffset = 1.62F;
        posY = -1.62F;
    }

    // Entity is positioned at the exact camera point; adding eye height would double-offset it.
    @Override
    public float getEyeHeight() {
        return 0F;
    }

    @Override
    public boolean isEntityAlive() {
        return true;
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {}

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {}

    @Override
    public void onUpdate() {}

    // EntityLivingBase requires these abstract methods.
    @Override
    public int getTotalArmorValue() {
        return 0;
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return new ItemStack[0];
    }

    @Override
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

    @Override
    public ItemStack getEquipmentInSlot(int slot) {
        return null;
    }

    @Override
    public ItemStack getHeldItem() {
        return null;
    }
}
