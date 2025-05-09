package com.kdiachenko.aem.filevault.model

import java.io.Serializable
import java.net.MalformedURLException
import java.net.URI
import java.util.*

/**
 * Model class representing an AEM server configuration
 */
open class AEMServerConfig(
    open var id: String = UUID.randomUUID().toString(),
    open var name: String = "",
    open var url: String = "",
    open var isDefault: Boolean = false
) : Serializable
