package com.kdiachenko.aem.filevault.model

import com.kdiachenko.aem.filevault.settings.service.CredentialsManager
import java.util.UUID

data class DetailedAEMServerConfig(
    override var id: String = UUID.randomUUID().toString(),
    override var name: String = "",
    override var url: String = "",
    override var isDefault: Boolean = false,
    var username: String = "",
    var password: String = ""
) : AEMServerConfig(id, name, url, isDefault)

fun AEMServerConfig.toDetailed(): DetailedAEMServerConfig {
    val credentials = CredentialsManager.get(id)
    return DetailedAEMServerConfig(id, name, url, isDefault,
        credentials?.userName ?: "", credentials?.getPasswordAsString() ?: "")
}

fun List<AEMServerConfig>.toDetailed(): List<DetailedAEMServerConfig> {
    return this.map { it.toDetailed() }
}
