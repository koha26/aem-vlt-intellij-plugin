package com.kdiachenko.aem.filevault.integration.listener

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.dto.OperationEntryDetail
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener
import org.apache.jackrabbit.vault.util.DefaultProgressListener

class OperationProgressTrackerListener : ProgressTrackerListener {
    private val default = DefaultProgressListener()
    val entries = mutableListOf<OperationEntryDetail>()

    override fun onMessage(
        mode: ProgressTrackerListener.Mode?,
        action: String,
        path: String
    ) {
        default.onMessage(mode, action, path)
        entries += OperationEntryDetail(OperationAction.Companion.fromString(action), path)
    }

    override fun onError(
        mode: ProgressTrackerListener.Mode?,
        path: String,
        e: Exception
    ) {
        default.onError(mode, path, e)
        entries += OperationEntryDetail(OperationAction.ERROR, path, e.message)
    }

}
