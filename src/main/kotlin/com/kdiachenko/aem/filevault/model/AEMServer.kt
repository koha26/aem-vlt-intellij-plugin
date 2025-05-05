package com.kdiachenko.aem.filevault.model

import java.io.Serializable

/**
 * Model class representing an AEM server configuration
 */
data class AEMServer(
    var name: String = "",
    var url: String = "",
    var username: String = "",
    var password: String = "",
    var isDefault: Boolean = false
) : Serializable {
    override fun toString(): String = name
}
