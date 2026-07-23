package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlFile
import com.kdiachenko.aem.filevault.i18n.Messages
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterModificationService
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@Service(Service.Level.PROJECT)
class WorkspaceFilterModificationService(private val project: Project) : IWorkspaceFilterModificationService {
    private val logger = Logger.getInstance(WorkspaceFilterModificationService::class.java)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): IWorkspaceFilterModificationService =
            project.getService(IWorkspaceFilterModificationService::class.java)
    }

    override fun inspect(selection: VirtualFile): FilterModificationPreview {
        val selectedPath = Path.of(selection.path)
        val context = selectedPath.resolveContentPackage()
            ?: return preview(
                status = FilterModificationStatus.NOT_APPLICABLE,
                selection = selection,
                context = null,
                jcrPath = null,
                fingerprint = null,
                message = "Selection is not under jcr_root.",
            )
        val jcrPath = selectedPath.toNormalizedJcrPath(context)
        if (jcrPath == "/") {
            return preview(
                status = FilterModificationStatus.NOT_APPLICABLE,
                selection = selection,
                context = context,
                jcrPath = jcrPath,
                fingerprint = null,
                message = "Adding root scope is disabled.",
            )
        }
        if (!Files.exists(context.filterFile)) {
            return preview(
                status = FilterModificationStatus.ADDED,
                selection = selection,
                context = context,
                jcrPath = jcrPath,
                fingerprint = null,
                message = "Create filter.xml and add $jcrPath.",
            )
        }

        val bytes = readCurrentPsiBytes(context.filterFile)
        val fingerprint = sha256(bytes)
        val filter = try {
            DefaultWorkspaceFilter().apply {
                load(ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            logger.info(
                "Failed to parse FileVault filter ${context.filterFile}: ${e.javaClass.simpleName}: ${e.message}",
            )
            return preview(
                status = FilterModificationStatus.FILTER_INVALID,
                selection = selection,
                context = context,
                jcrPath = jcrPath,
                fingerprint = fingerprint,
                message = e.message ?: "Invalid filter.xml",
            )
        }
        val exact = filter.filterSets.firstOrNull { it.root == jcrPath }
        if (exact != null) {
            return preview(
                status = FilterModificationStatus.EXACT_ROOT_EXISTS,
                selection = selection,
                context = context,
                jcrPath = jcrPath,
                fingerprint = fingerprint,
                message = "$jcrPath already has a filter entry.",
            )
        }
        val unconditionalBroader = filter.filterSets.any { it.covers(jcrPath) && it.entries.isEmpty() }
        if (unconditionalBroader) {
            return preview(
                status = FilterModificationStatus.ALREADY_COVERED,
                selection = selection,
                context = context,
                jcrPath = jcrPath,
                fingerprint = fingerprint,
                message = "$jcrPath is already covered.",
            )
        }
        val expands = filter.filterSets.any { it.covers(jcrPath) || it.root.startsWith("$jcrPath/") }
        return preview(
            status = if (expands) {
                FilterModificationStatus.EXPANSION_CONFIRMATION_REQUIRED
            } else {
                FilterModificationStatus.ADDED
            },
            selection = selection,
            context = context,
            jcrPath = jcrPath,
            fingerprint = fingerprint,
            message = if (expands) {
                "Adding $jcrPath expands existing filtered scope."
            } else {
                "Add $jcrPath to filter.xml."
            },
        )
    }

    override fun apply(preview: FilterModificationPreview): FilterModificationResult {
        if (preview.status != FilterModificationStatus.ADDED &&
            preview.status != FilterModificationStatus.EXPANSION_CONFIRMATION_REQUIRED
        ) {
            return FilterModificationResult(preview.status, null, preview.message)
        }
        val context = requireNotNull(preview.context)
        val jcrPath = requireNotNull(preview.jcrPath)
        if (preview.filterFingerprint == null) {
            if (Files.exists(context.filterFile)) {
                return FilterModificationResult(
                    FilterModificationStatus.FILTER_CHANGED,
                    null,
                    "filter.xml changed; inspect again.",
                )
            }
        } else if (currentFingerprint(context.filterFile) != preview.filterFingerprint) {
            return FilterModificationResult(
                FilterModificationStatus.FILTER_CHANGED,
                null,
                "filter.xml changed; inspect again.",
            )
        }

        lateinit var result: FilterModificationResult
        WriteCommandAction.runWriteCommandAction(
            project,
            Messages.message("filter.add.command"),
            null,
            Runnable {
                val psiManager = PsiManager.getInstance(project)
                val filterVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(context.filterFile)
                val resultFile = if (filterVirtualFile == null) {
                    val packageVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(context.packageRoot)
                        ?: error("Package directory is unavailable")
                    val packageDirectory = psiManager.findDirectory(packageVirtualFile)
                        ?: error("Package directory is unavailable")
                    val metaInf = packageDirectory.findSubdirectory("META-INF") ?: packageDirectory.createSubdirectory("META-INF")
                    val vault = metaInf.findSubdirectory("vault") ?: metaInf.createSubdirectory("vault")
                    val created = PsiFileFactory.getInstance(project).createFileFromText(
                        "filter.xml",
                        XmlFileType.INSTANCE,
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <workspaceFilter version="1.0">
                            <filter root="${StringUtil.escapeXmlEntities(jcrPath)}"/>
                        </workspaceFilter>
                        """.trimIndent(),
                    )
                    (vault.add(created) as PsiFile).virtualFile
                } else {
                    val xmlFile = psiManager.findFile(filterVirtualFile) as? XmlFile ?: error("filter.xml is not XML")
                    PsiDocumentManager.getInstance(project).commitAllDocuments()
                    val root = xmlFile.rootTag ?: error("workspaceFilter root is missing")
                    require(root.name == "workspaceFilter")
                    val newTag = XmlElementFactory.getInstance(project).createTagFromText(
                        "<filter root=\"${StringUtil.escapeXmlEntities(jcrPath)}\"/>",
                    )
                    val inserted = root.addSubTag(newTag, false)
                    CodeStyleManager.getInstance(project).reformat(inserted)
                    PsiDocumentManager.getInstance(project).commitAllDocuments()
                    filterVirtualFile
                }
                logger.info("Added FileVault filter root $jcrPath to ${context.filterFile}")
                result = FilterModificationResult(
                    FilterModificationStatus.ADDED,
                    resultFile,
                    Messages.message("filter.add.success", jcrPath, context.filterFile.toString()),
                )
            },
        )
        return result
    }

    internal fun openFile(path: Path) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path) ?: return
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    private fun preview(
        status: FilterModificationStatus,
        selection: VirtualFile,
        context: com.kdiachenko.aem.filevault.integration.filter.ContentPackageContext?,
        jcrPath: String?,
        fingerprint: String?,
        message: String,
    ) = FilterModificationPreview(
        status = status,
        selection = selection,
        context = context,
        jcrPath = jcrPath,
        filterFingerprint = fingerprint,
        message = message,
    )

    private fun readCurrentPsiBytes(filterFile: Path): ByteArray = ReadAction.compute<ByteArray, RuntimeException> {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filterFile)
            ?: return@compute Files.readAllBytes(filterFile)
        val xmlFile = PsiManager.getInstance(project).findFile(virtualFile) as? XmlFile
            ?: return@compute Files.readAllBytes(filterFile)
        val text = PsiDocumentManager.getInstance(project).getDocument(xmlFile)?.text ?: xmlFile.text
        text.toByteArray(virtualFile.charset)
    }

    private fun currentFingerprint(path: Path): String? = ReadAction.compute<String?, RuntimeException> {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path) ?: return@compute null
        val xmlFile = PsiManager.getInstance(project).findFile(virtualFile) as? XmlFile ?: return@compute null
        val text = PsiDocumentManager.getInstance(project).getDocument(xmlFile)?.text ?: xmlFile.text
        sha256(text.toByteArray(virtualFile.charset))
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
