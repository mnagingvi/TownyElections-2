package com.townyelections.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests for the legacy and hex colour handling used by every message. */
class TextUtilTest {

    private static final String SECTION = "\u00A7";

    @Test
    void translatesLegacyColourCodes() {
        assertEquals(SECTION + "cRed", TextUtil.legacy("&cRed"));
        assertEquals(SECTION + "a" + SECTION + "b are fine", TextUtil.legacy("&a&b are fine"));
    }

    @Test
    void translatesHexColours() {
        assertEquals(SECTION + "x" + SECTION + "f" + SECTION + "f" + SECTION + "8"
                        + SECTION + "8" + SECTION + "0" + SECTION + "0Gold",
                TextUtil.legacy("&#ff8800Gold"));
    }

    @Test
    void leavesPlainTextUntouched() {
        assertEquals("plain text", TextUtil.legacy("plain text"));
        // '&' followed by something that is not a colour/format code stays literal.
        assertEquals("S&P 500 & more", TextUtil.legacy("S&P 500 & more"));
        assertEquals("", TextUtil.legacy(null));
    }

    @Test
    void acceptsNamedAndCodedColours() {
        assertEquals("&c", TextUtil.parseColor("red"));
        assertEquals("&9", TextUtil.parseColor("blue"));
        assertEquals("&c", TextUtil.parseColor("&c"));
        assertEquals("&#aabbcc", TextUtil.parseColor("#AABBCC"));
        assertEquals("&#aabbcc", TextUtil.parseColor("&#aabbcc"));
    }

    @Test
    void rejectsFormattingCodesAndNonsense() {
        assertNull(TextUtil.parseColor("&l"));
        assertNull(TextUtil.parseColor("chartreuse"));
        assertNull(TextUtil.parseColor(""));
        assertNull(TextUtil.parseColor(null));
    }
}
