package com.rubixscript.flipscout.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotosTest {
    @Test fun smallImagesAreNotSampled() = assertEquals(1, sampleSize(800, 600, 1280))

    @Test fun halvesWhileStillCoveringTarget() {
        assertEquals(1, sampleSize(2000, 1500, 1280))
        assertEquals(2, sampleSize(4000, 3000, 1280))
        assertEquals(4, sampleSize(5200, 3900, 1280))
    }

    @Test fun usesLongestEdge() = assertEquals(2, sampleSize(1000, 4000, 1280))
}
