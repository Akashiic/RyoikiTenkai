package com.akashic.ryoiki.mode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.akashic.ryoiki.Config;
import com.akashic.ryoiki.DomainType;
import com.akashic.ryoiki.domain.DomainManager;

/**
 * The techniques of the sorcerer mode, on the server: what each does to the world, tick by tick.
 *
 * <p>
 * A technique touches only <b>other players who are in the mode too</b>. The owner's rule (2026-11-13): the mode is
 * for who has its permission, and among them <b>nothing protects</b>: not a region, not a rule against players
 * fighting, not a plugin's shield. So what a technique does is done directly, life taken and bodies moved, and never
 * asked of the game's own damage, which every protection listens to. The damage is small and the same whatever the
 * armour: the mode's fights are not decided by equipment.
 */
final class Abilities {

    /** How far around a technique players are told of it. */
    private static final double SEEN = 64;

    private final List<Orb> orbs = new ArrayList<>();
    private final List<Cut> cuts = new ArrayList<>();
    private final List<Red> reds = new ArrayList<>();

    private static final class Red {

        EntityPlayerMP caster;
        /** Gathered in the air: the caster hangs there, and the orb flies wherever it looks. */
        boolean air;
        int age;
        /** Once let fly: from where, which way, how far. */
        boolean flying;
        double x;
        double y;
        double z;
        final double[] way = new double[3];
        double range;
        final Set<UUID> met = new HashSet<>();
    }

    private static final class Orb {

        EntityPlayerMP caster;
        double x;
        double y;
        double z;
        final double[] velocity = new double[3];
        /** How far before its caster's eyes it keeps. */
        double reach;
        /** Begun in the air: the caster hangs there and the orb goes anywhere. */
        boolean air;
        int age;
        /** Who it has drawn in, and has not let go yet. */
        final Set<UUID> caught = new HashSet<>();
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
        orbs.clear();
        cuts.clear();
        reds.clear();
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
            // Atração: the orb appears where the caster looks, before the first thing solid in the way.
            Orb orb = new Orb();
            orb.caster = caster;
            orb.air = !caster.onGround;
            orb.reach = AbilityMath.ORB_AIM;
            double reach = Math.max(1.2, clear(caster.worldObj, ex, ey, ez, look, orb.reach) - 0.4);
            orb.x = ex + look.xCoord * reach;
            orb.y = ey + look.yCoord * reach;
            orb.z = ez + look.zCoord * reach;
            if (!orb.air) {
                orb.y = AbilityMath.grounded(orb.y, caster.posY);
            }
            orbs.add(orb);
            tell(
                caster.worldObj,
                orb.x,
                orb.y,
                orb.z,
                ModeWire.effect(
                    AbilityMath.PULL,
                    caster.getEntityId(),
                    orb.x,
                    orb.y,
                    orb.z,
                    orb.air ? 1 : 0,
                    0,
                    0,
                    AbilityMath.ORB_RADIUS,
                    AbilityMath.ORB_TICKS),
                mode);
            return AbilityMath.ORB_RECHARGE;
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

    /**
     * A player uses the second technique of its domain's kind. Returns the ticks it now recharges for, or 0 if its
     * domain's kind has none yet.
     */
    int second(EntityPlayerMP caster, DomainType technique, SorcererMode mode) {
        if (technique != DomainType.INFINITE_VOID) {
            return 0;
        }
        for (Red other : reds) {
            if (other.caster == caster) {
                return 0;
            }
        }
        // Repulsão: gathered first, where the caster is; let fly when it is ready.
        Red red = new Red();
        red.caster = caster;
        red.air = !caster.onGround;
        reds.add(red);
        tell(
            caster.worldObj,
            caster.posX,
            caster.posY,
            caster.posZ,
            ModeWire.effect(
                AbilityMath.RED_CHARGE,
                caster.getEntityId(),
                caster.posX,
                caster.posY,
                caster.posZ,
                red.air ? 1 : 0,
                0,
                0,
                0,
                AbilityMath.RED_CHARGE_TICKS),
            mode);
        return AbilityMath.RED_RECHARGE;
    }

    /** One tick of a red orb. False when it is over. */
    private boolean red(Red red, SorcererMode mode) {
        EntityPlayerMP caster = red.caster;
        if (!red.flying) {
            if (!caster.isEntityAlive() || !mode.isOn(caster) || DomainManager.INSTANCE.frozen(caster)) {
                // Never let fly.
                return false;
            }
            if (red.air) {
                hang(caster);
            }
            if (++red.age < AbilityMath.RED_CHARGE_TICKS) {
                return true;
            }
            // Let fly, the way the caster looks now: from before its chest, until something solid.
            Vec3 look = caster.getLookVec();
            red.way[0] = look.xCoord;
            red.way[1] = look.yCoord;
            red.way[2] = look.zCoord;
            if (!red.air) {
                AbilityMath.level(look.xCoord, look.yCoord, look.zCoord, red.way);
            }
            Vec3 way = Vec3.createVectorHelper(red.way[0], red.way[1], red.way[2]);
            red.x = caster.posX + red.way[0] * AbilityMath.CUT_AHEAD;
            red.y = caster.posY + caster.getEyeHeight() - AbilityMath.CUT_BELOW + red.way[1] * AbilityMath.CUT_AHEAD;
            red.z = caster.posZ + red.way[2] * AbilityMath.CUT_AHEAD;
            red.range = clear(caster.worldObj, red.x, red.y, red.z, way, AbilityMath.RED_RANGE);
            red.flying = true;
            red.age = 0;
            if (red.air) {
                caster.fallDistance = 0;
            }
            tell(
                caster.worldObj,
                red.x,
                red.y,
                red.z,
                ModeWire.effect(
                    AbilityMath.RED_FIRE,
                    caster.getEntityId(),
                    red.x,
                    red.y,
                    red.z,
                    red.way[0],
                    red.way[1],
                    red.way[2],
                    red.range,
                    (int) Math.ceil(red.range / AbilityMath.RED_SPEED)),
                mode);
            return true;
        }
        double from = AbilityMath.redGone(red.age, red.range);
        double to = AbilityMath.redGone(++red.age, red.range);
        for (EntityPlayerMP target : targets(caster, mode)) {
            if (red.met.contains(target.getUniqueID())) {
                continue;
            }
            AxisAlignedBB box = target.boundingBox;
            double enters = AbilityMath.enters(
                red.x,
                red.y,
                red.z,
                red.way[0],
                red.way[1],
                red.way[2],
                box.minX - AbilityMath.RED_WIDTH,
                box.minY - AbilityMath.RED_WIDTH,
                box.minZ - AbilityMath.RED_WIDTH,
                box.maxX + AbilityMath.RED_WIDTH,
                box.maxY + AbilityMath.RED_WIDTH,
                box.maxZ + AbilityMath.RED_WIDTH);
            if (enters < 0 || enters > to || enters < from - AbilityMath.RED_SPEED) {
                continue;
            }
            // It goes through: this one is thrown off along its way, and it flies on.
            red.met.add(target.getUniqueID());
            strike(caster, target, AbilityMath.redDamage(Config.redDamage, enters), "ryoiki.repulsao");
            double hard = AbilityMath.redThrow(enters);
            move(
                target,
                new double[] { red.way[0] * hard, Math.max(red.way[1] * hard, 0) + AbilityMath.RED_LIFT,
                    red.way[2] * hard });
            tell(
                target.worldObj,
                target.posX,
                target.posY + target.height / 2,
                target.posZ,
                ModeWire.effect(
                    AbilityMath.RED_HIT,
                    caster.getEntityId(),
                    target.posX,
                    target.posY + target.height / 2,
                    target.posZ,
                    red.way[0],
                    red.way[1],
                    red.way[2],
                    enters,
                    0),
                mode);
        }
        return to < red.range;
    }

    /** Held where it is in the air, for as long as this is done each tick. */
    private static void hang(EntityPlayerMP caster) {
        caster.motionX = 0;
        caster.motionY = 0;
        caster.motionZ = 0;
        caster.fallDistance = 0;
        caster.velocityChanged = true;
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
        for (Iterator<Orb> i = orbs.iterator(); i.hasNext();) {
            Orb orb = i.next();
            if (!orb(orb, mode)) {
                i.remove();
            }
        }
        for (Iterator<Cut> i = cuts.iterator(); i.hasNext();) {
            Cut cut = i.next();
            if (!cut(cut, mode)) {
                i.remove();
            }
        }
        for (Iterator<Red> i = reds.iterator(); i.hasNext();) {
            Red red = i.next();
            if (!red(red, mode)) {
                i.remove();
            }
        }
    }

    /** One tick of an orb. False when it is over. */
    private boolean orb(Orb orb, SorcererMode mode) {
        EntityPlayerMP caster = orb.caster;
        boolean over = ++orb.age > AbilityMath.ORB_TICKS || !caster.isEntityAlive()
            || !mode.isOn(caster)
            || caster.worldObj.getPlayerEntityByName(caster.getCommandSenderName()) != caster
            || DomainManager.INSTANCE.frozen(caster);
        double[] v = new double[3];
        if (!over) {
            // After the caster's eyes: where they look, as far out as it keeps, short of anything solid.
            Vec3 look = caster.getLookVec();
            double ex = caster.posX;
            double ey = caster.posY + caster.getEyeHeight();
            double ez = caster.posZ;
            double reach = Math.max(1.2, clear(caster.worldObj, ex, ey, ez, look, orb.reach) - 0.4);
            double tx = ex + look.xCoord * reach;
            double ty = ey + look.yCoord * reach;
            double tz = ez + look.zCoord * reach;
            if (!orb.air) {
                ty = AbilityMath.grounded(ty, caster.posY);
            }
            AbilityMath.follow(orb.x, orb.y, orb.z, tx, ty, tz, orb.velocity);
            orb.x += orb.velocity[0];
            orb.y += orb.velocity[1];
            orb.z += orb.velocity[2];
            if (orb.air) {
                // Begun in the air, it holds its caster there.
                hang(caster);
            }
        } else if (orb.air) {
            caster.fallDistance = 0;
        }
        for (EntityPlayerMP target : targets(caster, mode)) {
            // Its middle, not its feet, is what the orb pulls.
            double dx = orb.x - target.posX;
            double dy = orb.y - (target.posY + target.height / 2);
            double dz = orb.z - target.posZ;
            UUID id = target.getUniqueID();
            boolean near = dx * dx + dy * dy + dz * dz <= AbilityMath.ORB_RADIUS * AbilityMath.ORB_RADIUS;
            if (over || !near) {
                if (orb.caught.remove(id)) {
                    // Let go, by its end or by a swing it could not follow: it leaves with the orb's speed.
                    AbilityMath.thrown(orb.velocity[0], orb.velocity[1], orb.velocity[2], v);
                    move(target, v);
                }
                continue;
            }
            if (orb.caught.add(id) || orb.age % AbilityMath.ORB_PULSE == 0) {
                strike(caster, target, Config.pullDamage, "ryoiki.atracao");
            }
            AbilityMath.pull(dx, dy, dz, v);
            if (v[0] == 0 && v[1] == 0 && v[2] == 0) {
                // Held at the orb: it goes where the orb goes.
                v[0] = orb.velocity[0];
                v[1] = orb.velocity[1];
                v[2] = orb.velocity[2];
            }
            move(target, v);
        }
        tell(
            caster.worldObj,
            orb.x,
            orb.y,
            orb.z,
            ModeWire.effect(
                over ? AbilityMath.PULL_END : AbilityMath.PULL_MOVE,
                caster.getEntityId(),
                orb.x,
                orb.y,
                orb.z,
                orb.velocity[0],
                orb.velocity[1],
                orb.velocity[2],
                AbilityMath.ORB_RADIUS,
                Math.max(0, AbilityMath.ORB_TICKS - orb.age)),
            mode);
        return !over;
    }

    /** One tick of a flying cut. False when it is over. */
    private boolean cut(Cut cut, SorcererMode mode) {
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
        if (met == null) {
            return to < cut.range && !cut.caster.isDead;
        }
        strike(cut.caster, met, Config.cutDamage, "ryoiki.talho");
        move(
            met,
            new double[] { met.motionX + cut.dx * AbilityMath.CUT_THROW, Math.max(met.motionY, AbilityMath.CUT_LIFT),
                met.motionZ + cut.dz * AbilityMath.CUT_THROW });
        double hx = cut.x + cut.dx * at;
        double hy = cut.y + cut.dy * at;
        double hz = cut.z + cut.dz * at;
        tell(
            met.worldObj,
            hx,
            hy,
            hz,
            ModeWire.effect(AbilityMath.CUT_HIT, cut.caster.getEntityId(), hx, hy, hz, cut.dx, cut.dy, cut.dz, at, 0),
            mode);
        return false;
    }

    /** Who a player's techniques can touch: the others in the mode, in its world. Creative players are not touched. */
    private static List<EntityPlayerMP> targets(EntityPlayerMP caster, SorcererMode mode) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (Object o : caster.worldObj.playerEntities) {
            EntityPlayerMP other = (EntityPlayerMP) o;
            if (other != caster && other.isEntityAlive() && mode.isOn(other) && !other.capabilities.disableDamage) {
                out.add(other);
            }
        }
        return out;
    }

    private static void move(EntityPlayerMP target, double[] velocity) {
        target.motionX = velocity[0];
        target.motionY = velocity[1];
        target.motionZ = velocity[2];
        target.fallDistance = 0;
        target.velocityChanged = true;
    }

    /**
     * The technique's blow: life taken directly, absorption first, with the wince of a blow for all to see; and
     * death, by the one who used it, if it takes the last of it.
     */
    private static void strike(EntityPlayerMP caster, EntityPlayer target, float damage, String name) {
        if (damage <= 0) {
            return;
        }
        float absorbed = Math.min(target.getAbsorptionAmount(), damage);
        target.setAbsorptionAmount(target.getAbsorptionAmount() - absorbed);
        float left = target.getHealth() - (damage - absorbed);
        target.worldObj.setEntityState(target, (byte) 2);
        if (left > 0) {
            target.setHealth(left);
            return;
        }
        DamageSource source = new EntityDamageSource(name, caster).setDamageBypassesArmor()
            .setDamageIsAbsolute();
        target.setHealth(0);
        target.onDeath(source);
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
