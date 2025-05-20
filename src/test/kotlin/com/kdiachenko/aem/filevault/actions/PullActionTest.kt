package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.util.application
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.facade.impl.FileVaultFacadeStub
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.settings.AEMServerSettings

class PullActionTest : BasePlatformTestCase() {

    private lateinit var pullAction: PullAction

    override fun getTestDataPath() = "src/test/testData/com/kdiachenko/aem/filevault/actions/pull"

    public override fun setUp() {
        super.setUp()

        project.registerServiceInstance(IFileVaultFacade::class.java, FileVaultFacadeStub())

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

        val file =
            myFixture.copyDirectoryToProject("src/main/content/jcr_root/content/project/en/.content.xml",
                "main/content/jcr_root/content/project/en/.content.xml")
        myFixture.configureByFile(file.path)
        val action = createAnActionEvent()
        pullAction.actionPerformed(action)
    }

    fun createAnActionEvent(): AnActionEvent =
        AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, createDataContext())

    fun createDataContext(): DataContext {
        return DataContext {
            when (it) {
                CommonDataKeys.PROJECT.name -> myFixture.project
                CommonDataKeys.EDITOR.name -> myFixture.editor
                CommonDataKeys.PSI_FILE.name -> myFixture.file
                CommonDataKeys.VIRTUAL_FILE.name -> myFixture.file.virtualFile
                else -> null
            }
        }
    }
}
