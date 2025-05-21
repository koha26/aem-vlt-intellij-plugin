package com.kdiachenko.aem.filevault.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kdiachenko.aem.filevault.util.JcrPathUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.Icon
import java.io.File

class BaseOperationActionTest : BasePlatformTestCase() {

    private lateinit var action: TestBaseOperationAction
    private lateinit var testVirtualFile: TestVirtualFile

    public override fun setUp() {
        super.setUp()

        // Create a test implementation of BaseOperationAction
        action = TestBaseOperationAction()

        // Create a test virtual file
        testVirtualFile = TestVirtualFile("test.txt")
    }

    fun testVirtualToIoFile() {
        // Test that virtualToIoFile converts the virtual file to an IO file
        val result = action.testVirtualToIoFile(testVirtualFile)
        assertEquals(File(testVirtualFile.path), result)
    }

    fun testInUnderJcrRoot_whenJcrPathExists() {
        // Set up a virtual file with a JCR path
        val fileWithJcrPath = TestVirtualFile("/path/to/jcr_root/content/path/test.txt")

        // Test that inUnderJcrRoot returns true when the file has a JCR path
        val result = action.testInUnderJcrRoot(fileWithJcrPath)
        assertTrue(result)
    }

    fun testInUnderJcrRoot_whenJcrPathDoesNotExist() {
        // Set up a virtual file without a JCR path
        val fileWithoutJcrPath = TestVirtualFile("non-jcr/path/test.txt")

        // Test that inUnderJcrRoot returns false when the file doesn't have a JCR path
        val result = action.testInUnderJcrRoot(fileWithoutJcrPath)
        assertFalse(result)
    }

    /**
     * Test implementation of BaseOperationAction that exposes protected methods for testing
     */
    private class TestBaseOperationAction : BaseOperationAction() {
        override fun getIcon(): Icon = AllIcons.Empty

        override fun actionPerformed(e: AnActionEvent) {
            // Not needed for testing
        }

        fun testVirtualToIoFile(virtualFile: VirtualFile): File {
            return virtualToIoFile(virtualFile)
        }

        fun testInUnderJcrRoot(virtualFile: VirtualFile): Boolean {
            return virtualFile.inUnderJcrRoot()
        }
    }

    /**
     * Test implementation of VirtualFile for testing
     */
    private class TestVirtualFile(private val filePath: String) : VirtualFile() {
        override fun getName(): String = filePath.substringAfterLast('/')

        override fun getFileSystem() = throw UnsupportedOperationException("Not implemented")

        override fun getPath(): String = filePath

        override fun isWritable() = true

        override fun isDirectory() = false

        override fun isValid() = true

        override fun getParent() = null

        override fun getChildren() = emptyArray<VirtualFile>()

        override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) =
            throw UnsupportedOperationException("Not implemented")

        override fun contentsToByteArray() = ByteArray(0)

        override fun getTimeStamp() = 0L

        override fun getLength() = 0L

        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

        override fun getInputStream() = throw UnsupportedOperationException("Not implemented")

        // Helper method to simulate JcrPathUtil.toJcrPath
        fun toJcrPath(): String? {
            return if (path.contains("content/")) "/content/${path.substringAfter("content/")}" else null
        }
    }
}
