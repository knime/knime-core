/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   18.09.2007 (thiel): created
 */
package org.knime.base.node.util.exttool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Observable;

import org.knime.base.node.util.exttool.ViewUpdateNotice.ViewType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableBoolean;

/**
 * Wraps a Java runtime process. Supports cancellations and progress messages
 * (through the {@link ExecutionContext}). Picks up the output of the launched
 * process and forwards it into the log file. It keeps a buffer of 500 lines for
 * views to display and implements the {@link Observable} interface. All
 * listeners will be provided with a {@link ViewUpdateNotice} that contains a
 * new line picked up from the external process.<br>
 * NOTE: Canceling the execution of the external process will only terminate the
 * command launched by this class. If the command created subprocesses (like a
 * shell script will do), these subprocesses will continue living. Even worse,
 * their output will not be picked up anymore, causing their output buffers to
 * fill up and potentially block the process. Thus, these processes will exist
 * in the system until they are killed manually.
 *
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class CommandExecution extends Observable {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(CommandExecution.class);

    /**
     * time (milliseconds) between two calls to checkCancel.
     */
    private static final int CANCEL_CHECK_INTERVAL = 1000;

    /**
     * the maximum number of lines stored for stdout and stderr output of the
     * external tool. (Keeping default scope so views and buffers here have the
     * same length.)
     */
    static final int MAX_OUTLINES_STORED = 500;

    private final LinkedList<String> m_extOutput = new LinkedList<String>();

    private final LinkedList<String> m_extErrout = new LinkedList<String>();

    private final String[] m_cmds;

    private final String m_cmd;

    private File m_dir;

    private String[] m_envps;

    /**
     * Creates new instance with the given command to execute. Each command line
     * argument to the command must be specified in a separate string.
     *
     * @param command The command to execute and its arguments, each specified
     *            separately.
     *
     */
    public CommandExecution(final String... command) {
        if (command == null) {
            throw new NullPointerException("Specified commands are null");
        }
        if (command.length == 0) {
            throw new IllegalArgumentException(
                    "Must specify at least one command to execute");
        }
        for (String cmd : command) {
            if (cmd == null) {
                throw new NullPointerException(
                        "Command and arguments can't be null");
            }
        }
        m_cmds = command;
        m_cmd = null;
        m_dir = null;
        m_envps = null;
    }

    /**
     * Creates new instance with the given command to execute. The command line
     * arguments for the command must be separated by a space (and quoted
     * appropriately).
     *
     * @param cmdWithArgs The command to execute and its arguments.
     *
     */
    public CommandExecution(final String cmdWithArgs) {
        if (cmdWithArgs == null) {
            throw new NullPointerException("Specified commands are null");
        }
        if (cmdWithArgs.length() == 0) {
            throw new IllegalArgumentException("Can't run an empty command");
        }

        m_cmds = null;
        m_cmd = cmdWithArgs;
        m_dir = null;
        m_envps = null;
    }

    /**
     * @see Runtime#exec(String, String[], File)
     *
     * @param dir the working directory of the subprocess, or null if the
     *            subprocess should inherit the working directory of the current
     *            process.
     */
    public void setExecutionDir(final File dir) {
        m_dir = dir;
    }

    /**
     * @see Runtime#exec(String, String[], File)
     *
     * @return the working directory of the subprocess, or null if the
     *         subprocess inherits the working directory of the current process.
     */
    public File getExecutionDir() {
        return m_dir;
    }

    /**
     * @see Runtime#exec(String, String[], File)
     *
     * @param envps array of strings, each element of which has environment
     *            variable settings in the format name=value, or null if the
     *            subprocess should inherit the environment of the current
     *            process.
     */
    public void setEnvps(final String... envps) {
        m_envps = envps;
    }

    /**
     * @see Runtime#exec(String, String[], File)
     *
     * @return an array of strings, each element of which has environment
     *         variable settings in the format name=value, or null if the
     *         subprocess inherits the environment of the current process.
     */
    public String[] getEnvps() {
        return m_envps;
    }

    /**
     * Executes the commands via the Runtime and streams the stdout as well as
     * the stderr to the logger and notifies the registered observer if a msg
     * occurs.
     *
     * @see Runtime#exec(String[], String[], File)
     *
     * @param exec The <code>ExecutionContext</code> to monitor the status of
     *            execution and to check for user cancellations.
     *
     * @return The exit value of the subprocess.
     *
     * @throws Exception If the execution terminates abnormally.
     */
    public int execute(final ExecutionContext exec) throws Exception {

        // execute the command
        int exitVal;
        try {
            exec.setProgress("Starting command");
            Runtime rt = Runtime.getRuntime();

            // Go go go !
            LOGGER.info("Launching command: '" + getCmdString() + "'");
            exec.setProgress("External command is running...");

            final Process proc;
            if (m_cmds != null) {
                assert m_cmd == null;
                proc = rt.exec(m_cmds, m_envps, m_dir);
            } else {
                assert m_cmds == null;
                proc = rt.exec(m_cmd, m_envps, m_dir);
            }

            // create a thread that periodically checks for user cancellation
            final MutableBoolean procDone = new MutableBoolean(false);
            new Thread(new CheckCanceledRunnable(proc, procDone, exec)).start();

            // pick up the output to std err from the executable
            Thread stdErrThread =
                    new Thread(new StdErrCatchRunnable(proc, exec,
                            m_extErrout));
            stdErrThread.setName("ExtTool StdErr collector");
            stdErrThread.start();

            // pick up the output to std out from the executable
            Thread stdOutThread =
                    new Thread(new StdOutCatchRunnable(proc, exec,
                            m_extOutput));
            stdOutThread.setName("ExtTool StdOut collector");
            stdOutThread.start();

            // wait until the external process finishes.
            exitVal = proc.waitFor();

            synchronized (procDone) {
                // this should terminate the check cancel thread
                procDone.setValue(true);
            }

            exec.checkCanceled();
            exec.setProgress("External command done.");
            LOGGER.info("External commands terminated with exit code: "
                    + exitVal);
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Exception e) {
            LOGGER.error("Execution failed (with exception)", e);
            throw e;
        } catch (Throwable t) {
            LOGGER.fatal("Execution failed (with exception)", t);
            throw new Exception(t);
        }

        return exitVal;
    }

    /**
     * @return the command in one string
     */
    private String getCmdString() {
        if (m_cmd != null) {
            return m_cmd;
        } else {
            assert m_cmds != null;
            assert m_cmds.length > 0;
            String allCmds = "";
            for (String cmd : m_cmds) {
                allCmds += cmd + " ";
            }
            allCmds = allCmds.substring(0, allCmds.length() - 1);
            return allCmds;
        }
    }

    /**
     * Clears the std err and out buffers.
     */
    public void clearBuffers() {
        synchronized (m_extErrout) {
            m_extErrout.clear();
        }
        synchronized (m_extOutput) {
            m_extOutput.clear();
        }
    }

    /**
     * @return a (copy of a) buffer containing the last 500 lines to standard
     *         output
     */
    public LinkedList<String> getStdOutput() {
        synchronized (m_extOutput) {
            return new LinkedList<String>(m_extOutput);
        }
    }

    /**
     * @return a (copy of a) buffer containing the last 500 lines to standard
     *         error output
     */
    public LinkedList<String> getStdErr() {
        synchronized (m_extErrout) {
            return new LinkedList<String>(m_extErrout);
        }
    }

    /**
     *
     * @author Kilian Thiel, University of Konstanz
     */
    class CheckCanceledRunnable implements Runnable {

        private final Process m_proc;

        private final MutableBoolean m_done;

        private final ExecutionContext m_exec;

        /**
         * Creates a new runnable for a thread that should check periodically
         * for user cancellations in the specified exec context.
         *
         * @param proc the process to kill after user cancellations.
         * @param done flag which causes this runnable to finish if set true
         * @param exec to check for user cancellations.
         */
        CheckCanceledRunnable(final Process proc, final MutableBoolean done,
                final ExecutionContext exec) {
            m_proc = proc;
            m_done = done;
            m_exec = exec;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            synchronized (m_done) {
                while (!m_done.booleanValue()) {
                    try {
                        m_exec.checkCanceled();
                    } catch (CanceledExecutionException cee) {
                        // blow away the running external process
                        // doesn't kill sub processes though!
                        m_proc.destroy();
                        return;
                    }
                    try {
                        m_done.wait(CANCEL_CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
            }
        }
    }

    /**
     * @author Kilian Thiel, University of Konstanz
     */
    class StdErrCatchRunnable implements Runnable {

        private final Process m_proc;

        private final ExecutionContext m_exec;

        private final LinkedList<String> m_outBuffer;

        /**
         * A runnable for a thread that picks up the error output from the
         * specified process. It stops reading if the execution context gets
         * canceled.
         *
         * @param proc the process to read the error output from
         * @param exec used to check for user cancellations.
         * @param outputBuffer the list to add the output lines to
         */
        StdErrCatchRunnable(final Process proc, final ExecutionContext exec,
                final LinkedList<String> outputBuffer) {
            m_proc = proc;
            m_exec = exec;
            m_outBuffer = outputBuffer;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            final ViewUpdateNotice notice =
                    new ViewUpdateNotice(ViewType.stderr);

            InputStream stderr = m_proc.getErrorStream();
            BufferedReader berr =
                    new BufferedReader(new InputStreamReader(stderr));
            String line = null;
            try {
                while ((line = berr.readLine()) != null) {
                    LOGGER.debug("STDERR: " + line);
                    synchronized (m_outBuffer) {
                        m_outBuffer.add(line);
                        if (m_outBuffer.size() > MAX_OUTLINES_STORED) {
                            // we keep no more than 500 lines
                            m_outBuffer.removeFirst();
                        }
                    }
                    notice.setNewLine(line);
                    CommandExecution.this.setChanged();
                    CommandExecution.this.notifyObservers(notice);
                    // stop reading lines when user cancels
                    m_exec.checkCanceled();
                }
            } catch (IOException ioe) {
                LOGGER.error("I/O Error while trying to read from"
                        + "the std err of the external process!!"
                        + " Giving up.", ioe);
            } catch (CanceledExecutionException cee) {
                LOGGER.debug("STDERR: ....Canceled..."
                        + " not reading any output anymore");
                synchronized (m_outBuffer) {
                    m_outBuffer.add("....Canceled..."
                            + " not reading any output anymore");
                }
            } finally {
                try {
                    berr.close();
                } catch (IOException ioe) {
                    // then don't close it..
                }
            }
        }
    }

    /**
     * @author Kilian Thiel, University of Konstanz
     */
    class StdOutCatchRunnable implements Runnable {

        private final Process m_proc;

        private final ExecutionContext m_exec;

        private final LinkedList<String> m_outBuffer;

        /**
         * A runnable for a thread that picks up the output from the specified
         * process. It stops reading if the execution context gets canceled.
         *
         * @param proc the process to read the output from
         * @param exec used to check for user cancellations.
         * @param outputBuffer list to add output lines to
         */
        StdOutCatchRunnable(final Process proc, final ExecutionContext exec,
                final LinkedList<String> outputBuffer) {
            m_proc = proc;
            m_exec = exec;
            m_outBuffer = outputBuffer;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            final ViewUpdateNotice notice =
                    new ViewUpdateNotice(ViewType.stdout);

            InputStream stdout = m_proc.getInputStream();
            BufferedReader bout =
                    new BufferedReader(new InputStreamReader(stdout));
            String line = null;
            try {
                while ((line = bout.readLine()) != null) {
                    LOGGER.debug("STDOUT: " + line);
                    synchronized (m_outBuffer) {
                        m_outBuffer.add(line);
                        if (m_outBuffer.size() > MAX_OUTLINES_STORED) {
                            // we keep no more than 500 lines
                            m_outBuffer.removeFirst();
                        }
                    }

                    notice.setNewLine(line);
                    CommandExecution.this.setChanged();
                    CommandExecution.this.notifyObservers(notice);
                    // stop reading lines when user cancels
                    m_exec.checkCanceled();
                }
            } catch (IOException ioe) {
                LOGGER.error("I/O Error while trying to read from"
                        + "the std out of the external process!!"
                        + " Giving up.", ioe);
            } catch (CanceledExecutionException cee) {
                LOGGER.debug("STDOUT: ....Canceled..."
                        + " not reading any output anymore");
                synchronized (m_outBuffer) {
                    m_outBuffer.add("....Canceled..."
                            + " not reading any output anymore");
                }
            } finally {
                try {
                    bout.close();
                } catch (IOException ioe) {
                    // then don't close it..
                }
            }
        }
    }
}
