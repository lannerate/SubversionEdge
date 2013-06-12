package com.collabnet.svnedge.integration.command;

/**
 * 
 * The command state represents the state of each command.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public enum CommandState {

    /**
     * When a command is first scheduled in the scheduler, after being 
     * received from the CTF manager.
     */
    SCHEDULED, 
    /**
     * When the command is selected to be run by the scheduler.
     */
    RUNNING, 
    /**
     * When the command has finished executing.
     */
    TERMINATED, 
    /**
     * When the command result has been successfully reported to CTF.
     */
    REPORTED
}
