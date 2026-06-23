package pw.x4.autovpn.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test fun newer_patch() = assertTrue(isNewerVersion("1.0.1", "1.0.0"))

    @Test fun newer_minor_beats_higher_patch() = assertTrue(isNewerVersion("1.1.0", "1.0.9"))

    @Test fun equal_is_not_newer() = assertFalse(isNewerVersion("1.0.0", "1.0.0"))

    @Test fun older_is_not_newer() = assertFalse(isNewerVersion("1.0.0", "1.0.1"))

    @Test fun shorter_padded_with_zero() {
        assertTrue(isNewerVersion("1.0", "0.9.9"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
    }
}
