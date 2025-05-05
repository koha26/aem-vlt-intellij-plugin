package com.kdiachenko.aem.filevault.service.dto

data class VltBasicParams(
    val jcrPath: String,
    val localPath: String,
    val mountPoint: String,
)
