package com.kdiachenko.aem.filevault.integration.dto

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotEquals

class VltFilterTest {

    @Test
    fun testVltFilterWithDefaultValues() {
        val filter = VltFilter(root = "/content/test")

        assertEquals("/content/test", filter.root)
        assertEquals("", filter.mode)
        assertEquals(emptyList<String>(), filter.excludePatterns)
        assertEquals(emptyList<String>(), filter.includePatterns)
    }

    @Test
    fun testVltFilterWithCustomValues() {
        val filter = VltFilter(
            root = "/content/test",
            mode = "merge",
            excludePatterns = listOf("/content/test/exclude1", "/content/test/exclude2"),
            includePatterns = listOf("/content/test/include1", "/content/test/include2")
        )

        assertEquals("/content/test", filter.root)
        assertEquals("merge", filter.mode)
        assertEquals(listOf("/content/test/exclude1", "/content/test/exclude2"), filter.excludePatterns)
        assertEquals(listOf("/content/test/include1", "/content/test/include2"), filter.includePatterns)
    }

    @Test
    fun testVltFilterEqualityForIdenticalObjects() {
        val filter1 = VltFilter(
            root = "/content/test",
            mode = "merge",
            excludePatterns = listOf("/content/test/exclude"),
            includePatterns = listOf("/content/test/include")
        )

        val filter2 = VltFilter(
            root = "/content/test",
            mode = "merge",
            excludePatterns = listOf("/content/test/exclude"),
            includePatterns = listOf("/content/test/include")
        )

        assertEquals(filter1, filter2)
        assertEquals(filter1.hashCode(), filter2.hashCode())
    }

    @Test
    fun testVltFilterEqualityForDifferentObjects() {
        val filter1 = VltFilter(
            root = "/content/test",
            mode = "merge",
            excludePatterns = listOf("/content/test/exclude"),
            includePatterns = listOf("/content/test/include")
        )

        val filter2 = VltFilter(
            root = "/content/other",
            mode = "replace",
            excludePatterns = listOf("/content/other/exclude"),
            includePatterns = listOf("/content/other/include")
        )

        assertNotEquals(filter1, filter2)
        assertNotEquals(filter1.hashCode(), filter2.hashCode())
    }
}
