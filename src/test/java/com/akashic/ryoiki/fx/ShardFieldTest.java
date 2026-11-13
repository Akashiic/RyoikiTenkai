package com.akashic.ryoiki.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShardFieldTest {

    @Test
    void theShardsCoverTheWholeScreenAtFirst() {
        ShardField field = new ShardField(7, 5, 42);
        assertEquals(70, field.shards.length);
        float area = 0;
        for (ShardField.Shard s : field.shards) {
            assertTrue(ShardField.area(s) > 0, "a shard with no area");
            for (int k = 0; k < 3; k++) {
                assertTrue(s.x[k] >= 0 && s.x[k] <= 1 && s.y[k] >= 0 && s.y[k] <= 1);
            }
            area += ShardField.area(s);
        }
        assertEquals(1, area, 1e-4);
    }

    @Test
    void theSameSeedBreaksTheSameWay() {
        ShardField a = new ShardField(6, 4, 7);
        ShardField b = new ShardField(6, 4, 7);
        for (int i = 0; i < a.shards.length; i++) {
            assertEquals(a.shards[i].centerX, b.shards[i].centerX);
            assertEquals(a.shards[i].velocityY, b.shards[i].velocityY);
            assertEquals(a.shards[i].spin, b.shards[i].spin);
        }
    }

    @Test
    void aShardHoldsThenFliesAndNeverGoesBack() {
        ShardField field = new ShardField(6, 4, 3);
        for (ShardField.Shard s : field.shards) {
            assertEquals(0, ShardField.flight(s, 0));
            assertEquals(0, ShardField.flight(s, s.delay));
            assertEquals(1, ShardField.flight(s, 1), 1e-5);
            float last = 0;
            for (float t = 0; t <= 1; t += 0.05F) {
                float f = ShardField.flight(s, t);
                assertTrue(f >= last);
                last = f;
            }
            assertTrue(s.delay <= 0.45F);
        }
    }
}
