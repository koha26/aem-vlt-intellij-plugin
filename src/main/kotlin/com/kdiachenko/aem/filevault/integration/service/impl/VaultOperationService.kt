package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.diagnostic.Logger
import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.factory.IVaultAppFactory
import com.kdiachenko.aem.filevault.integration.factory.impl.VaultAppFactory
import com.kdiachenko.aem.filevault.integration.service.IVaultOperationService
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.apache.jackrabbit.vault.fs.io.*

/**
 * Service for executing Vault operations with the Vault FS App.
 */
object VaultOperationService : IVaultOperationService {
    private val logger = Logger.getInstance(VaultOperationService::class.java)

    override fun export(context: VltOperationContext) {
        executeVaultCommand {
            doExport(context)
        }
    }

    override fun import(context: VltOperationContext) {
        executeVaultCommand {
            doImport(context)
        }
    }

    /**
     * Executes a Vault command with the given server configuration, temporary directory, and progress listener.
     *
     * @param operation Operation to execute with the VltOperationContext
     */
    fun executeVaultCommand(operation: () -> Unit) {
        withPluginClassLoader {
            operation()
        }
    }

    /**
     * Exports content from AEM to the local file system.
     *
     * @param context Operation context containing all necessary information
     */
    private fun doExport(context: VltOperationContext) {
        val vaultFsApp = initVaultFsApp(context)

        val verbose = true
        val addr = RepositoryAddress(context.serverConfig.url + "/crx")
        val localFile = vaultFsApp.getPlatformFile(context.localAbsPath, false)

        var exporter: AbstractExporter? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            exporter = PlatformExporter(localFile)
            val vCtx = vaultFsApp.createVaultContext(localFile)

            vCtx.isVerbose = verbose
            val vaultFile = vCtx.getFileSystem(addr).getFile(context.jcrPath)
            if (vaultFile == null) {
                logger.error("Not such remote file: ${context.jcrPath}")
                return
            }

            logger.info("Exporting ${vaultFile.path} to ${localFile.canonicalPath}")
            if (verbose) {
                exporter.setVerbose(context.progressListener)
            }
            exporter.isNoMetaInf = true
            exporter.export(vaultFile)
            logger.info("Exporting done.")
        } finally {
            exporter?.close()
        }
    }

    /**
     * Imports content from the local file system to AEM.
     *
     * @param context Operation context containing all necessary information
     */
    private fun doImport(context: VltOperationContext) {
        val vaultFsApp = initVaultFsApp(context)

        val verbose = true
        val addr = RepositoryAddress(context.serverConfig.url + "/crx")
        val localFile = vaultFsApp.getPlatformFile(context.localAbsPath, false)
        val vCtx = vaultFsApp.createVaultContext(localFile)
        vCtx.isVerbose = verbose
        val vaultFile = vCtx.getFileSystem(addr).getFile(context.jcrPath)
        logger.info("Importing ${localFile.canonicalPath} to ${vaultFile.path}")

        var archive: Archive? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            archive = FileArchive(localFile)
            archive.open(false)
            val importer = Importer()
            if (verbose) {
                importer.options.listener = context.progressListener
            }
            val session = vaultFile.fileSystem.aggregateManager.session
            importer.run(archive, session, vaultFile.path)
            logger.info("Importing done.")
        } finally {
            archive?.close()
        }
    }

    private fun initVaultFsApp(context: VltOperationContext): CustomizedVaultFsApp {
        val vaultFsApp = vaultAppFactory.createVaultApp(context)
        vaultFsApp.init()
        return vaultFsApp
    }

    private fun withPluginClassLoader(callback: () -> Unit) {
        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader
        val pluginClassLoader = this.javaClass.classLoader
        try {
            currentThread.contextClassLoader = pluginClassLoader
            callback()
        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }

    private val vaultAppFactory: IVaultAppFactory = VaultAppFactory
}
