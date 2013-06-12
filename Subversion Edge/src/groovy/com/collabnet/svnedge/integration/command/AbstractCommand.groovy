/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.integration.command

import com.collabnet.svnedge.domain.integration.CommandResult;
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration

import java.util.HashMap;
import java.util.Map

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger
import grails.util.GrailsUtil
import com.collabnet.svnedge.util.ConfigUtil
import com.collabnet.svnedge.domain.integration.RepoStatus
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * Defines the Abstract Command to be instantiated based on the map
 * received from the Replica manager and executed by the Executor Service.
 *
 * After the execution of the command, a value is available that indicates if
 * the command succeeded or not. If a command failed, two exceptions might
 * be available: executionException and/or undoException.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 */
abstract class AbstractCommand {

    private static Logger log = Logger.getLogger(getClass())

    // synchronization object for state transition logging
    private static Object stateLogLock = new Object()

    /**
     * The command execution ID
     */
    protected String id
    /**
     * The current state of the command.
     */
    protected CommandState state
    /**
     * The command execution ID
     */
    protected String repoName
    /**
     * The final state of the command. If it is false, the params property
     * will contain an exception
     */
    protected Boolean succeeded
    /**
     * The originating exception thrown during the execution, if any occurs.
     */
    protected Throwable executionException
    /**
     * The originating exception thrown during the undo, if any occurs.
     */
    protected Throwable undoException
    /**
     * The parameters sent for the commands. These are updated with the values
     * for any exceptions and results.
     */
    protected Map<String, String> params
    /**
     * The execution context for commands containing the information needed
     * to communicate with the Replica Manager (TeamForge).
     */
    protected CommandsExecutionContext context
    /**
     * The view of the state transition from a given time command instance 
     * transitions from since the command has been loaded, executed and 
     * reported in nanoseconds precision.
     */
    protected Map<Long, CommandState> stateTimeTransitions
    /**
     * The view of the latest time of the state transition command instance 
     * transitions from since the command has been loaded, executed and 
     * reported in nanoseconds precision.
     */
    protected Map<CommandState, Long> stateTransitions

   /**
    * Output stream for writing the output of this command
    */
    protected OutputStream commandOutputStream;

    /**
     * Constructs a new abstract replica command.
     */
    def AbstractCommand() {
        succeeded = false
        params = new HashMap<String, String>()
        stateTransitions = new LinkedHashMap<CommandState, Long>();
        stateTimeTransitions = new LinkedHashMap<Long, CommandState>();
    }

    /**
     * Makes a new transition state for this command.
     */
    public void makeTransitionToState(CommandState newState) {
        def time = System.currentTimeMillis()
        def previousState = this.state
        logStateTransition(this, previousState, newState, time)
        this.state = newState
        this.stateTransitions.put(newState, time)
        this.stateTimeTransitions.put(time, newState)
    }

    @Override
    public String toString() {
        def paramsList = "params=" + params ? params : "[]"
        if (!succeeded) {
            return "${getClass().getSimpleName()}($id): ${this.state} : $paramsList"
        }
        return "${getClass().getSimpleName()}" +
            "($id-${succeeded?'suceeded':'failed'}): ${this.state} : $paramsList"
    }

    @Override
    public int hashCode() {
        final int prime = 31
        int result = 1
        def hash = this.id ? this.id.hashCode() : 0
        result = prime * result + hash
        return result
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj) {
            return false
        }
        if (getClass() != obj.getClass())
            return false
        AbstractCommand other = (AbstractCommand) obj
        if (id == null) {
            if (other.id != null)
                return false
        } else if (!id.equals(other.id))
            return false
        return true
    }

    /**
     * Transforms the given Command Map into an instance of an 
     * Abstract Replica Command given the commandMap[code] property.
     * @param commandMap is an instance of a Map with the properties of a
     * command.
     * @param executionContext is the execution context of the command
     * @return an instance of Abstract Replica Command.
     */
    def static AbstractCommand makeCommand(classLoader, commandMap) {
        if (!commandMap['code']) {
            throw new IllegalArgumentException("The name (code) of the " +
                "command is missing.")
        }
        def classObject = null
        try {
            classObject = loadCommandClass(classLoader, commandMap['code'])

        } catch (ClassNotFoundException clne) {
            commandMap['exception'] = 
                new CommandNotImplementedException(clne, commandMap['code'])
            commandMap['succeeded'] = false
            logExecution("LOAD-FAILED", commandMap, clne)
            return commandMap
        }
        def commandInstance = classObject.newInstance()

        commandInstance.init(commandMap.id, commandMap['params'], 
            commandMap.context)

        return commandInstance
    }

    /**
     * @param result is the command result.
     * @param state is the state to set in case the result does not have state
     * @return a new instance of a command that has been executed.
     */
    public static AbstractCommand makeCommand(CommandResult result, 
            CommandState state) {

        def classLoader = result.getClass().getClassLoader()
        def classObject = loadCommandClass(classLoader, result.commandCode)
        def commandInstance = classObject.newInstance()
        commandInstance.id = result.commandId
        // result.succeeded is boolean, but it must be null when not reported.
        if (result.succeeded != null) {
            commandInstance.succeeded = result.succeeded
            commandInstance.makeTransitionToState(state)

        } else {
            commandInstance.makeTransitionToState(state)
        }
        return commandInstance
    }

    /**
     * Loads a command class by the command code.
     */
    def static loadCommandClass(classLoader, commandCode) 
            throws ClassNotFoundException {

        def className = commandCode.capitalize() + "Command"
        log.debug("Instantiating command instance: ${className}")
        def commandPackage = "com.collabnet.svnedge.integration.command.impl"
        return classLoader.loadClass("$commandPackage.$className")
    }

    /**
     * Factory method to create the code based on the name of the class.
     * @param command
     * @return
     */
    def static makeCodeName(String simpleClassName) {
        // static classes
        if (simpleClassName.contains("\$")) {
            simpleClassName = simpleClassName.substring(
                simpleClassName.indexOf("\$") + 1)
        }
        def firstChar = simpleClassName.charAt(0).toString()
        def result = simpleClassName.replaceFirst(firstChar, 
            firstChar.toLowerCase())
        return result.replace("Command", "")
    }

    /**
     *  Factory method to create the code name based on the name of the class of
     *  a command.
     * @param command is an abstract command.
     * @return the name of the code. Ex. RepoAddCommand => repoAdd.
     */
    def static makeCodeName(command) {
        if (command instanceof AbstractCommand) {
            String className = command.getClass().getSimpleName()
            // static classes
            return makeCodeName(className)

        } else if (command instanceof Map){
            // the command instance is a map
            return command["code"]

        } else {
            return makeCodeName(command)
        }
    }

    /**
     * Initializes the command with the received parameters and the application
     * context.
     * @param initialParameter the initial parameters received for the command.
     * @param appCtx the application context used to acquire service instances.
     */
    def init(id, initialParameters, CommandsExecutionContext executionContext) {
        if (id == null) {
            throw new IllegalArgumentException("The command ID must be " +
                "provided.")
        }
        if (executionContext == null) {
            throw new IllegalArgumentException("The execution context must " +
                "be provided.")
        }
        this.context = executionContext
        this.id = id
        this.params = initialParameters

        // start the command execution output log for this instance
        if (ConfigurationHolder.config.svnedge.replica.logging.commandOutput)  {
            this.commandOutputStream = new FileOutputStream(getCommandOutputFile())
            writeCommmandOutputFileHeading()
        }

        log.debug("Instantiating the command " + getClass().getName() + 
                " with the parameters " + initialParameters)
    }

    /**
     * The command execution constraints that must be met before the command
     * is executed.
     */
    public abstract constraints() throws Throwable

    /**
     * The main command intent implementation.
     */
    public abstract execute() throws Throwable

    /**
     * The command execution constraints that must be met before the command
     * is executed.
     */
    public abstract undo() throws Throwable

    /**
     * Before the execution of a command, the method 'constraints()' is defined
     * to verify any pre-conditions that must be met before the execution of 
     * the implementing Command class. If the constraints fail, the method 
     * 'execute()' is NOT executed.
     * 
     * When the method 'constraints()' finishes, the method 'execute()' runs 
     * with the given parameters, as instructed by the method.
     * 
     * If any exception is thrown from the methods 'constraints()' and 
     * 'execute()', the properties of the params will include the exception, 
     * and the succeeded property contains a false value. Right after that, 
     * the undo() method is executed to undo anything done. It's important to 
     * clean anything that changed the state of the system by the 'execute()' 
     * method.
     * @throws CommandExecutionException if any Exception occurs while
     * executing the methods 'constraints()' or 'execute()'.
     */
    public final void run() throws CommandExecutionException {
        try {
            log.debug("Verifying the constraints for the command...")
            constraints()
            log.debug("Constraints passed... executing the command...")
            succeeded = doExecute()

        } catch (Throwable t) {
            succeeded = retryCommand()                        
            if (!succeeded) {
                executionException = t
                log.error("Failed to execute command: " + t.getMessage())
                if (t.cause) {
                    executionException = t.cause
                    logExecution("EXECUTION-EXCEPTION", t.cause)
                } else {
                    logExecution("EXECUTION-EXCEPTION", t)
                }
                doHandleExecutionException(executionException)
            }
        }

        if (ConfigurationHolder.config.svnedge.replica.logging.commandOutput)  {
            if (commandOutputStream) commandOutputStream.close()
            if (succeeded) getCommandOutputFile().delete()
        }

        makeTransitionToState(CommandState.TERMINATED)

        if (executionException) {
            try {
                log.debug("Undoing the command because the exception " +
                        "${executionException.getClass().getName()}: " + 
                        executionException.getMessage())
                logExecution("BEFORE-UNDO")
                undo()
                logExecution("AFTER-UNDO")
                log.debug("Undid the command successful...")

            } catch (Throwable t) {
                undoException = t
                logExecution("UNDO-EXCEPTION", t)
                log.error("Failed to undo the execution of the command: " +
                    t.getMessage())
            }
        }
        log.debug("Finished running command")

        if (executionException || undoException) {
            log.debug("Preparing to throw the exceptions: ")
            if (executionException) {
                log.debug(executionException.getClass().getName())
            }
            if (undoException) {
                log.debug(undoException.getClass().getName())
            }
            throw new CommandExecutionException(this, executionException,
                undoException)
        }
    }

    private boolean doExecute() {
        execute()
        log.debug("Command execution was successful...")
        return true
    }

    private boolean retryCommand() {
        boolean succeeded = false
        try {
            ReplicaConfiguration replicaConfig = 
                    ReplicaConfiguration.currentConfig
            if (replicaConfig) {
                int maxAttempts = replicaConfig.commandRetryAttempts
                long waitMillis = replicaConfig.commandRetryWaitSeconds * 1000
                for (int i = 0; !succeeded && i < maxAttempts; i++) {
                    log.debug "Command execution failed; retrying in " + 
                            waitMillis + " msec..."
                    Thread.sleep(waitMillis)
                    try {
                        succeeded = doExecute()
                    } catch (Throwable t) {
                        log.warn "Command retry #" + i + " failed. " + 
                                t.message
                    }
                }
            } 
        } catch (Throwable t) {
            log.warn("Replica command could not be retried.", t)
        }
        return succeeded
    }
    
    /**
     * Override to add command specific exception handling
     */
    protected void doHandleExecutionException(t) {
    }

    /**
     * @param serviceName the service name.
     * @return the instance of the service bean.
     */
    public getService(serviceName) {
        if (!context) {
            throw new IllegalStateException("The CommandsExecutionContext " +
                "must be provided with the Application Context property")
        }
        return context.appContext.getBean(serviceName)
    }

    /**
     * @param executionStep is a TOKEN of the execution step.
     */
    def logExecution(String executionStep) {
        logExecution(executionStep, this, null)
    }

    /**
     * @param executionStep is a TOKEN of the execution step
     * @param exception the instance of the exception thrown.
     */
    def logExecution(String executionStep, Throwable exception) {
        logExecution(executionStep, this, exception)
    }

    /**
     * @param executionStep is a TOKEN of the execution step
     * @param command the instance of the command.
     */
    def static logExecution(String executionStep, AbstractCommand command) {
        logExecution(executionStep, command, null)
    }

    /**
     * Logs the execution of a command into the file 
     * "data/logs/replica_cmds_YYYY_MM_DD.log".
     * @param executionStep is a TOKEN of the execution step
     * @param command is the instance of a replica command execution.
     * @param exception is an optional execution thrown.
     */
    def static logExecution(executionStep, command, exception) {
        File logFile = getExecutionLogFile(command?.context)
        if (!logFile) {
            def errorMsg = "Can't log replica commands: logs directory can't" +
                " be determined with context ${command?.context}..."
            log.error errorMsg
            throw new IllegalStateException(errorMsg)
        }
        if (!logFile.exists()) {
            try {
                FileUtils.touch(logFile);
                log.debug "Created the empty log file " + logFile

            } catch (Exception e) {
                log.error "Can't create the replica command log file " +
                    "$logFile: " + e.getMessage()
                return
            }
        }
        logFile.withWriterAppend("UTF-8") {

            def timeToken = String.format('%tH:%<tM:%<tS,%<tL', new Date())

            def logEntry = "${timeToken} ${command.id} " +
                "${command.class.simpleName} ${executionStep} params: " +
                    (command.params ? "${command.params}" : "[]")
            it.write(logEntry + "\n")

            if (exception) {
                def sw = new StringWriter();
                def pw = new PrintWriter(sw, true);
                GrailsUtil.deepSanitize(exception).printStackTrace(pw);
                pw.flush();
                sw.flush();
                it.write(sw.toString() + "\n")
            }
        }
    }

    /**
     * Logs a state transition in the replica commands log
     * "data/logs/replica_cmds_YYYY_MM_DD.log".
     * @param cmd the AbstractCommand instance
     * @param previousState is the status quo
     * @param newState is the state into which we are transitioning
     * @param time is the timestamp
     */
    def static logStateTransition(cmd, previousState, newState, timestamp) {
        // state logging is skipped when not configured, or states are not actually changing
        if (!ConfigurationHolder.config || !ConfigurationHolder.config.svnedge.replica.logging.commandStateTransitions) {
            return
        }
        if (previousState == newState) {
            return
        }
        // the logging block requires synchronization to insure all commands transitions are written
        synchronized (stateLogLock) {
            File logFile = getExecutionLogFile(cmd.context)
            if (!logFile) {
                def errorMsg = "Can't log replica commands: logs directory can't" +
                    " be determined with context ${cmd.context}..."
                log.error errorMsg
                throw new IllegalStateException(errorMsg)
            }
            if (!logFile.exists()) {
                try {
                    FileUtils.touch(logFile);
                    log.debug "Created the empty log file " + logFile

                } catch (Exception e) {
                    log.error "Can't create the replica command log file " +
                        "$logFile: " + e.getMessage()
                    return
                }
            }
            logFile.withWriterAppend("UTF-8") {

                def timeToken = String.format('%tH:%<tM:%<tS,%<tL', timestamp)

                def logEntry = "${timeToken} ${cmd.id} " +
                    "${cmd.class.simpleName} entering state: ${newState}"
                it.write(logEntry + "\n")
            }
        }
    }

    /**
     * When the command has been reported to CTF. Any object maintaining a 
     * reference of this can verify which state this command is in.
     */
    public void setAsReported() {
        makeTransitionToState(CommandState.REPORTED)
    }

    /**
     * @param state is the command state.
     * @return the state transition time for the given state.
     */
    public long getStateTransitionTime(CommandState state) {
        return stateTransitions.get(state) ? stateTransitions.get(state) : -1
    }

    /**
     * @return the state transition time for the given state.
     */
    public long getCurrentStateTransitionTime() {
        return stateTransitions.get(this.state) ? 
            stateTransitions.get(this.state) : -1
    }

    /**
     * @return the set of transition times the command has changed over time.
     */
    public Set<Long> getStateTransitionTimes() {
        return stateTimeTransitions.keySet()
    }

    /**
     * @param time is an existing time from getStateTransitionTimes().
     * @return the state associated with the time.
     */
    public CommandState getStateAtTime(long time) {
        return stateTimeTransitions.get(time)
    }

    /**
     * @param ctxt the execution context.
     * @return the File instance related to the log file for the command 
     * execution.
     */
    static File getExecutionLogFile(CommandsExecutionContext ctxt) {
        def now = new Date()
        //creates the file for the current day
        def logName = "replica_cmds_" + String.format('%tY_%<tm_%<td', now) +
            ".log"
        def logsDir = (ctxt) ?
                ctxt.logsDir :
                new File(ConfigurationHolder.config.svnedge.logsDirPath + "")
        if (!logsDir) {
            return null
        }
        return new File(logsDir, logName)
    }

    protected File getCommandOutputFile() {
        File logDir = new File(ConfigUtil.dataDirPath(), "logs")
        File tempLogDir = new File("temp", logDir)
        return new File("${this.id}.log", tempLogDir)
    }

    private void writeCommmandOutputFileHeading() {
        def logEntry = "${new  Date()} ${this.id} " +
                "${this.class.simpleName} params: " +
                 (this.params ? "${this.params}" : "[]") +
                "\nExecution output below\n----------------------\n"

        commandOutputStream.write(logEntry.toString().getBytes())
    }

    /**
     * Helper for executing shell commands for the ReplicaCommand heirarchy
     * Stdout and Stderr are logged to "data/logs/temp/${command.id}.log" when
     * <code>replica.logging.commandOutput</code> is set to true in Config.groovy
     * @param comand List of String command and args to execute
     * @param repo which can be
     * @param disableLogging turn off logging explicitly if it's enabled for the environment
     * @return
     */
    protected String executeShellCommand(command, repo = null, disableLogging = false) {
        def msg
        def retVal
        def result
        try {
            def commandLineService = getService("commandLineService")
            if (!disableLogging && ConfigurationHolder.config.svnedge.replica.logging.commandOutput)  {
                result = commandLineService.execute(command, commandOutputStream, commandOutputStream , null, null, true)
            }
            else {
                result = commandLineService.execute(command as String[], null, null, true)
            }
            retVal = result[0]
            msg = result[1]
        } catch (Exception e) {
            msg = "${command} failed: ${e.getMessage()}"
        }
        if (retVal != "0") {
            if (!msg) {
                msg = result[2]
            }
            log.warn(msg)
            if (null != repo) {
                repo.status = RepoStatus.ERROR
                repo.statusMsg = msg
                repo.save()
            }
            throw new IllegalStateException(msg)
        }
        return msg
    }
}
