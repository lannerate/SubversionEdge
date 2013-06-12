package com.collabnet.svnedge.domain

import com.collabnet.svnedge.domain.Wizard

class WizardStep {
    //static belongsTo = [wizard: Wizard]
    int wizardId
    String helperClassName
    String label
    boolean done
    int rank
    
    public helper() {
        return Class.forName(helperClassName, true, this.class.classLoader).newInstance()
    }
}
