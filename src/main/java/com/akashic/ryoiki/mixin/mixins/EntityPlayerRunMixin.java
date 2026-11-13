package com.akashic.ryoiki.mixin.mixins;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashic.ryoiki.client.ClashInput;

/**
 * The two casters of a clash run whichever way they go. The game ends a run that is not forward, at the start of
 * the player's tick, and reads its speed again when it moves it: so the run is put back between the two, here,
 * where the player's own part of the tick begins.
 */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerRunMixin {

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void ryoiki$run(CallbackInfo ci) {
        ClashInput.beforeMove((EntityPlayer) (Object) this);
    }
}
