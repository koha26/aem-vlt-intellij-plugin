package com.kdiachenko.aem.filevault.util

import com.intellij.mock.MockVirtualFile
import com.kdiachenko.aem.filevault.integration.filter.ContentPackageContext
import com.kdiachenko.aem.filevault.util.JcrPathUtil.normalizeJcrPath
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path

class JcrPathUtilTest {

    @Test
    fun `resolveContentPackage chooses nearest exact jcr_root`() {
        val selected = Path.of("C:/repo/jcr_root/outer/jcr_root/apps/site/component")

        val context = selected.resolveContentPackage()

        assertEquals(Path.of("C:/repo/jcr_root/outer"), context?.packageRoot)
        assertEquals(Path.of("C:/repo/jcr_root/outer/jcr_root"), context?.jcrRoot)
        assertEquals(
            Path.of("C:/repo/jcr_root/outer/META-INF/vault/filter.xml"),
            context?.filterFile,
        )
        assertEquals("/apps/site/component", selected.toNormalizedJcrPath(context!!))
    }

    @Test
    fun `resolveContentPackage rejects substring directory`() {
        assertNull(Path.of("C:/repo/my_jcr_root/apps/site").resolveContentPackage())
    }

    @Test
    fun `toNormalizedJcrPath maps content xml and cq namespace`() {
        val root = Path.of("C:/repo/jcr_root")
        val context = ContentPackageContext(
            packageRoot = Path.of("C:/repo"),
            jcrRoot = root,
            filterFile = Path.of("C:/repo/META-INF/vault/filter.xml"),
        )

        assertEquals(
            "/apps/site/components/button",
            root.resolve("apps/site/components/button/.content.xml").toNormalizedJcrPath(context),
        )
        assertEquals(
            "/apps/site/components/button/cq:dialog",
            root.resolve("apps/site/components/button/_cq_dialog.xml").toNormalizedJcrPath(context),
        )
    }

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
        val virtualFile = virtualFile("/content/project/jcr_root/content/dam/image.png")
        val result = virtualFile.toJcrPath()
        assertEquals("/content/dam/image.png", result)
    }

    @Test
    fun `VirtualFile toJcrPath should return null when jcr_root does not exist`() {
        val virtualFile = virtualFile("/content/project/content/dam/image.png")
        val result = virtualFile.toJcrPath()
        assertNull(result)
    }

    @Test
    fun `VirtualFile toJcrPath uses its path unchanged`() {
        val virtualFile = MockVirtualFile("/content/project/jcr_root/content/dam/image.png")

        assertThrows(InvalidPathException::class.java) { virtualFile.toJcrPath() }
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

    private fun virtualFile(path: String) = object : MockVirtualFile("unused") {
        override fun getPath() = path
    }
}
