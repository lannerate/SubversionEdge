/*
 * CollabNet Subversion Edge
 * Copyright (C) 2010, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.console

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue 
import java.util.regex.Pattern;

import com.collabnet.svnedge.util.CommandLineOutputListener;
import com.collabnet.svnedge.util.ConfigUtil
import org.apache.commons.io.output.TeeOutputStream

class CommandLineService {

    boolean transactional = false

    def operatingSystemService

    /**
     * The token for commands listeners waiting for the output.
     */
    public static final String COMMAND_TERMINATED = "0xdeadc0de"

    int executeWithStatus(String...command, Map<String, String> env=null, 
            String input=null) {

        return Integer.parseInt(execute(command, (Map<String, String>) env, 
            input)[0])
    }

    String executeWithOutput(String...command, Map<String, String> env=null, 
            String input=null) {

        return execute(command, (Map<String, String>) env, input)[1]
    }

    /**
     * version of executeWithStatus which suppresses logging
     * @param command vararg list of command and arguments
     * @param env process builder additions to
     * @param input
     * @return int exitcode
     */
    int executeWithStatusQuietly(String...command, Map<String, String> env=null,
                          String input=null) {
        return Integer.parseInt(execute(command, (Map<String, String>) env, 
            input, true)[0])
    }

    /**
     * version of executeWithOutput which suppresses logging
     * @param command vararg list of command and arguments
     * @param env process builder additions to
     * @param input
     * @return int exitcode
     */
    String executeWithOutputQuietly(String...command, Map<String, String> env=null,
                             String input=null) {
        return execute(command, (Map<String, String>) env, input, true)[1]
    }

    /**
     * this method will launch the input command and return immediately without
     * waiting for result or output
     */
    void executeDetached(String command)  {
        Runtime.getRuntime().exec(command)
        return
    }

    /**
     *
     * @param command vararg list of command and arguments
     * @param env Map environment
     * @param input input to provide to the command
     * @param quiet when true, most logging is suppressed (for security, eg)
     * @return String[] of exit code, out, and err
     */
    String[] execute(String... command, Map<String, String> env=null, 
            String input=null, boolean quiet=false) {

        Process p = startProcess(command, env, quiet)
        def output = new StringBuffer(512)
        def error = new StringBuffer(512)
        if (input) {
            p.out.write(input.getBytes())
        }
        p.out.close()
        p.waitForProcessOutput(output, error)
        def exitStatus = p.waitFor()
        // logging command and output can be suppressed
        if (!quiet) {
            log.debug("Command: " + command + " result=" + exitStatus)

            if (output.length() > 0) {
                log.debug("Process output: " + output)
            }
            if (error.length() > 0) {
                if (exitStatus == 0) {
                    // Some apps write to stderr even though they start normally,
                    // e.g. httpd
                    log.debug("Process err output: " + error)
                } else {
                    log.error("Exit status=" + exitStatus +
                             " Process err output: " + error)
                }
            }
        }
        else {
            // limited logging when requested
            log.debug("Command '${command[0]}' executed with return code: " + exitStatus)
        }
        return [String.valueOf(exitStatus), output.toString(), error.toString()]
    }

    private static final Collection<String> INTERESTING_ENVVARS = 
        new HashSet(["JAVA_HOME", "LD_PRELOAD", "PWD", "HOSTNAME", 
        "SSH_TTY", "LOGNAME", "LD_LIBRARY_PATH", "SSH_CONNECTION", 
        "SHELL", "PATH", "USER", "HOME", "PYTHONPATH"]) 

    Process startProcess(List command, Map<String, String> addEnv=null,
        boolean quiet=false) {
        
        return startProcess(command as String[], addEnv, quiet)
    }

    Process startProcess(String... command, Map<String, String> addEnv=null, 
            boolean quiet=false) {

        ProcessBuilder pb = new ProcessBuilder(command)
        Map<String, String> env = pb.environment();
        if (null != addEnv) {
            env.putAll(addEnv)
        }
        if (log.isDebugEnabled()) {
            log.debug(env.findAll({key, value -> 
                INTERESTING_ENVVARS.contains(key)})
                .collect({key, value -> key + ":" + value}))
        }
        // look for "addEnv['PWD'] and use for process working dir if found
        String wd = addEnv?.PWD ?: ConfigUtil.appHome()
        pb.directory(new File(wd))
        if (!quiet) {
            log.debug("Calling pb.start() for command=" + command)
        }
        return pb.start()
    }

    Process startProcessWithInputStream(List command, InputStream input, 
        OutputStream out = null, OutputStream err = null, Map<String, String> addEnv=null,
        boolean quiet=false) {
    
        Process p = startProcess(command as String[], addEnv, quiet)
        if (input) {
            runAsync { 
                try {
                    p.out << input
                } catch (IOException e) {
                    if (e.message == "Stream closed") {
                        log.warn("Command line stdin closed before input was consumed.")
                    } else {
                        log.warn(e.message, e)
                    }
                    // exit the process with an error code
                    p.destroy()
                } finally {
                    p.out.close()
                }
            }
        }
        if (out && err) {
            p.consumeProcessOutput(out, err)
        } else if (out) {
            p.consumeProcessOutput(out, out)
        } else {
            p.consumeProcessOutput()
        }
        return p
    }
    
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("sorry|password", Pattern.CASE_INSENSITIVE)

    /**
     * Executes the command with the assumption that it will either request a password or return
     * fairly quickly.  Returns whether process requests a password.
     */
    boolean testForPassword(String... command, Map<String, String> env=null) {
        Process p = startProcess(command, env)
        def output = new StringBuffer(512)
        def error = new StringBuffer(512)
        p.consumeProcessOutput(output, error)
        p.out.close()
        Integer exitStatus = null
        boolean isPassword = false
        try {
            exitStatus = p.waitFor()
            isPassword = PASSWORD_PATTERN.matcher(error).find()
            if (!isPassword) {
                isPassword = PASSWORD_PATTERN.matcher(output).find()
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted testForPassword error=" + error + " out=" + output +
                    " isPassword? " + isPassword)
            p.destroy()
        }
        log.debug("testForPassword command: " + command + " result=" + exitStatus)
        if (output.length() > 0) {
            log.debug("Process output: " + output)
        }
        if (error.length() > 0) {
            log.debug("Process error: " + error)
        }
        isPassword
    }

    /**
     * Creates a file: scheme URI which is usable by svn command line clients
     */
    def createSvnFileURI(File f) {
        // toURI returns a "file:/Drive:/... URI in windows, but 
        // svn expects 3 preceding slashes e.g. file:///Drive:/...
        def uri = f.toURI().toString()
        if (uri.startsWith("file:/") && uri.charAt(6) != '/') {
            uri = "file:///" + uri.substring(6)    
        } else if (uri.startsWith('file:////')) {
            uri = "file://" + uri.substring(9)
        }
        return uri
    }

    /**
     * Executes the given command and outputs the lines to a blocking queue. The proper use will require the
     * client to call commandLines.take(), as it will block waiting for a element in the queue.
     * @param command an OS command execution.
     * @param commandLines the queue waiting for the command.
     */
    void executeWithCommandLineListener(String command, BlockingQueue<String> commandLines) {
        def execProcess = Runtime.getRuntime().exec(command)
        def line
        def th = Thread.start {
            def reader = new InputStreamReader(execProcess.getInputStream())
            while ((line = reader.readLine()) != null) {
                commandLines.offer(line)
            }
            commandLines.offer(COMMAND_TERMINATED)
            reader.close();
        }
    }

    /**
     * A Non-blocking method that executes the given command in the background
     * and returns a listener reference to read the lines.
     * @param command is the command to be executed.
     * @return an instance of the {@link RealTimeCommandLineListener} to
     * wait and navigate through the output lines.
     * @throws IOException in case the command does not exist or any other OS
     * error.
     */
    public CommandLineOutputListener executeAsync(String command) throws IOException {
        def outputQueue = new LinkedBlockingQueue<String>()
        def listener = new CommandLineOutputListener(outputQueue)
        executeWithCommandLineListener(command, outputQueue)
        return listener
    }

    /**
     * Execute variant that takes output streams
     * @param command list of command and arguments
     * @param env Map environment (optional)
     * @param input input to provide to the command (optional)
     * @param quiet when true, most logging is suppressed (for secuurity, eg)
     * @param output OutputStream to which to stream stdout
     * @param error OutputStream to which to stream stderr
     * @return String[] of exit code, out, and err
     */
    String[] execute(List command, OutputStream outputStream,
            OutputStream errorStream, Map<String, String> env=null,
            String input=null, Boolean quiet=false) {

        Process p = startProcess(command as String[], env, quiet)
        if (input) {
            p.out.write(input.getBytes())
        }
        p.out.close()

        // ByteArray stream to capture command output as returned by this method
        ByteArrayOutputStream outputByteArray = new ByteArrayOutputStream(512)
        ByteArrayOutputStream errorByteArray = new ByteArrayOutputStream(512)

        // Optionally, streams provided by caller will also receive the process output
        OutputStream stdout = (outputStream) ? new TeeOutputStream (outputByteArray, outputStream) : outputByteArray
        OutputStream stderr = (errorStream) ? new TeeOutputStream (errorByteArray, errorStream) : errorByteArray

        p.waitForProcessOutput(stdout, stderr)
        def exitStatus = p.waitFor()
        String outString = new String(outputByteArray.toByteArray(), "UTF-8");
        String errorString = new String(errorByteArray.toByteArray(), "UTF-8");
        // console log output of command tokens can be suppressed with "quiet" param
        if (!quiet) {
            log.debug("Command: " + command + " result=" + exitStatus)

            if (outString.length() > 0) {
                log.debug("Process output: " + outString)
            }
            if (errorString.length() > 0) {
                if (exitStatus == 0) {
                    // Some apps write to stderr even though they start normally,
                    // e.g. httpd
                    log.debug("Process err output: " + errorString)
                } else {
                    log.error("Exit status=" + exitStatus +
                             " Process err output: " + errorString)
                }
            }
        }
        else {
            // limited logging when requested
            log.debug("Command '${command[0]}' executed with return code: " + exitStatus)
        }
        return [String.valueOf(exitStatus), outString, errorString] as String[]
    }

    def getPathOwner(def path) {
        def result = getPathData(path)
        return result.length > 2 ? result[2] : "nobody"
    }
    
    def getPathGroup(def path) {
        def result = getPathData(path)
        return result.length > 3 ? result[3] : "nobody"
    }
    
    private String[] getPathData(def path) {
        if (operatingSystemService.isWindows()) {
            return new String[0]
        }
        //Sometimes ls -ld output coloumns are separated by double space.
        //For ex drwxr-xr-x  7 rajeswari __cubitu 4096 May 14 01:45 data/
        return executeWithOutput("ls", "-dl", path).replaceAll(" +", " ").split(" ")
    }
}
