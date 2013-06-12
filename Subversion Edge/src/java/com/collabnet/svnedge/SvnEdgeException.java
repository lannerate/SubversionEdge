package com.collabnet.svnedge;

/**
 * The base class for all Edge specific exceptions.  
 * This is done in Java vs. Groovy as the latter has an issue with an extra
 * ctor added to the Exception class in Java 7, when Edge is compiled using
 * a 1.6 JDK.
 */
public class SvnEdgeException extends Exception {

    public SvnEdgeException() {
    }

    public SvnEdgeException(String message) {
        super(message);
    }

    public SvnEdgeException(Throwable cause) {
        super(cause);
    }

    public SvnEdgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
