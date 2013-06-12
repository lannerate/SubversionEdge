package com.collabnet.svnedge;

/**
 * The base class for all Edge specific runtime exceptions.  
 * This is done in Java vs. Groovy as the latter has an issue with an extra
 * ctor added to the Exception class in Java 7, when Edge is compiled using
 * a 1.6 JDK.
 */
public class SvnEdgeRuntimeException extends RuntimeException {

    public SvnEdgeRuntimeException() {
    }

    public SvnEdgeRuntimeException(String message) {
        super(message);
    }

    public SvnEdgeRuntimeException(Throwable cause) {
        super(cause);
    }

    public SvnEdgeRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
