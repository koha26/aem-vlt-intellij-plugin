package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.util.application
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.facade.impl.FileVaultFacadeStub
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.settings.AEMServerSettings

class PullActionTest : BasePlatformTestCase() {

    private lateinit var pullAction: PullAction
    private lateinit var fileVaultFacadeStub: FileVaultFacadeStub

    override fun getTestDataPath() = "src/test/testData/com/kdiachenko/aem/filevault/actions/pull"

    public override fun setUp() {
        super.setUp()

        fileVaultFacadeStub = FileVaultFacadeStub()
        project.registerServiceInstance(IFileVaultFacade::class.java, fileVaultFacadeStub)

        pullAction = PullAction()
    }

    fun testIcon() {
        val icon = pullAction.getIcon()

        assertNotNull(icon)
        assertEquals(com.intellij.icons.AllIcons.Vcs.Fetch, icon)
    }

    fun testActionUpdateThread() {
        assertEquals(ActionUpdateThread.BGT, pullAction.actionUpdateThread)
    }

    fun testActionPerformed() {
        val server = AEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = false
        )

        val serverSettings = AEMServerSettings()
        serverSettings.state.addServer(server)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        /*val file =
            myFixture.copyDirectoryToProject(
                "content/project/en/.content.xml",
                "main/content/jcr_root/content/project/en/.content.xml"
            )
        myFixture.configureByFile(file.path)*/
        myFixture.configureByFile("main/content/jcr_root/content/project/en/.content.xml")
        val action = createAnActionEvent()
        pullAction.actionPerformed(action)

        assertEquals(1, fileVaultFacadeStub.exportedFiles.size)
        assertEquals(
            "/src/main/content/jcr_root/content/project/en/.content.xml",
            fileVaultFacadeStub.exportedFiles[0]
        )
    }

    fun testActionPerformedFolder() {
        val server = AEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = false
        )

        val serverSettings = AEMServerSettings()
        serverSettings.state.addServer(server)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        /*val file =
            myFixture.copyDirectoryToProject(
                "content/project/en/.content.xml",
                "main/content/jcr_root/content/project/en/.content.xml"
            )*/
        val file = myFixture.configureByFile("main/content/jcr_root/content/project/en/.content.xml")
        val action = createAnActionEvent(file.virtualFile.parent)
        pullAction.actionPerformed(action)

        assertEquals(1, fileVaultFacadeStub.exportedFiles.size)
        assertEquals("/src/main/content/jcr_root/content/project/en", fileVaultFacadeStub.exportedFiles[0])
    }

    fun testUpdate() {
        val server = AEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = false
        )

        val serverSettings = AEMServerSettings()
        serverSettings.state.addServer(server)
        application.registerServiceInstance(AEMServerSettings::class.java, serverSettings)

        /*val file =
            myFixture.copyDirectoryToProject(
                "src/main/content/jcr_root/content/project/en/.content.xml",
                "main/content/jcr_root/content/project/en/.content.xml"
            )*/
        myFixture.configureByFile("main/content/jcr_root/content/project/en/.content.xml")
        //myFixture.configureByFile(file.path)
        val action = createAnActionEvent()
        pullAction.update(action)

        assertTrue(action.presentation.isEnabledAndVisible)
        assertEquals(com.intellij.icons.AllIcons.Vcs.Fetch, action.presentation.icon)
    }

    fun createAnActionEvent(): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext())

    fun createAnActionEvent(virtualFile: VirtualFile): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext(virtualFile))

    fun createDataContext(): DataContext {
        return createDataContext(myFixture.file.virtualFile)
    }

    fun createDataContext(virtualFile: VirtualFile): DataContext {
        return DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> myFixture.project
                CommonDataKeys.EDITOR.name -> myFixture.editor
                CommonDataKeys.VIRTUAL_FILE.name -> virtualFile
                else -> null
            }
        }
    }
}
