package de.remk0.flickpick.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun votes() {
        assertEquals("1.2M votes", formatVotes(1_234_567))
        assertEquals("2M votes", formatVotes(2_000_000))
        assertEquals("36.5k votes", formatVotes(36_512))
        assertEquals("870 votes", formatVotes(870))
        assertEquals("No votes", formatVotes(null))
    }

    @Test fun rating() {
        assertEquals("8.0", formatRating(8.0))
        assertEquals("–", formatRating(null))
    }
}
