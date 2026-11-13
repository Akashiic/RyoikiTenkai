package com.akashic.ryoiki.mixin.mixins;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.akashic.ryoiki.client.SorcererClient;

/**
 * The first-person hand of a player in the sorcerer mode is bare. Each tick the game compares the item held with the
 * one the hand shows, and lowers the hand to change it: in the mode the item held counts as none, so the hand puts
 * down what it had and comes up empty, and does the reverse on the way out. The inventory is not touched.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererModeMixin {

    @ModifyVariable(method = "updateEquippedItem", at = @At("STORE"), ordinal = 0)
    private ItemStack ryoiki$bareHand(ItemStack held) {
        return SorcererClient.firstPersonItem(held);
    }
}
