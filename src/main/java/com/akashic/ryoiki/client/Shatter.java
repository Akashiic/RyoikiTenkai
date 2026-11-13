package com.akashic.ryoiki.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.akashic.ryoiki.fx.ShardField;

/**
 * The end of a domain, on the screen: for an instant the view is whole, in the domain's colour; then it breaks into
 * shards that fly apart, and the world is behind them.
 */
final class Shatter {

    static final int TICKS = 24;

    private static final ShardField FIELD = new ShardField(8, 5, 0x5348415244L);

    private Shatter() {}

    /** @param left ticks left of the {@link #TICKS}, with the fraction */
    static void draw(DomainLook look, float left, int width, int height) {
        float time = Draw.clamp01(1 - left / TICKS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Draw.translucent();
        Tessellator t = Tessellator.instance;
        int body = look.shardColor();
        int edge = look.shardEdgeColor();
        t.startDrawing(GL11.GL_TRIANGLES);
        for (ShardField.Shard s : FIELD.shards) {
            float flight = ShardField.flight(s, time);
            if (flight >= 1) {
                continue;
            }
            float alpha = 1 - flight * flight;
            float scale = 1 - flight * 0.55F;
            double turn = s.spin * flight * Math.PI * 2;
            float cos = (float) Math.cos(turn) * scale;
            float sin = (float) Math.sin(turn) * scale;
            float cx = (s.centerX + s.velocityX * flight) * width;
            // Falling a little as it goes.
            float cy = (s.centerY + s.velocityY * flight + flight * flight * 0.25F) * height;
            for (int k = 0; k < 3; k++) {
                float dx = (s.x[k] - s.centerX) * width;
                float dy = (s.y[k] - s.centerY) * height;
                // Each corner its own shade: the shards catch the light unevenly, like glass.
                float glint = (s.shade + k * 0.31F) % 1F;
                Draw.color(t, Draw.mix(body, edge, glint * (0.18F + 0.5F * flight)), alpha);
                t.addVertex(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos, 0);
            }
        }
        t.draw();
        // The cracks: bright lines along the shards' edges, strongest as they let go.
        GL11.glLineWidth(1.5F);
        t.startDrawing(GL11.GL_LINES);
        for (ShardField.Shard s : FIELD.shards) {
            float flight = ShardField.flight(s, time);
            if (flight >= 1) {
                continue;
            }
            float crack = Draw.clamp01(time * 9) * (1 - flight);
            float scale = 1 - flight * 0.55F;
            double turn = s.spin * flight * Math.PI * 2;
            float cos = (float) Math.cos(turn) * scale;
            float sin = (float) Math.sin(turn) * scale;
            float cx = (s.centerX + s.velocityX * flight) * width;
            float cy = (s.centerY + s.velocityY * flight + flight * flight * 0.25F) * height;
            Draw.color(t, edge, 0.85F * crack);
            for (int k = 0; k < 3; k++) {
                for (int end = 0; end < 2; end++) {
                    int c = (k + end) % 3;
                    float dx = (s.x[c] - s.centerX) * width;
                    float dy = (s.y[c] - s.centerY) * height;
                    t.addVertex(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos, 0);
                }
            }
        }
        t.draw();
        GL11.glLineWidth(1);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
    }
}
