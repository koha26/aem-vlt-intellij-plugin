package com.kdiachenko.aem.filevault.integration.filter

import org.apache.jackrabbit.vault.fs.api.PathFilterSet
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ScopedPathPolicyTest {

    @Test
    fun `policy includes matching files traverses ancestors and excludes siblings`() {
        val filter = DefaultWorkspaceFilter().apply {
            add(PathFilterSet("/apps/site/components"))
        }
        val policy = WorkspaceFilterScopedPathPolicy("/apps/site", filter)

        assertEquals(ScopedPathDecision.TRAVERSE_ONLY, policy.decide(Path.of(""), true))
        assertEquals(ScopedPathDecision.INCLUDE, policy.decide(Path.of("components"), true))
        assertEquals(ScopedPathDecision.INCLUDE, policy.decide(Path.of("components/button/.content.xml"), false))
        assertEquals(ScopedPathDecision.EXCLUDE, policy.decide(Path.of("clientlibs/site.js"), false))
    }
}
