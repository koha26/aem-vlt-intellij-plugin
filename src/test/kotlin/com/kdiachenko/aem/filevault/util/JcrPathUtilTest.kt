package com.kdiachenko.aem.filevault.util

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.util.io.FileUtil
import com.kdiachenko.aem.filevault.util.JcrPathUtil.normalizeJcrPath
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File

class JcrPathUtilTest {

    @Test
    fun `File toJcrPath should return correct path when jcr_root exists`() {
        val file = File("/content/project/jcr_root/content/dam/image.png")
        val result = file.toJcrPath()
        assertEquals("/content/dam/image.png", result)
    }

    @Test
    fun `File toJcrPath should return null when jcr_root does not exist`() {
        val file = File("/content/project/content/dam/image.png")
        val result = file.toJcrPath()
        assertNull(result)
    }

    @Test
    fun `VirtualFile toJcrPath should return correct path when jcr_root exists`() {
        val virtualFile = MockVirtualFile("/content/project/jcr_root/content/dam/image.png")
        val result = virtualFile.toJcrPath()
        assertEquals("/content/dam/image.png", result)
    }

    @Test
    fun `VirtualFile toJcrPath should return null when jcr_root does not exist`() {
        val virtualFile = MockVirtualFile("/content/project/content/dam/image.png")
        val result = virtualFile.toJcrPath()
        assertNull(result)
    }

    @Test
    fun `String toJcrPath should return correct path when jcr_root exists`() {
        val path = "/content/project/jcr_root/content/dam/image.png"
        val result = path.toJcrPath()
        assertEquals("/content/dam/image.png", result)
    }

    @Test
    fun `String toJcrPath should return null when jcr_root does not exist`() {
        val path = "/content/project/content/dam/image.png"
        val result = path.toJcrPath()
        assertNull(result)
    }

    @Test
    fun `String toJcrPath should return root slash when jcr_root is at the end`() {
        val path = "/content/project/jcr_root"
        val result = path.toJcrPath()
        assertEquals("/", result)
    }

    @Test
    fun `String normalizeJcrPath should remove trailing _content xml`() {
        val path = "/content/dam/.content.xml"
        val result = path.normalizeJcrPath()
        assertEquals("/content/dam", result)
    }

    @Test
    fun `String normalizeJcrPath should remove same path`() {
        val path = "/content/dam/folder"
        val result = path.normalizeJcrPath()
        assertEquals("/content/dam/folder", result)
    }

    @Test
    fun `String normalizeJcrPath should convert cq namespaced file`() {
        val path = "/content/dam/_cq_dialog.xml"
        val result = path.normalizeJcrPath()
        assertEquals("/content/dam/cq:dialog", result)
    }

    @Test
    fun `String normalizeJcrPath should return path as is for regular xml`() {
        val path = "/content/dam/regular.xml"
        val result = path.normalizeJcrPath()
        assertEquals("/content/dam/regular.xml", result)
    }

    @Test
    fun `isCqNamespacedFile should return true for _cq_ prefixed file`() {
        val path = "/content/dam/_cq_dialog.xml"
        val result = JcrPathUtil.isCqNamespacedFile(path)
        assertEquals(true, result)
    }

    @Test
    fun `isCqNamespacedFile should return false for non _cq_ prefixed file`() {
        val path = "/content/dam/regular.xml"
        val result = JcrPathUtil.isCqNamespacedFile(path)
        assertEquals(false, result)
    }
}
