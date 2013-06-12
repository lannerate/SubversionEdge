package com.collabnet.svnedge.util

import org.apache.log4j.Logger;

/**
 * Executes the supplied Runnable within a loop until stopped via interrupt
 */
public abstract class InterruptibleLoopRunnable implements Runnable {
    def log = Logger.getLogger(this.getClass())
    
    public void run() {
        while (!Thread.interrupted()) {
            try {
                loop()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
            } catch (Exception e) {
                log.error "Unexpected exception in loop.", e
            }
        }
        log.info("Shutting down after interrupt")
    }
    
    protected abstract void loop()
}
