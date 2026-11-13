package com.akashic.ryoiki.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * How this player fights while it is one of the two casters of a clash. Nobody else, and at no other time.
 *
 * <p>
 * A click is a blow of the chain and nothing else. The game's own click would do three things that break a chain:
 * strike the other its own way (and, running, knock it back, slow the striker and stop its run, on this client
 * before the server can say no), start breaking the block in the crosshair, and swing again and again while the
 * button is held on a block. So the click is taken before the game sees it, and answered with one swing of the arm,
 * which is what the server reads as a blow.
 *
 * <p>
 * And the two run, always and in any direction: ready to strike, never strolling. The game stops a run that is not
 * forward; each tick, after it has and before it moves the player, the run is put back
 * ({@code EntityPlayerRunMixin}).
 */
public final class ClashInput {

    public static final ClashInput INSTANCE = new ClashInput();

    private ClashInput() {}

    /** Whether this client's player is fighting a clash now. */
    public static boolean fighting() {
        Minecraft mc = Minecraft.getMinecraft();
        ClientDomain domain = ClientDomain.INSTANCE;
        if (mc.thePlayer == null || !domain.fused()) {
            return false;
        }
        int id = mc.thePlayer.getEntityId();
        return domain.clashEntities[0] == id || domain.clashEntities[1] == id;
    }

    /** One click: one swing of the arm. The server sees the swing begin and asks the chain for a blow. */
    public static void blow() {
        Minecraft.getMinecraft().thePlayer.swingItem();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        // The attack is a mouse button unless the player bound it elsewhere: buttons are key codes from -100 up.
        int attack = mc.gameSettings.keyBindAttack.getKeyCode() + 100;
        if (event.button >= 0 && ClientDomain.INSTANCE.frozen()) {
            // Held by the void: no click does anything.
            event.setCanceled(true);
            return;
        }
        if (event.button < 0 || event.button != attack || !fighting()) {
            return;
        }
        event.setCanceled(true);
        if (event.buttonstate) {
            blow();
        }
    }

    /** With the attack on a key of the keyboard the game's click gets through: at least it strikes nobody. */
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer == Minecraft.getMinecraft().thePlayer && fighting()) {
            event.setCanceled(true);
        }
    }

    /**
     * Before the game moves a player this tick (and after it has decided whether the player still runs): this
     * client's player, fighting, runs.
     */
    public static void beforeMove(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().thePlayer && ClientDomain.INSTANCE.frozen()) {
            // Held by the void: no step, no jump, whatever keys are down. (The server holds it too.)
            net.minecraft.client.entity.EntityPlayerSP self = (net.minecraft.client.entity.EntityPlayerSP) player;
            self.movementInput.moveForward = 0;
            self.movementInput.moveStrafe = 0;
            self.movementInput.jump = false;
            self.moveForward = 0;
            self.moveStrafing = 0;
            self.setJumping(false);
            self.motionX = 0;
            self.motionZ = 0;
            self.setSprinting(false);
            return;
        }
        if (player == Minecraft.getMinecraft().thePlayer && AbilityFx.INSTANCE.hangs(player.getEntityId())) {
            // Hanging in the air by the orb it guides: it turns its eyes, and nothing else. (The server holds it too.)
            net.minecraft.client.entity.EntityPlayerSP self = (net.minecraft.client.entity.EntityPlayerSP) player;
            self.movementInput.moveForward = 0;
            self.movementInput.moveStrafe = 0;
            self.movementInput.jump = false;
            self.moveForward = 0;
            self.moveStrafing = 0;
            self.setJumping(false);
            self.motionX = 0;
            self.motionY = 0;
            self.motionZ = 0;
            return;
        }
        if (player == Minecraft.getMinecraft().thePlayer && !player.isSprinting() && fighting()) {
            player.setSprinting(true);
        }
    }
}
