package com.akashic.ryoiki.mixin.mixins;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashic.ryoiki.client.ClientDomain;
import com.akashic.ryoiki.net.DomainWire;

/**
 * The client's end of the domain channel, and the chunks it keeps while its player is in a domain: the server
 * unloads the terrain the player left (a chunk packet with no sections), and the client ignores that, so the terrain
 * is still there on the way back.
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientDomainMixin {

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void ryoiki$payload(S3FPacketCustomPayload packet, CallbackInfo ci) {
        if (DomainWire.CHANNEL.equals(packet.func_149169_c())) {
            ClientDomain.INSTANCE.onPayload(packet.func_149168_d());
            ci.cancel();
        }
    }

    @Inject(method = "handleChunkData", at = @At("HEAD"), cancellable = true)
    private void ryoiki$keepChunk(S21PacketChunkData packet, CallbackInfo ci) {
        // func_149274_i: whole chunk; func_149276_g: the sections sent. Whole and none is an unload.
        if (packet.func_149274_i() && packet.func_149276_g() == 0
            && ClientDomain.INSTANCE.keep.keeps(packet.func_149273_e(), packet.func_149271_f())) {
            ci.cancel();
        }
    }
}
