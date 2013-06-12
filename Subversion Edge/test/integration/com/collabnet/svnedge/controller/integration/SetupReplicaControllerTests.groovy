package com.collabnet.svnedge.controller.integration;

import org.junit.After
import org.junit.Before

import com.collabnet.svnedge.controller.AbstractSvnEdgeControllerTests
import com.collabnet.svnedge.domain.integration.ApprovalState
import com.collabnet.svnedge.domain.integration.CtfServer;
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration

class SetupReplicaControllerTests extends AbstractSvnEdgeControllerTests {
    
    @Before
    public void setUp() {
        super.setUp()
        // mock the bindData method
        controller.metaClass.bindData = { obj, params, excludes = [] ->
            obj.properties = params
        }
        
        ReplicaConfiguration config = new ReplicaConfiguration(
            name: 'test_replica',
            description: 'test_replica',
            systemId: 'exsy1010',
            approvalState: ApprovalState.APPROVED,
            commandPollRate: 5,
            maxLongRunningCmds: 2,
            maxShortRunningCmds: 10,
            commandRetryAttempts: 3,
            commandRetryWaitSeconds: 5)
        if (!config.save()) {
            println config.errors
        }

        CtfServer ctfServer = new CtfServer(baseUrl: "http://host.name.com")
        if (!ctfServer.save()) {
            println ctfServer.errors
        }
    }

    @After
    public void tearDown() {
    }

    void testEditConfig() {
        def model = controller.editConfig()
        assertNotNull "ReplicaConfiguration should not be null", model['config']
        assertNotNull "CTF URL is expected in model", model['ctfURL']
    }
    
    void testUpdateMonitoring() {
        def params = controller.params
        params.commandRetryAttempts = 3
        params.commandRetryWaitSeconds = 5
        controller.updateConfig()
        assertEquals "Expected redirect to 'editConfig' view on success",
                'editConfig', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error

        def model = controller.editConfig()
        ReplicaConfiguration config = model['config']
        assertEquals "Retry attempts was not set correctly", 3, 
                config.commandRetryAttempts
        assertEquals "Retry delay was not set correctly", 5, 
                config.commandRetryWaitSeconds
    }
    
    void testUpdateMonitoringFailRetryAttempts() {
        def params = controller.params
        params.commandRetryAttempts = 20
        params.commandRetryWaitSeconds = 5
        controller.updateConfig()
        assertNull "Not expected to redirect on error",
                controller.redirectArgs["action"]
        assertNull "Controller should NOT provide a success message",
                controller.flash.message
        assertNotNull "Controller should provide an error message",
                controller.request.error
    }
    
    void testUpdateMonitoringFailRetryWait() {
        def params = controller.params
        params.commandRetryAttempts = 3
        params.commandRetryWaitSeconds = 500
        controller.updateConfig()
        assertNull "Not expected to redirect on error",
                controller.redirectArgs["action"]
        assertNull "Controller should NOT provide a success message",
                controller.flash.message
        assertNotNull "Controller should provide an error message",
                controller.request.error
    }

}
