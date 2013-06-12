package com.collabnet.svnedge.util

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.collabnet.svnedge.console.CommandLineService.COMMAND_TERMINATED

/**
 * An instance of this class is created as an output result of RealTimeCommandLineService.execute(command)
 * 
 * @author Marcello de Sales (marcello.desales@gmail.com)
 *
 */
public class CommandLineOutputListener {

    /**
     * The command lines as a blocking queue.
     */
    private BlockingQueue<String> commandLines
    /**
     * While there are still lines to be output from the command.
     */
    def hasMore = true

    /**
     * Creates a new instance of this class with the given reference to the 
     * command lines queue.
     * @param commandLinesQueue
     */
    public CommandLineOutputListener(BlockingQueue<String> cmdLinesQueue) {
        this.commandLines = cmdLinesQueue
    }

    /**
     * @return whether or not this listener finished reading the lines.
     */
    def hasMoreOutputLine() {
        return hasMore
    }

    /**
     * @return The next output line from the log. This method blocks when
     * until a command line is available. 
     * If the command is called after the line has been available, the method
     * returns null. Use the following idiom:
     * <pre>
     *  while ((line = executor.getNextOutputLine()) != null) {
             "not-null line reference".
        }
     * </pre>
     */
    def synchronized getNextOutputLine() {
        def line = commandLines.take()
        return line.equals(COMMAND_TERMINATED) ? null : line
    }

}
