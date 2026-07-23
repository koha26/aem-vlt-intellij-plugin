package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import com.kdiachenko.aem.filevault.integration.service.INotificationService
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterModificationService
import com.kdiachenko.aem.filevault.stubs.NotificationEntry
import com.kdiachenko.aem.filevault.stubs.NotificationServiceStub
import com.kdiachenko.aem.filevault.stubs.WorkspaceFilterModificationServiceStub
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import java.nio.file.Files
import java.nio.file.Path

class AddToFileVaultFilterActionTest : BasePlatformTestCase() {
    private lateinit var action: AddToFileVaultFilterAction

    override fun setUp() {
        super.setUp()
        action = AddToFileVaultFilterAction()
    }

    override fun tearDown() {
        try {
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    fun testUpdateVisibleForContentPackageSelection() {
        val selection = createSelection()
        val event = createActionEvent(selection)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
        assertEquals("Add to FileVault Filter", event.presentation.text)
    }

    fun testUpdateHiddenForJcrRootSelection() {
        val jcrRoot = createJcrRoot()
        val event = createActionEvent(jcrRoot)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionPerformedAppliesAddedPreviewAndShowsNotification() {
        val selection = createSelection()
        val notifications = setupNotificationService()
        val stub = setupModificationService(
            selection = selection,
            previewStatus = FilterModificationStatus.ADDED,
            resultStatus = FilterModificationStatus.ADDED,
            message = "Added /apps/site/components",
        )

        action.actionPerformed(createActionEvent(selection))
        PlatformTestUtil.waitWhileBusy { stub.appliedPreviews.isEmpty() && notifications.info.isEmpty() }

        assertEquals(1, stub.inspectSelections.size)
        assertEquals(1, stub.appliedPreviews.size)
        assertEquals(
            listOf(NotificationEntry("FileVault Filter Updated", "Added /apps/site/components")),
            notifications.info,
        )
    }

    fun testActionPerformedHonorsExpansionConfirmationCancel() {
        val selection = createSelection()
        val stub = setupModificationService(
            selection = selection,
            previewStatus = FilterModificationStatus.EXPANSION_CONFIRMATION_REQUIRED,
            resultStatus = FilterModificationStatus.ADDED,
            message = "Expands scope",
        )
        TestDialogManager.setTestDialog { 1 }

        action.actionPerformed(createActionEvent(selection))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(1, stub.inspectSelections.size)
        assertTrue(stub.appliedPreviews.isEmpty())
    }

    fun testActionPerformedShowsUnchangedInfoWithoutApply() {
        val selection = createSelection()
        val notifications = setupNotificationService()
        val stub = setupModificationService(
            selection = selection,
            previewStatus = FilterModificationStatus.ALREADY_COVERED,
            resultStatus = FilterModificationStatus.ALREADY_COVERED,
            message = "Already covered",
        )

        action.actionPerformed(createActionEvent(selection))
        PlatformTestUtil.waitWhileBusy { notifications.info.isEmpty() }

        assertEquals(1, stub.inspectSelections.size)
        assertTrue(stub.appliedPreviews.isEmpty())
        assertEquals(
            listOf(NotificationEntry("FileVault Filter Unchanged", "Already covered")),
            notifications.info,
        )
    }

    private fun setupNotificationService(): NotificationServiceStub {
        val stub = NotificationServiceStub()
        project.registerServiceInstance(INotificationService::class.java, stub)
        return stub
    }

    private fun setupModificationService(
        selection: VirtualFile,
        previewStatus: FilterModificationStatus,
        resultStatus: FilterModificationStatus,
        message: String,
    ): WorkspaceFilterModificationServiceStub {
        val stub = WorkspaceFilterModificationServiceStub()
        val selectionPath = Path.of(selection.path)
        val context = requireNotNull(selectionPath.resolveContentPackage())
        val jcrPath = selectionPath.toNormalizedJcrPath(context)
        stub.inspectResult = FilterModificationPreview(
            status = previewStatus,
            selection = selection,
            context = context,
            jcrPath = jcrPath,
            filterFingerprint = "fingerprint",
            message = message,
        )
        stub.applyResult = FilterModificationResult(
            status = resultStatus,
            filterFile = null,
            message = message,
        )
        project.registerServiceInstance(IWorkspaceFilterModificationService::class.java, stub)
        return stub
    }

    private fun createSelection(): VirtualFile {
        val selection = packageRoot().resolve("jcr_root/apps/site/components")
        Files.createDirectories(selection)
        return refresh(selection)
    }

    private fun createJcrRoot(): VirtualFile {
        val jcrRoot = packageRoot().resolve("jcr_root")
        Files.createDirectories(jcrRoot)
        return refresh(jcrRoot)
    }

    private fun packageRoot(): Path = Path.of(requireNotNull(project.basePath)).resolve("ui.apps")

    private fun refresh(path: Path): VirtualFile =
        requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path))

    private fun createActionEvent(virtualFile: VirtualFile): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext(virtualFile))

    private fun createDataContext(virtualFile: VirtualFile): DataContext =
        DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                else -> null
            }
        }
}
