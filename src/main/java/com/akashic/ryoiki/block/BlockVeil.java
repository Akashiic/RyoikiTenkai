package com.akashic.ryoiki.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * What a domain arena is made of: a solid block nobody sees or breaks. The client draws nothing for it (so an arena's
 * chunks have no mesh to build, and entering one costs no frame); the domain's look is drawn over it by
 * {@code client.DomainRenderer}. Meta {@link #FLOOR} gives light, so players inside are lit the same by day and by
 * night.
 */
public final class BlockVeil extends Block {

    public static final int SHELL = 0;
    public static final int FLOOR = 1;

    public BlockVeil() {
        super(Material.glass);
        setBlockName("ryoikitenkai.veu");
        setBlockTextureName("ryoikitenkai:veu");
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setStepSound(soundTypeCloth);
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) == FLOOR ? 15 : 0;
    }

    @Override
    public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
        return 0;
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /** Nothing to pick: creative's middle click and tooltips such as Waila's see no block here. */
    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int getMobilityFlag() {
        return 2; // pistons do not move it
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getAmbientOcclusionLightValue() {
        return 1.0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        // not in any creative tab
    }
}
