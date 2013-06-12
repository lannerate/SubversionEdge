/* copyright */
package com.collabnet.svnedge.integration

import com.collabnet.svnedge.SvnEdgeException

/**
 * This class represents an exception converting the system to
 * Replica mode
 */ 
class ReplicaConversionException extends SvnEdgeException {
    
    public ReplicaConversionException() {
        super()
    }

    
    public ReplicaConversionException(String msg) {
        super(msg)
    }
}
