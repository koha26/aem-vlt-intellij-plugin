package com.kdiachenko.aem.filevault.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Utility class for working with JCR paths
 */
object JcrPathUtil {

    fun File.toJcrPath(): String? = this.absolutePath.toJcrPath()

    fun VirtualFile.toJcrPath(): String? = this.path.toJcrPath()

    fun String.toJcrPath(): String? {
        val absolutePath = this
        if (absolutePath.indexOf("jcr_root") == -1) {
            return null
        }
        val jcrPath = absolutePath.substring(absolutePath.indexOf("jcr_root") + "jcr_root".length)
        if (jcrPath.isEmpty()) {
            return "/"
        }
        return FileUtil.normalize(jcrPath)
    }

    fun String.normalizeJcrPath(): String {
        if (this.endsWith("/.content.xml")) {
            return this.substring(0, this.length - 13)
        }
        if (this.endsWith(".xml") && isCqNamespacedFile(this)) {
            val filter = this.replace(".xml", "")
                .replace("_cq_", "cq:")
            return filter
        }
        return this
    }

    fun isCqNamespacedFile(jcrPath: String): Boolean {
        val name = jcrPath.substringAfterLast("/")
        return name.startsWith("_cq_")
    }
}
