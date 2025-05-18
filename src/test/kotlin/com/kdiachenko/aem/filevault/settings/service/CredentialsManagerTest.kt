package com.kdiachenko.aem.filevault.settings.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CredentialsManagerTest : BasePlatformTestCase() {

    private val testId = "test-server-id"
    private val testUsername = "test-username"
    private val testPassword = "test-password"

    public override fun setUp() {
        super.setUp()

        CredentialsManager.remove(testId)
    }

    public override fun tearDown() {
        CredentialsManager.remove(testId)

        super.tearDown()
    }

    fun testAddAndGet() {
        CredentialsManager.add(testId, testUsername, testPassword)

        val credentials = CredentialsManager.get(testId)

        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.userName)
        assertEquals(testPassword, credentials?.getPasswordAsString())
    }

    fun testRemove() {
        CredentialsManager.add(testId, testUsername, testPassword)

        val credentials = CredentialsManager.get(testId)
        assertNotNull(credentials)

        CredentialsManager.remove(testId)

        val removedCredentials = CredentialsManager.get(testId)
        assertNull(removedCredentials)
    }

    fun testGetAsync() {
        CredentialsManager.add(testId, testUsername, testPassword)

        val credentialsPromise = CredentialsManager.getAsync(testId)
        val credentials = credentialsPromise.blockingGet(5000)

        assertNotNull(credentials)
        assertEquals(testUsername, credentials?.userName)
        assertEquals(testPassword, credentials?.getPasswordAsString())
    }

    fun testGetNonExistentCredentials() {
        val credentials = CredentialsManager.get("non-existent-id")

        assertNull(credentials)
    }

    fun testUpdateCredentials() {
        CredentialsManager.add(testId, testUsername, testPassword)

        val initialCredentials = CredentialsManager.get(testId)
        assertNotNull(initialCredentials)
        assertEquals(testUsername, initialCredentials?.userName)
        assertEquals(testPassword, initialCredentials?.getPasswordAsString())

        val updatedUsername = "updated-username"
        val updatedPassword = "updated-password"
        CredentialsManager.add(testId, updatedUsername, updatedPassword)

        val updatedCredentials = CredentialsManager.get(testId)
        assertNotNull(updatedCredentials)
        assertEquals(updatedUsername, updatedCredentials?.userName)
        assertEquals(updatedPassword, updatedCredentials?.getPasswordAsString())
    }
}
