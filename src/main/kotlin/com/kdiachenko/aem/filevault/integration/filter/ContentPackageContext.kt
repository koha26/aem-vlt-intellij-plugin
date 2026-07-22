package com.kdiachenko.aem.filevault.integration.filter

import java.nio.file.Path

data class ContentPackageContext(
    val packageRoot: Path,
    val jcrRoot: Path,
    val filterFile: Path,
)
