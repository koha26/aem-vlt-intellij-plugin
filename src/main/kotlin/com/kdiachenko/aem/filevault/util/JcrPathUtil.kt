package com.kdiachenko.aem.filevault.util

import java.io.File

/**
 * Utility class for working with JCR paths
 */
object JcrPathUtil {

    /**
     * Calculate JCR path from a local file
     * Handles common AEM project structures
     */
    fun calculateJcrPath(file: File): String {
        // Standard FileVault structure: find jcr_root
        var current: File? = file
        while (current != null) {
            if (current.name == "jcr_root") {
                val relativePath = file.absolutePath.substring(current.absolutePath.length)
                return relativePath.replace('\\', '/')
            }

            // For Jackrabbit FileVault structure with content packages
            if (current.name == "content" && File(current, "META-INF/vault").exists()) {
                val jcrRootDir = File(current, "jcr_root")
                if (jcrRootDir.exists() && file.absolutePath.startsWith(jcrRootDir.absolutePath)) {
                    val relativePath = file.absolutePath.substring(jcrRootDir.absolutePath.length)
                    return relativePath.replace('\\', '/')
                }
            }

            current = current.parentFile
        }

        // If no jcr_root found, try to infer from common patterns
        val path = file.absolutePath

        // Check content/apps pattern
        val appsIndex = path.indexOf("/apps/")
        if (appsIndex >= 0) {
            return path.substring(appsIndex).replace('\\', '/')
        }

        // Check content/content pattern
        val contentIndex = path.indexOf("/content/")
        if (contentIndex >= 0) {
            return path.substring(contentIndex).replace('\\', '/')
        }

        // Check etc pattern
        val etcIndex = path.indexOf("/etc/")
        if (etcIndex >= 0) {
            return path.substring(etcIndex).replace('\\', '/')
        }

        // Check lib pattern
        val libIndex = path.indexOf("/lib/")
        if (libIndex >= 0) {
            return path.substring(libIndex).replace('\\', '/')
        }

        // Default to content path if no pattern matched
        return "/content"
    }

    /**
     * Identifies if a file is likely an AEM component
     */
    fun isAemComponent(file: File): Boolean {
        if (file.isDirectory) {
            // Check for component markers
            val hasComponentFiles = file.listFiles()?.any {
                it.name == ".content.xml" || it.name == "dialog.xml" || it.name == "cq:dialog"
            } ?: false

            if (hasComponentFiles) {
                return true
            }

            // Check path pattern
            val path = file.absolutePath.replace('\\', '/')
            return path.contains("/components/") || path.contains("/apps/") && path.matches(".*/[^/]+/components/[^/]+".toRegex())
        }

        return false
    }

    /**
     * Determines if a file is likely an AEM content node
     */
    fun isContentNode(file: File): Boolean {
        if (file.isFile && file.name == ".content.xml") {
            return true
        }

        val path = file.absolutePath.replace('\\', '/')
        return path.contains("/content/") && !path.contains("/content/dam/")
    }

    /**
     * Determines if a file is likely a DAM asset
     */
    fun isDamAsset(file: File): Boolean {
        val path = file.absolutePath.replace('\\', '/')
        return path.contains("/content/dam/")
    }
}
