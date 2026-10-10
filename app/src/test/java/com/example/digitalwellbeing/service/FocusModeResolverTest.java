package com.example.digitalwellbeing.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.digitalwellbeing.data.entity.FocusMode;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FocusModeResolverTest {

    private final FocusModeResolver resolver = new FocusModeResolver();

    private FocusMode mode(String name, int start, int end, int daysMask) {
        FocusMode mode = new FocusMode();
        mode.name = name;
        mode.startMinute = start;
        mode.endMinute = end;
        mode.daysMask = daysMask;
        mode.enabled = true;
        mode.blockedPackages = "com.example.social,com.example.video";
        return mode;
    }

    @Test
    public void picksTheModeCoveringTheCurrentMinute() {
        List<FocusMode> modes = Arrays.asList(
                mode("Study", 9 * 60, 12 * 60, 0x7F),
                mode("Sleep", 23 * 60, 6 * 60, 0x7F));
        assertEquals("Study", resolver.activeMode(modes, 10 * 60, 3).name);
        assertEquals("Sleep", resolver.activeMode(modes, 1 * 60, 3).name);
        assertNull(resolver.activeMode(modes, 15 * 60, 3));
    }

    @Test
    public void ignoresModesThatDoNotCoverToday() {
        List<FocusMode> modes = Collections.singletonList(
                mode("Work", 9 * 60, 17 * 60, 0b0011111));
        assertNull(resolver.activeMode(modes, 10 * 60, 7)); // Sunday
    }

    @Test
    public void blocksOnlyListedPackages() {
        FocusMode study = mode("Study", 0, 0, 0x7F);
        assertTrue(resolver.blocks(study, "com.example.video"));
        assertFalse(resolver.blocks(study, "com.example.notes"));
        assertFalse(resolver.blocks(null, "com.example.video"));
    }
}
