package com.akashic.ryoiki.mode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.DomainType;

/**
 * The techniques of the sorcerer mode, on the server: what each does to the world, tick by tick.
 *
 * <p>
 * A technique touches only <b>other players who are in the mode too</b>, where the server lets players fight at
 * all. What it does to them goes through the game's own damage, as a blow from the one who used it: whatever
 * guards a place or a player against another player's blows (a protection plugin, a team) guards against this, and
 * someone so guarded is not moved either. The damage is small and the same whatever the armour: the mode's fights
 * are not decided by equipment.
 */
final class Abilities {

    /** How far around a technique players are told of it. */
    private static final double SEEN = 64;

    private final List<Pull> pulls = new ArrayList<>();
    private final List<Cut> cuts = new ArrayList<>();

    private static final class Pull {

        EntityPlayerMP caster;
        double x;
        double y;
        double z;
        int age;
        final Set<UUID> caught = new HashSet<>();
        final Set<UUID> guarded = new HashSet<>();
    }

    private static final class Cut {

        EntityPlayerMP caster;
        double x;
        double y;
        double z;
        double dx;
        double dy;
        double dz;
        double range;
        int age;
    }

    void reset() {
        pulls.clear();
        cuts.clear();
    }

    /**
     * A player uses the first technique of its domain's kind. Returns the ticks it now recharges for.
     */
    int first(EntityPlayerMP caster, DomainType technique, SorcererMode mode) {
        Vec3 look = caster.getLookVec();
        double ex = caster.posX;
        double ey = caster.posY + caster.getEyeHeight();
        double ez = caster.posZ;
        if (technique == DomainType.INFINITE_VOID) {
            // Atração: the point is where the caster aims, on the first thing solid in the way or at the end of
            // its reach.
            double reach = clear(caster.worldObj, ex, ey, ez, look, AbilityMath.PULL_AIM) - 0.3;
            Pull pull = new Pull();
            pull.caster = caster;
            pull.x = ex + look.xCoord * reach;
            pull.y = ey + look.yCoord * reach;
            pull.z = ez + look.zCoord * reach;
            pulls.add(pull);
            tell(
                caster.worldObj,
                pull.x,
                pull.y,
                pull.z,
                ModeWire.effect(
                    AbilityMath.PULL,
                    caster.getEntityId(),
                    pull.x,
                    pull.y,
                    pull.z,
                    look.xCoord,
                    look.yCoord,
                    look.zCoord,
                    AbilityMath.PULL_RADIUS,
                    AbilityMath.PULL_TICKS),
                mode);
            return AbilityMath.PULL_RECHARGE;
        }
        // Talho: the blade leaves from before the caster's chest and flies until something solid.
        Cut cut = new Cut();
        cut.caster = caster;
        cut.dx = look.xCoord;
        cut.dy = look.yCoord;
        cut.dz = look.zCoord;
        cut.x = ex + look.xCoord * AbilityMath.CUT_AHEAD;
        cut.y = ey - AbilityMath.CUT_BELOW + look.yCoord * AbilityMath.CUT_AHEAD;
        cut.z = ez + look.zCoord * AbilityMath.CUT_AHEAD;
        cut.range = clear(caster.worldObj, cut.x, cut.y, cut.z, look, AbilityMath.CUT_RANGE);
        cuts.add(cut);
        tell(
            caster.worldObj,
            cut.x,
            cut.y,
            cut.z,
            ModeWire.effect(
                AbilityMath.CUT,
                caster.getEntityId(),
                cut.x,
                cut.y,
                cut.z,
                cut.dx,
                cut.dy,
                cut.dz,
                cut.range,
                AbilityMath.cutTicks(cut.range)),
            mode);
        return AbilityMath.CUT_RECHARGE;
    }

    /** How far from a place, along a direction, nothing solid is in the way: at most {@code reach}. */
    private static double clear(World world, double x, double y, double z, Vec3 look, double reach) {
        // The game's own ray changes the vectors it is given: these are its own.
        Vec3 from = Vec3.createVectorHelper(x, y, z);
        Vec3 to = Vec3.createVectorHelper(x + look.xCoord * reach, y + look.yCoord * reach, z + look.zCoord * reach);
        MovingObjectPosition hit = world.func_147447_a(from, to, false, true, false);
        if (hit == null || hit.hitVec == null) {
            return reach;
        }
        double dx = hit.hitVec.xCoord - x;
        double dy = hit.hitVec.yCoord - y;
        double dz = hit.hitVec.zCoord - z;
        return Math.min(reach, Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    void tick(SorcererMode mode) {
        double[] v = new double[3];
        for (Iterator<Pull> i = pulls.iterator(); i.hasNext();) {
            Pull pull = i.next();
            if (++pull.age > AbilityMath.PULL_TICKS || pull.caster.isDead) {
                i.remove();
                continue;
            }
            for (EntityPlayerMP target : targets(pull.caster, mode)) {
                // Its middle, not its feet, is what the point pulls.
                double dx = pull.x - target.posX;
                double dy = pull.y - (target.posY + target.height / 2);
                double dz = pull.z - target.posZ;
                UUID id = target.getUniqueID();
                if (pull.guarded.contains(id)
                    || dx * dx + dy * dy + dz * dz > AbilityMath.PULL_RADIUS * AbilityMath.PULL_RADIUS) {
                    continue;
                }
                if (!pull.caught.contains(id)) {
                    if (strike(pull.caster, target, Config.pullDamage, "ryoiki.atracao")) {
                        pull.caught.add(id);
                    } else {
                        pull.guarded.add(id);
                        continue;
                    }
                }
                AbilityMath.pull(dx, dy, dz, v);
                target.motionX = v[0];
                target.motionY = v[1];
                target.motionZ = v[2];
                target.fallDistance = 0;
                target.velocityChanged = true;
            }
        }
        for (Iterator<Cut> i = cuts.iterator(); i.hasNext();) {
            Cut cut = i.next();
            double from = AbilityMath.cutGone(cut.age, cut.range);
            double to = AbilityMath.cutGone(++cut.age, cut.range);
            EntityPlayerMP met = null;
            double at = Double.MAX_VALUE;
            for (EntityPlayerMP target : targets(cut.caster, mode)) {
                AxisAlignedBB box = target.boundingBox;
                double enters = AbilityMath.enters(
                    cut.x,
                    cut.y,
                    cut.z,
                    cut.dx,
                    cut.dy,
                    cut.dz,
                    box.minX - AbilityMath.CUT_WIDTH,
                    box.minY - AbilityMath.CUT_WIDTH,
                    box.minZ - AbilityMath.CUT_WIDTH,
                    box.maxX + AbilityMath.CUT_WIDTH,
                    box.maxY + AbilityMath.CUT_WIDTH,
                    box.maxZ + AbilityMath.CUT_WIDTH);
                // Met on the stretch the blade covers this tick. (One it has already passed stepped in behind it.)
                if (enters >= 0 && enters <= to && enters >= from - AbilityMath.CUT_SPEED && enters < at) {
                    met = target;
                    at = enters;
                }
            }
            if (met != null && strike(cut.caster, met, Config.cutDamage, "ryoiki.talho")) {
                met.motionX += cut.dx * AbilityMath.CUT_THROW;
                met.motionY = Math.max(met.motionY, AbilityMath.CUT_LIFT);
                met.motionZ += cut.dz * AbilityMath.CUT_THROW;
                met.velocityChanged = true;
                double hx = cut.x + cut.dx * at;
                double hy = cut.y + cut.dy * at;
                double hz = cut.z + cut.dz * at;
                tell(
                    met.worldObj,
                    hx,
                    hy,
                    hz,
                    ModeWire.effect(
                        AbilityMath.CUT_HIT,
                        cut.caster.getEntityId(),
                        hx,
                        hy,
                        hz,
                        cut.dx,
                        cut.dy,
                        cut.dz,
                        at,
                        0),
                    mode);
                i.remove();
            } else if (to >= cut.range || cut.caster.isDead) {
                i.remove();
            }
        }
    }

    /** Who a player's techniques can touch: the others in the mode, in its world, that it may fight. */
    private static List<EntityPlayerMP> targets(EntityPlayerMP caster, SorcererMode mode) {
        List<EntityPlayerMP> out = new ArrayList<>();
        if (!MinecraftServer.getServer()
            .isPVPEnabled()) {
            return out;
        }
        for (Object o : caster.worldObj.playerEntities) {
            EntityPlayerMP other = (EntityPlayerMP) o;
            if (other != caster && other.isEntityAlive()
                && mode.isOn(other)
                && !other.capabilities.disableDamage
                && caster.canAttackPlayer(other)) {
                out.add(other);
            }
        }
        return out;
    }

    /**
     * The technique's blow, through the game's own damage: true if it landed. One that deals no damage lands
     * wherever players may fight.
     */
    private static boolean strike(EntityPlayerMP caster, EntityPlayer target, float damage, String name) {
        if (damage <= 0) {
            return true;
        }
        // A technique lands even right after another blow: the game's moment of grace is for its own fights.
        target.hurtResistantTime = 0;
        DamageSource source = new EntityDamageSource(name, caster).setDamageBypassesArmor()
            .setDamageIsAbsolute();
        return target.attackEntityFrom(source, damage);
    }

    private static void tell(World world, double x, double y, double z, byte[] data, SorcererMode mode) {
        for (Object o : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (player.getDistanceSq(x, y, z) <= SEEN * SEEN) {
                mode.send(player, data);
            }
        }
    }
}
