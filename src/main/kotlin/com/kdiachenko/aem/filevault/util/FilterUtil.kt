package com.kdiachenko.aem.filevault.util

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Utility class for working with FileVault filter.xml files
 */
object FilterUtil {

    /**
     * Generate a filter.xml file for a specific JCR path
     *
     * @param targetDir Directory where to save the filter.xml
     * @param jcrPath JCR path to include in the filter
     * @return The created filter.xml file
     */
    fun generateFilterXml(targetDir: File, jcrPath: String): File {
        // Ensure path starts with /
        val normalizedPath = if (jcrPath.startsWith("/")) jcrPath else "/$jcrPath"

        // Create META-INF/vault directory if needed
        val metaInfDir = File(targetDir, "META-INF/vault")
        metaInfDir.mkdirs()

        val filterFile = File(metaInfDir, "filter.xml")

        // Create the XML document
        val docFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docFactory.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        // Create root element
        val rootElement = doc.createElement("workspaceFilter")
        rootElement.setAttribute("version", "1.0")
        doc.appendChild(rootElement)

        // Create filter element
        val filterElement = doc.createElement("filter")
        filterElement.setAttribute("root", normalizedPath)
        rootElement.appendChild(filterElement)

        // Add include rule
        val includeElement = doc.createElement("include")
        includeElement.setAttribute("pattern", normalizedPath + "(/.*)?")
        filterElement.appendChild(includeElement)

        // Write to file
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(doc)
        val result = StreamResult(filterFile)
        transformer.transform(source, result)

        return filterFile
    }

    /**
     * Add a path to an existing filter.xml file
     *
     * @param filterFile Existing filter.xml file
     * @param jcrPath JCR path to add
     * @return True if the path was added, false if it already exists
     */
    fun addPathToFilter(filterFile: File, jcrPath: String): Boolean {
        if (!filterFile.exists()) {
            return false
        }

        // Ensure path starts with /
        val normalizedPath = if (jcrPath.startsWith("/")) jcrPath else "/$jcrPath"

        // Parse the XML
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(filterFile)

        // Check if the path already exists
        val filterElements = doc.getElementsByTagName("filter")
        for (i in 0 until filterElements.length) {
            val filterElement = filterElements.item(i) as Element
            val root = filterElement.getAttribute("root")
            if (root == normalizedPath) {
                return false // Path already exists
            }
        }

        // Add the new filter
        val rootElement = doc.documentElement
        val filterElement = doc.createElement("filter")
        filterElement.setAttribute("root", normalizedPath)
        rootElement.appendChild(filterElement)

        val includeElement = doc.createElement("include")
        includeElement.setAttribute("pattern", normalizedPath + "(/.*)?")
        filterElement.appendChild(includeElement)

        // Write back to file
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(doc)
        val result = StreamResult(filterFile)
        transformer.transform(source, result)

        return true
    }

    /**
     * Find an existing filter.xml file in the project structure
     *
     * @param startDir Directory to start the search from
     * @return The filter.xml file if found, null otherwise
     */
    fun findFilterXml(startDir: File): File? {
        // First check the META-INF/vault directory in this directory
        val metaInfDir = File(startDir, "META-INF/vault")
        val filterFile = File(metaInfDir, "filter.xml")
        if (filterFile.exists()) {
            return filterFile
        }

        // Look for jcr_root directory
        var current: File? = startDir
        while (current != null) {
            if (current.name == "jcr_root") {
                val parentDir = current.parentFile
                val vaultDir = File(parentDir, "META-INF/vault")
                val parentFilterFile = File(vaultDir, "filter.xml")
                if (parentFilterFile.exists()) {
                    return parentFilterFile
                }
            }
            current = current.parentFile
        }

        // Look for common content package structure
        current = startDir
        while (current != null) {
            val vaultDir = File(current, "META-INF/vault")
            if (vaultDir.exists()) {
                val packageFilterFile = File(vaultDir, "filter.xml")
                if (packageFilterFile.exists()) {
                    return packageFilterFile
                }
            }
            current = current.parentFile
        }

        return null
    }
}
