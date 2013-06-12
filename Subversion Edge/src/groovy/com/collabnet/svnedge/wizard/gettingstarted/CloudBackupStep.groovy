/*
* CollabNet Subversion Edge
* Copyright 2012, CollabNet Inc. All rights reserved.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.collabnet.svnedge.wizard.gettingstarted

import com.collabnet.svnedge.wizard.WizardStepHelper

class CloudBackupStep extends WizardStepHelper {
    
    public CloudBackupStep() {
        this.targetController = 'setupCloudServices'
        this.targetAction = 'index'
        this.alternateActions << 'getStarted' << 'credentials' << 
                'createAccount' << 'updateCredentials' << 'removeCredentials'
        this.forceTarget = true
    }

    public String getContentTemplate(String controller, String action) {
        def prefix = '/gettingStartedWizard/'
        def template = prefix + 'cloudBackup'
        return template
    }
}
