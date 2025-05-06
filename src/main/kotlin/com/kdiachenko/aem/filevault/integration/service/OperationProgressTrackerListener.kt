package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener
import org.apache.jackrabbit.vault.util.DefaultProgressListener

class OperationProgressTrackerListener : ProgressTrackerListener {
    private val default = DefaultProgressListener()
    val entries = mutableListOf<OperationEntry>()

    override fun onMessage(
        mode: ProgressTrackerListener.Mode?,
        action: String,
        path: String
    ) {
        default.onMessage(mode, action, path)
        entries += OperationEntry(OperationAction.fromString(action), path)
    }

    override fun onError(
        mode: ProgressTrackerListener.Mode?,
        path: String,
        e: Exception
    ) {
        default.onError(mode, path, e)
        entries += OperationEntry(OperationAction.ERROR, path, e.message)
    }

}
