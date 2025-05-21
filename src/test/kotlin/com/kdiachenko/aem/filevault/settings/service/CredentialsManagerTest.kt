package com.kdiachenko.aem.filevault.settings.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import com.kdiachenko.aem.filevault.integration.service.impl.MetaInfService
import org.junit.jupiter.api.BeforeEach

class CredentialsManagerTest : BasePlatformTestCase() {

    private val testId = "test-server-id"
    private val testUsername = "test-username"
    private val testPassword = "test-password"
    private lateinit var service: CredentialsManager

    public override fun setUp() {
        super.setUp()
        service = CredentialsManager.getInstance()

        service.remove(testId)
    }

    public override fun tearDown() {
        service.remove(testId)

        super.tearDown()
    }

    fun testAddAndGet() {
        service.add(testId, testUsername, testPassword)

        val credentials = service.get(testId)

        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.userName)
        assertEquals(testPassword, credentials?.getPasswordAsString())
    }

    fun testRemove() {
        service.add(testId, testUsername, testPassword)

        val credentials = service.get(testId)
        assertNotNull(credentials)

        service.remove(testId)

        val removedCredentials = service.get(testId)
        assertNull(removedCredentials)
    }

    fun testGetAsync() {
        service.add(testId, testUsername, testPassword)

        val credentialsPromise = service.getAsync(testId)
        val credentials = credentialsPromise.blockingGet(5000)

        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.userName)
        assertEquals(testPassword, credentials?.getPasswordAsString())
    }

    fun testGetNonExistentCredentials() {
        val credentials = service.get("non-existent-id")

        assertNull(credentials)
    }

    fun testUpdateCredentials() {
        service.add(testId, testUsername, testPassword)

        val initialCredentials = service.get(testId)
        assertNotNull(initialCredentials)
        assertEquals(testUsername, initialCredentials?.userName)
        assertEquals(testPassword, initialCredentials?.getPasswordAsString())

        val updatedUsername = "updated-username"
        val updatedPassword = "updated-password"
        service.add(testId, updatedUsername, updatedPassword)

        val updatedCredentials = service.get(testId)
        assertNotNull(updatedCredentials)
        assertEquals(updatedUsername, updatedCredentials?.userName)
        assertEquals(updatedPassword, updatedCredentials?.getPasswordAsString())
    }
}
