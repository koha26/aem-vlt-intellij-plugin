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

fun AEMServerConfig.toLazyDetailed(): DetailedAEMServerConfig {
    val detailedAEMServerConfig = DetailedAEMServerConfig(
        id, name, url, isDefault
    )
    CredentialsManager.getInstance().getAsync(id).onSuccess {
        detailedAEMServerConfig.username = it?.userName ?: ""
        detailedAEMServerConfig.password = it?.getPasswordAsString() ?: ""
    }
    return detailedAEMServerConfig
}

fun AEMServerConfig.toDetailed(): DetailedAEMServerConfig {
    val credentials = CredentialsManager.getInstance().get(id)
    val detailedAEMServerConfig = DetailedAEMServerConfig(
        id, name, url, isDefault,
        credentials?.userName ?: "", credentials?.getPasswordAsString() ?: ""
    )
    return detailedAEMServerConfig
}

fun List<AEMServerConfig>.toLazyDetailed(): List<DetailedAEMServerConfig> {
    return this.map { it.toLazyDetailed() }
}
