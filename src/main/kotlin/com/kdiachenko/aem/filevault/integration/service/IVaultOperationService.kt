package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.nio.file.Path

/**
 * Service for executing Vault operations with the Vault FS App.
 */
interface IVaultOperationService {

    /**
     * Exports content from AEM to the local file system.
     *
     * @param context Operation context containing all necessary information
     */
    fun export(context: VltOperationContext)

    /**
     * Imports content from the local file system to AEM.
     *
     * @param context Operation context containing all necessary information
     */
    fun import(context: VltOperationContext)
}
