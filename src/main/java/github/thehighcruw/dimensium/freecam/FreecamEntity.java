package github.thehighcruw.dimensium.freecam;

import net.minecraft.entity.EntityLivingBase;
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
    protected void entityInit() {
        super.entityInit();
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
    public net.minecraft.item.ItemStack[] getLastActiveItems() {
        return new net.minecraft.item.ItemStack[0];
    }

    @Override
    public void setCurrentItemOrArmor(int slot, net.minecraft.item.ItemStack stack) {}

    @Override
    public net.minecraft.item.ItemStack getEquipmentInSlot(int slot) {
        return null;
    }

    @Override
    public net.minecraft.item.ItemStack getHeldItem() {
        return null;
    }
}
