package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.testFramework.unregisterService
import com.intellij.util.application
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterStatus
import com.kdiachenko.aem.filevault.integration.service.INotificationService
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterModificationService
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterService
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import com.kdiachenko.aem.filevault.stubs.NotificationEntry
import com.kdiachenko.aem.filevault.stubs.NotificationServiceStub
import com.kdiachenko.aem.filevault.stubs.FileVaultFacadeStub
import com.kdiachenko.aem.filevault.stubs.WorkspaceFilterModificationServiceStub
import com.kdiachenko.aem.filevault.stubs.WorkspaceFilterServiceStub
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class PushActionTest : BasePlatformTestCase() {

    private lateinit var pushAction: PushAction
    private lateinit var fileVaultFacadeStub: FileVaultFacadeStub
    private lateinit var serverSettings: AEMServerSettings
    private val testServer = AEMServerConfig(
        id = "test-id",
        name = "Test Server",
        url = "http://localhost:4502",
        isDefault = false
    )

    override fun getTestDataPath() = "src/test/testData/com/kdiachenko/aem/filevault/actions/push"

    public override fun setUp() {
        super.setUp()
        setupFileVaultFacade()
        setupServerSettings()
        pushAction = PushAction()
    }

    override fun tearDown() {
        try {
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }

    fun testIcon() {
        val icon = pushAction.getIcon()

        assertNotNull(icon)
        assertEquals(com.intellij.icons.AllIcons.Vcs.Push, icon)
    }

    fun testActionUpdateThread() {
        assertEquals(ActionUpdateThread.BGT, pushAction.actionUpdateThread)
    }

    fun testActionPerformedOnFile() {
        setupTestFiles()
        val action = createAnActionEvent()
        pushAction.actionPerformed(action)

        assertEquals(1, fileVaultFacadeStub.importedFiles.size)
        assertEquals(
            "/src/content/jcr_root/content/project/en/.content.xml",
            fileVaultFacadeStub.importedFiles[0]
        )
    }

    fun testActionPerformedOnFolder() {
        setupTestFiles()
        val file = myFixture.file.virtualFile.parent
        val action = createAnActionEvent(file)
        pushAction.actionPerformed(action)

        assertEquals(1, fileVaultFacadeStub.importedFiles.size)
        assertEquals("/src/content/jcr_root/content/project/en", fileVaultFacadeStub.importedFiles[0])
    }

    fun testActionPerformedOnFileWithNonDefaultServer() {
        serverSettings = AEMServerSettings()
        serverSettings.state.configuredServers.add(
            AEMServerConfig(
                id = "test-id-1",
                name = "AEM Author",
                url = "http://localhost:4502",
                isDefault = false
            )
        )
        serverSettings.state.configuredServers.add(
            AEMServerConfig(
                id = "test-id-2",
                name = "AEM Publisher",
                url = "http://localhost:4503",
                isDefault = true
            )
        )
        application.unregisterService(AEMServerSettings::class.java)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        setupTestFiles()
        val action = createAnActionEvent()
        pushAction.actionPerformed(action)

        assertEquals(1, fileVaultFacadeStub.importedFiles.size)
    }

    fun testActionPerformedOnFileWithoutServer() {
        serverSettings = AEMServerSettings()
        application.unregisterService(AEMServerSettings::class.java)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        setupTestFiles()
        val action = createAnActionEvent()

        val exception = org.junit.jupiter.api.assertThrows<RuntimeException>({ pushAction.actionPerformed(action) })

        assertEquals(0, fileVaultFacadeStub.exportedFiles.size)
        assertEquals("No AEM servers configured. Please add servers in Settings | AEM VLT Settings.", exception.message)
    }

    fun testActionPerformedOnFileWithMultipleServers() {
        serverSettings = AEMServerSettings()
        serverSettings.state.configuredServers.add(
            AEMServerConfig(
                id = "test-id-1",
                name = "AEM Author",
                url = "http://localhost:4502",
                isDefault = false
            )
        )
        serverSettings.state.configuredServers.add(
            AEMServerConfig(
                id = "test-id-2",
                name = "AEM Publisher",
                url = "http://localhost:4503",
                isDefault = false
            )
        )
        application.unregisterService(AEMServerSettings::class.java)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        setupTestFiles()
        val action = createAnActionEvent()
        val exception = org.junit.jupiter.api.assertThrows<RuntimeException> { pushAction.actionPerformed(action) }

        assertEquals(0, fileVaultFacadeStub.exportedFiles.size)
        assertEquals("No default AEM servers configured. Please mark desired server as default in Settings | AEM VLT Settings.", exception.message)
    }

    fun testActionPerformedWithNullProject() {
        setupTestFiles()
        val action = createAnActionEvent(DataContext {
            when (it) {
                else -> null
            }
        })
        pushAction.actionPerformed(action)

        assertEquals(0, fileVaultFacadeStub.exportedFiles.size)
    }

    fun testActionPerformedWithNullVirtualFile() {
        setupTestFiles()
        val action = createAnActionEvent(DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> myFixture.project
                else -> null
            }
        })
        pushAction.actionPerformed(action)

        assertEquals(0, fileVaultFacadeStub.exportedFiles.size)
    }

    fun testUpdateWithFileUnderJcrRoot() {
        setupTestFiles()
        val action = createAnActionEvent()
        pushAction.update(action)

        assertTrue(action.presentation.isEnabledAndVisible)
        assertEquals(com.intellij.icons.AllIcons.Vcs.Push, action.presentation.icon)
    }

    fun testUpdateWithFileNotUnderJcrRoot() {
        myFixture.copyDirectoryToProject(
            "common/en",
            "content/content/project/en"
        )
        myFixture.configureByFile("content/content/project/en/.content.xml")
        val action = createAnActionEvent()
        pushAction.update(action)

        assertFalse(action.presentation.isEnabledAndVisible)
    }

    fun testUpdateWithNullProject() {
        myFixture.copyDirectoryToProject(
            "common/en",
            "content/content/project/en"
        )
        myFixture.configureByFile("content/content/project/en/.content.xml")
        val action = createAnActionEvent(DataContext {
            when (it) {
                else -> null
            }
        })
        pushAction.update(action)

        assertFalse(action.presentation.isEnabledAndVisible)
    }

    fun testUpdateWithNullVirtualFile() {
        myFixture.copyDirectoryToProject(
            "common/en",
            "content/content/project/en"
        )
        myFixture.configureByFile("content/content/project/en/.content.xml")
        val action = createAnActionEvent(DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> myFixture.project
                else -> null
            }
        })
        pushAction.update(action)

        assertFalse(action.presentation.isEnabledAndVisible)
    }

    fun testUpdateDoesNotEvaluateFilter() {
        setupTestFiles()
        val filterStub = setupWorkspaceFilterService(WorkspaceFilterStatus.FULLY_INCLUDED)
        val event = createAnActionEvent()

        pushAction.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
        assertEquals(0, filterStub.evaluateCalls.size)
    }

    fun testBlockedPreflightDoesNotCallFacade() {
        setupTestFiles()
        setupWorkspaceFilterService(WorkspaceFilterStatus.OUTSIDE_FILTER)
        setupWorkspaceFilterModificationService()
        TestDialogManager.setTestDialog { 1 }

        pushAction.actionPerformed(createAnActionEvent())
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(fileVaultFacadeStub.importedFiles.isEmpty())
    }

    fun testBlockedPreflightCanAddSelectionToFilter() {
        val notifications = setupNotificationService()
        setupTestFiles()
        setupWorkspaceFilterService(WorkspaceFilterStatus.OUTSIDE_FILTER)
        val modification = setupWorkspaceFilterModificationService()
        TestDialogManager.setTestDialog { 0 }

        pushAction.actionPerformed(createAnActionEvent())
        PlatformTestUtil.waitWhileBusy { modification.appliedPreviews.isEmpty() && notifications.info.isEmpty() }

        assertTrue(fileVaultFacadeStub.importedFiles.isEmpty())
        assertEquals(1, modification.inspectSelections.size)
        assertEquals(1, modification.appliedPreviews.size)
        assertEquals(
            listOf(NotificationEntry("FileVault Filter Updated", "Added /content/project/en")),
            notifications.info,
        )
    }

    fun testPartialCancelDoesNotCallFacade() {
        setupTestFiles()
        setupWorkspaceFilterService(WorkspaceFilterStatus.PARTIALLY_INCLUDED)
        TestDialogManager.setTestDialog { 1 }

        pushAction.actionPerformed(createAnActionEvent())
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(fileVaultFacadeStub.importedFiles.isEmpty())
    }

    fun testPartialContinuePassesApprovalAndFingerprint() {
        setupTestFiles()
        val filter = setupWorkspaceFilterService(WorkspaceFilterStatus.PARTIALLY_INCLUDED, "abc123")
        TestDialogManager.setTestDialog { 0 }

        pushAction.actionPerformed(createAnActionEvent())
        PlatformTestUtil.waitWhileBusy { fileVaultFacadeStub.importedOptions.isEmpty() }

        assertEquals(WorkspaceFilterOperationOptions(true, "abc123"), fileVaultFacadeStub.importedOptions.single())
        assertEquals(1, filter.evaluateCalls.size)
    }

    fun testSuccessNotification() {
        val notificationServiceStub = setupNotificationService()
        setupTestFiles()

        val action = createAnActionEvent()
        pushAction.actionPerformed(action)

        PlatformTestUtil.waitWhileBusy {
            notificationServiceStub.info.isEmpty() && notificationServiceStub.error.isEmpty()
        }

        assertNotification(
            listOf(
                NotificationEntry("Push Successful", "Imported")
            ),
            notificationServiceStub.info
        )
    }

    fun testFailNotification() {
        val notificationServiceStub = setupNotificationService()
        setupFailingFileVaultFacade()
        setupTestFiles()

        val action = createAnActionEvent()
        pushAction.actionPerformed(action)

        PlatformTestUtil.waitWhileBusy {
            notificationServiceStub.info.isEmpty() && notificationServiceStub.error.isEmpty()
        }

        assertNotification(
            listOf(
                NotificationEntry("Push Failed", "Failure message")
            ),
            notificationServiceStub.error
        )
    }

    private fun setupNotificationService(): NotificationServiceStub {
        val notificationServiceStub = NotificationServiceStub()
        project.registerServiceInstance(INotificationService::class.java, notificationServiceStub)
        return notificationServiceStub
    }

    private fun setupFailingFileVaultFacade() {
        fileVaultFacadeStub = object : FileVaultFacadeStub() {
            override fun importContent(
                serverConfig: DetailedAEMServerConfig,
                projectLocalFile: File,
                indicator: ProgressIndicator,
                options: WorkspaceFilterOperationOptions,
            ): CompletableFuture<DetailedOperationResult> {
                return CompletableFuture.supplyAsync {
                    DetailedOperationResult(false, "Failure message", listOf())
                }
            }
        }
        project.registerServiceInstance(IFileVaultFacade::class.java, fileVaultFacadeStub)
    }

    private fun assertNotification(
        expectedNotificationEntries: List<NotificationEntry>,
        actualNotificationEntries: List<NotificationEntry>
    ) {
        assertEquals(expectedNotificationEntries.size, actualNotificationEntries.size)
        assertEquals(expectedNotificationEntries, actualNotificationEntries)
    }

    private fun setupFileVaultFacade() {
        fileVaultFacadeStub = FileVaultFacadeStub()
        project.registerServiceInstance(IFileVaultFacade::class.java, fileVaultFacadeStub)
    }

    private fun setupServerSettings() {
        serverSettings = AEMServerSettings()
        serverSettings.state.addServer(testServer)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)
    }

    private fun setupWorkspaceFilterService(
        status: WorkspaceFilterStatus,
        filterFingerprint: String? = "fingerprint",
    ): WorkspaceFilterServiceStub {
        val stub = WorkspaceFilterServiceStub()
        val selection = Path.of(myFixture.file.virtualFile.path)
        when (status) {
            WorkspaceFilterStatus.FULLY_INCLUDED,
            WorkspaceFilterStatus.PARTIALLY_INCLUDED -> stub.allow(selection, status, filterFingerprint)
            else -> stub.blockWith(status, selection)
        }
        application.unregisterService(IWorkspaceFilterService::class.java)
        application.registerServiceInstance(IWorkspaceFilterService::class.java, stub)
        return stub
    }

    private fun setupWorkspaceFilterModificationService(): WorkspaceFilterModificationServiceStub {
        val stub = WorkspaceFilterModificationServiceStub()
        val selection = myFixture.file.virtualFile
        val selectionPath = Path.of(selection.path)
        val context = requireNotNull(selectionPath.resolveContentPackage())
        stub.inspectResult = FilterModificationPreview(
            status = FilterModificationStatus.ADDED,
            selection = selection,
            context = context,
            jcrPath = selectionPath.toNormalizedJcrPath(context),
            filterFingerprint = "fingerprint",
            message = "Add /content/project/en to filter.xml.",
        )
        stub.applyResult = FilterModificationResult(
            status = FilterModificationStatus.ADDED,
            filterFile = null,
            message = "Added /content/project/en",
        )
        project.registerServiceInstance(IWorkspaceFilterModificationService::class.java, stub)
        return stub
    }

    private fun setupTestFiles() {
        myFixture.copyDirectoryToProject(
            "common/en",
            "content/jcr_root/content/project/en"
        )
        myFixture.configureByFile("content/jcr_root/content/project/en/.content.xml")
        setupWorkspaceFilterService(WorkspaceFilterStatus.FULLY_INCLUDED)
    }

    fun createAnActionEvent(): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext())

    fun createAnActionEvent(dataContext: DataContext): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, dataContext)

    fun createAnActionEvent(virtualFile: VirtualFile): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext(virtualFile))

    fun createDataContext(): DataContext =
        createDataContext(myFixture.file.virtualFile)

    fun createDataContext(virtualFile: VirtualFile): DataContext =
        DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> myFixture.project
                CommonDataKeys.EDITOR.name -> myFixture.editor
                CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                else -> null
            }
        }
}
