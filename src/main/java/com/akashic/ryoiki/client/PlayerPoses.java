package com.akashic.ryoiki.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.akashic.ryoiki.fx.BlowPose;
import com.akashic.ryoiki.fx.CastPose;
import com.akashic.ryoiki.fx.Gait;

/**
 * Everything the mod asks of a player's model at once, as one pose. A model takes one pose a frame, so what is
 * layered here is layered by hand: the way the body stands, runs and jumps in the sorcerer mode underneath; over
 * it, the guard and the blows of a clash and the blow taken; over that, the gesture of a technique.
 *
 * <p>
 * While the body runs, jumps, fights or makes a gesture, the pose is of every bone the mod knows, hands and fingers
 * included: <b>nothing of the model's own animation is left in them</b>. Standing still and doing nothing, the model
 * is left to its own resting animation. A model's resting pose may have a hand on a hip or a wrist turned,
 * and a bone the mod did not pose would keep that through a technique.
 */
final class PlayerPoses {

    private static final int BODY = BlowPose.BONES.length;
    /** The bones of the body and, after them, those only a gesture has (the hands, their fingers). */
    private static final String[] ALL;
    /** Where each bone of a gesture is among them. */
    private static final int[] GESTURE = new int[CastPose.BONES.length];
    /** The bones of the arms and of the legs, among the bones of the body; the first is the whole body. */
    private static final int WHOLE = 0;
    private static final int ARMS_FROM = 3;
    private static final int ARMS_TO = 6;
    private static final int LEGS_FROM = 7;

    static {
        List<String> all = new ArrayList<>(Arrays.asList(BlowPose.BONES));
        for (int i = 0; i < CastPose.BONES.length; i++) {
            int at = all.indexOf(CastPose.BONES[i]);
            if (at < 0) {
                at = all.size();
                all.add(CastPose.BONES[i]);
            }
            GESTURE[i] = at;
        }
        ALL = all.toArray(new String[0]);
    }

    private PlayerPoses() {}

    /**
     * Poses a player's model for this frame.
     *
     * @param fight   the guard or the blow of a clash, and how much of it shows; null for none
     * @param fighter whether the player is one of the two of a clash: its arms keep the guard between blows
     * @param gesture the gesture of a technique ({@link CastPose#BONES}), and how much of it shows; null for none
     * @return whether anything was posed
     */
    static boolean apply(EntityPlayer player, float partial, float[] fight, float fightShows, boolean fighter,
        float[] gesture, float gestureShows) {
        Stride.Track track = Stride.INSTANCE.of(player);
        float[] pose = null;
        float shows = 0;
        float moving = 0;
        if (track != null) {
            Gait.State state = track.at(partial);
            // With the hands and the fingers, at rest: not as the model's own animation would leave them.
            pose = Arrays.copyOf(Gait.sample(state), ALL.length * 3);
            moving = state.moving;
            // Standing still, out of a fight, the model keeps its own resting animation (the owner's rule): the
            // mode's pose shows as the body moves or leaves the ground, and for the two of a clash.
            float active = fighter ? 1 : Math.max(smooth(state.moving), state.air);
            shows = track.weight(partial) * active;
            if (fighter) {
                // Between blows the hands stay up, whatever the legs are doing.
                float[] guard = BlowPose.guard();
                for (int bone = ARMS_FROM; bone <= ARMS_TO; bone++) {
                    blend(pose, guard, bone, 0.9F);
                }
            }
        }
        if (fight != null && fightShows > 0) {
            if (pose == null) {
                pose = Arrays.copyOf(fight, ALL.length * 3);
                shows = fightShows;
            } else {
                // A blow is thrown with the whole body from where it stands; running, the legs go on running.
                for (int bone = 0; bone < BODY; bone++) {
                    blend(pose, fight, bone, fightShows * (bone >= LEGS_FROM ? 1 - 0.85F * moving : 1));
                }
                shows = Math.max(shows, fightShows);
            }
        }
        if (gesture != null && gestureShows > 0) {
            if (pose == null) {
                FuturaBridge.INSTANCE.pose(player, CastPose.BONES, gesture, gestureShows);
                return true;
            }
            for (int i = 0; i < GESTURE.length; i++) {
                int bone = GESTURE[i];
                // A gesture is made with the whole body from where it stands; running, the legs go on running and
                // the body keeps the lean of the run.
                boolean carried = bone == WHOLE || bone >= LEGS_FROM && bone < BODY;
                float much = gestureShows * (carried ? 1 - 0.85F * moving : 1);
                for (int axis = 0; axis < 3; axis++) {
                    int at = bone * 3 + axis;
                    pose[at] += (gesture[i * 3 + axis] - pose[at]) * much;
                }
            }
            shows = Math.max(shows, gestureShows);
        }
        if (pose == null || shows <= 0) {
            return false;
        }
        FuturaBridge.INSTANCE.pose(player, ALL, pose, shows);
        return true;
    }

    private static float smooth(float v) {
        v = v < 0 ? 0 : v > 1 ? 1 : v;
        return v * v * (3 - 2 * v);
    }

    private static void blend(float[] pose, float[] over, int bone, float much) {
        for (int axis = 0; axis < 3; axis++) {
            int at = bone * 3 + axis;
            pose[at] += (over[at] - pose[at]) * much;
        }
    }
}
