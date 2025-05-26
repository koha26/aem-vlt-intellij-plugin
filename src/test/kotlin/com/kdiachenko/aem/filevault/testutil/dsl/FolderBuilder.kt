package com.kdiachenko.aem.filevault.testutil.dsl

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

/**
 * DSL builder class for creating folder structures using java.nio.file.Path
 */
class PathBuilder(private val parentPath: Path) {

    /**
     * Creates a nested folder with the given name and executes the provided block
     * @param name The name of the folder to create
     * @param block The DSL block to execute within this folder context
     */
    fun folder(name: String, block: PathBuilder.() -> Unit = {}) {
        val newPath = parentPath.resolve(name)
        Files.createDirectories(newPath)
        PathBuilder(newPath).block()
    }

    /**
     * Creates a file with the given name and content
     * @param name The name of the file to create
     * @param content The content to write to the file
     */
    fun file(name: String, content: String = "") {
        val filePath = parentPath.resolve(name)
        Files.createDirectories(filePath.parent)
        filePath.writeText(content)
    }

    /**
     * Creates a file with binary content
     * @param name The name of the file to create
     * @param content The binary content to write to the file
     */
    fun file(name: String, content: ByteArray) {
        val filePath = parentPath.resolve(name)
        Files.createDirectories(filePath.parent)
        Files.write(filePath, content)
    }

    /**
     * Creates a symbolic link
     * @param linkName The name of the symbolic link
     * @param target The target path for the link
     */
    fun symlink(linkName: String, target: Path) {
        val linkPath = parentPath.resolve(linkName)
        Files.createDirectories(linkPath.parent)
        Files.createSymbolicLink(linkPath, target)
    }

    /**
     * Creates a symbolic link with string target
     * @param linkName The name of the symbolic link
     * @param target The target path as string for the link
     */
    fun symlink(linkName: String, target: String) {
        symlink(linkName, Paths.get(target))
    }
}

/**
 * Extension function to create a folder structure using DSL syntax with Path
 * @param parentPath The parent path where the new folder will be created
 * @param folderName The name of the folder to create
 * @param block The DSL block defining the folder structure
 */
fun folder(parentPath: Path, folderName: String, block: PathBuilder.() -> Unit) {
    val targetPath = parentPath.resolve(folderName)
    Files.createDirectories(targetPath)
    PathBuilder(targetPath).block()
}

/**
 * Extension function to create a folder structure using DSL syntax with string path
 * @param parentPath The parent path where the new folder will be created
 * @param folderName The name of the folder to create
 * @param block The DSL block defining the folder structure
 */
fun folder(parentPath: String, folderName: String, block: PathBuilder.() -> Unit) {
    folder(Paths.get(parentPath), folderName, block)
}

/**
 * Extension function to create a folder structure at the current working directory
 * @param folderName The name of the folder to create
 * @param block The DSL block defining the folder structure
 */
fun folder(folderName: String, block: PathBuilder.() -> Unit) {
    folder(Paths.get("."), folderName, block)
}

/**
 * Extension function to execute DSL block in an existing directory
 * @param path The existing directory path
 * @param block The DSL block to execute
 */
fun Path.structure(block: PathBuilder.() -> Unit) {
    Files.createDirectories(this)
    PathBuilder(this).block()
}

/**
 * Extension function to execute DSL block in an existing directory (string version)
 * @param path The existing directory path as string
 * @param block The DSL block to execute
 */
fun String.structure(block: PathBuilder.() -> Unit) {
    Paths.get(this).structure(block)
}
