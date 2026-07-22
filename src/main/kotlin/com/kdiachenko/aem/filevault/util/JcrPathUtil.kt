package com.kdiachenko.aem.filevault.util

import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.integration.filter.ContentPackageContext
import java.io.File
import java.nio.file.Path

/**
 * Utility class for working with JCR paths
 */
object JcrPathUtil {

    private const val JCR_ROOT = "jcr_root"

    fun Path.resolveContentPackage(): ContentPackageContext? {
        val absolute = toAbsolutePath().normalize()
        val jcrRoot = generateSequence(absolute) { it.parent }
            .firstOrNull { it.fileName?.toString() == JCR_ROOT }
            ?: return null
        val packageRoot = jcrRoot.parent ?: return null
        return ContentPackageContext(
            packageRoot = packageRoot,
            jcrRoot = jcrRoot,
            filterFile = packageRoot.resolve("META-INF").resolve("vault").resolve("filter.xml"),
        )
    }

    fun Path.toNormalizedJcrPath(context: ContentPackageContext): String {
        val relative = context.jcrRoot.relativize(toAbsolutePath().normalize())
        val raw = "/" + relative.iterator().asSequence().joinToString("/") { it.toString() }
        return (if (raw == "/") raw else raw.replace('\\', '/')).normalizeJcrPath()
    }

    fun File.toJcrPath(): String? = toPath().let { path ->
        path.resolveContentPackage()?.let { path.toNormalizedJcrPath(it) }
    }

    fun VirtualFile.toJcrPath(): String? = Path.of(path).let { path ->
        path.resolveContentPackage()?.let { path.toNormalizedJcrPath(it) }
    }

    fun String.toJcrPath(): String? = Path.of(this).let { path ->
        path.resolveContentPackage()?.let { path.toNormalizedJcrPath(it) }
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
