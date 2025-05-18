package com.kdiachenko.aem.filevault.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AEMVltConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: AEMVltConfigurable

    public override fun setUp() {
        super.setUp()

        configurable = AEMVltConfigurable()
    }

    fun testGetDisplayName() {
        val displayName = configurable.displayName
        assertEquals("AEM VLT Settings", displayName)
    }

    fun testCreateComponent() {
        val component = configurable.createComponent()
        assertNotNull(component)
    }

    fun testIsModified_initiallyFalse() {
        assertFalse(configurable.isModified)
    }

    fun testGetPreferredFocusedComponent() {
        configurable.createComponent()

        val focusedComponent = configurable.preferredFocusedComponent
        assertNotNull(focusedComponent)
    }

    fun testReset() {
        configurable.createComponent()

        configurable.reset()
    }

    fun testDisposeUIResources() {
        configurable.createComponent()

        configurable.disposeUIResources()
    }

    fun testApply() {
        configurable.createComponent()

        configurable.apply()
    }
}
