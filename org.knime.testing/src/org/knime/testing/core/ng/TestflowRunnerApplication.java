/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.BatchExecutor;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.repository.RepositoryManager;
import org.osgi.framework.FrameworkUtil;

import com.knime.enterprise.client.filesystem.util.WorkflowDownloadApplication;

/**
 * This application executes the testflows and writes the results into XML files identical to the one produced by ANT's
 * &lt;junit> task. They can then be analyzed further.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestflowRunnerApplication implements IApplication {
    private String m_workflowNamePattern;

    private String m_workflowPathPattern;

    private final Collection<File> m_rootDirs = new ArrayList<File>();

    private String m_serverUri;

    private String m_xmlResultFile;

    private String m_xmlResultDir;

    private final TestrunConfiguration m_runConfiguration = new TestrunConfiguration();

    private volatile boolean m_stopped = false;

    private UntestedNodesTest m_untestedNodesTest;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        final long globalStartTime = System.currentTimeMillis();
        // we need a display, initialized as early as possible, otherwise closing JFrames may result
        // in X errors (BadWindow) under Linux
        PlatformUI.createDisplay();

        // make sure the logfile doesn't get split.
        System.setProperty(KNIMEConstants.PROPERTY_MAX_LOGFILESIZE, "-1");

        Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_rootDirs.isEmpty() && (m_serverUri == null))
                || ((m_xmlResultFile == null) && (m_xmlResultDir == null))) {
            printUsage();
            return EXIT_OK;
        }

        final AbstractXMLResultWriter resultWriter;
        if (m_xmlResultDir != null) {
            File xmlResultDir = new File(m_xmlResultDir);
            if (!xmlResultDir.exists() && !xmlResultDir.mkdirs()) {
                throw new IOException("Can not create directory for result files " + m_xmlResultDir);
            }
            resultWriter = new XMLResultDirWriter(xmlResultDir);
        } else {
            File xmlResultFile = new File(m_xmlResultFile);
            if (!xmlResultFile.getParentFile().exists() && !xmlResultFile.getParentFile().mkdirs()) {
                throw new IOException("Can not create directory for results file " + m_xmlResultFile);
            }
            resultWriter = new XMLResultFileWriter(xmlResultFile);
        }

        context.applicationRunning();

        if ((m_rootDirs.size() > 0) && m_runConfiguration.isLoadSaveLoad()) {
            // copy all workflows into a temporary directory because they will be modified by the load-save-load test
            copyRootDirs();
        }

        if (m_serverUri != null) {
            m_rootDirs.add(downloadWorkflows());
        }

        // this is to load the repository plug-in
        RepositoryManager.INSTANCE.toString();
        // and this initialized the image repository in the main thread; otherwise resolving old node factories
        // in FileSingleNodeContainerPersistor will fail (see bug# 4464)
        ImageRepository.getImage(SharedImages.Busy);

        final Display display = Display.getCurrent();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> callabale = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.currentThread().setName("Testflow executor");
                try {
                    return runAllTests(resultWriter, globalStartTime);
                } finally {
                    stop();
                    display.wake();
                }
            }
        };
        Future<Integer> result = executor.submit(callabale);
        dispatchLoop(display);

        return result.get();
    }



    private void dispatchLoop(final Display display) {
        while (!m_stopped) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void copyRootDirs() throws IOException {
        Collection<File> newRootDirs = new ArrayList<File>();

        File tempDir = FileUtil.createTempDir("tempTestRootDirs");

        for (File rootDir : m_rootDirs) {
            File tempRoot = new File(tempDir, rootDir.getName());
            FileUtil.copyDir(rootDir, tempRoot);
            newRootDirs.add(tempRoot);
        }

        m_rootDirs.clear();
        m_rootDirs.addAll(newRootDirs);
    }

    /**
     * Searches the root directory for testflows and executes each of them.
     *
     * @param resultWriter the result writer for the test results
     * @param globalStartTime the time when the whole application was started
     * @throws IOException if an I/O error occurs
     * @throws TransformerException if the results cannot be written properly
     */
    private int runAllTests(final AbstractXMLResultWriter resultWriter, final long globalStartTime) throws IOException,
        TransformerException {
        TestflowCollector registry = new TestflowCollector(m_workflowNamePattern, m_workflowPathPattern, m_rootDirs);
        Collection<WorkflowTestSuite> allTestFlows = registry.collectTestCases(m_runConfiguration);

        if (allTestFlows.size() == 0) {
            System.err.println("No testflows found, exiting");
            return 1;
        }

        int maxNameLength = 0;
        for (WorkflowTestSuite testFlow : allTestFlows) {
            maxNameLength = Math.max(maxNameLength, testFlow.getName().length());
        }

        final PrintStream sysout = System.out; // we save and use the copy because some test may re-assign it
        final PrintStream syserr = System.err; // we save and use the copy because some test may re-assign it
        resultWriter.startSuites();
        for (WorkflowTestSuite testFlow : allTestFlows) {
            if (m_stopped) {
                syserr.println("Tests aborted");
                break;
            }
            sysout.printf("=> Running %-" + maxNameLength + "s...", testFlow.getName());
            long startTime = System.currentTimeMillis();
            WorkflowTestResult result = WorkflowTestSuite.runTest(testFlow, resultWriter);
            long duration = System.currentTimeMillis() - startTime;
            long totalRuntime = System.currentTimeMillis() - globalStartTime;
            if (result.errorCount() > 0) {
                sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "ERROR", (duration / 1000.0), (totalRuntime / 1000.0));
            } else if (result.failureCount() > 0) {
                sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "FAILURE", (duration / 1000.0), (totalRuntime / 1000.0));
            } else {
                sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "OK", (duration / 1000.0), (totalRuntime / 1000.0));
            }
            resultWriter.addResult(result);

            if (m_untestedNodesTest != null) {
                m_untestedNodesTest.addNodesUnderTest(testFlow.m_context.getNodesUnderTest());
            }
        }

        if (m_untestedNodesTest != null) {
            // check for untested nodes
            WorkflowTestResult result = new WorkflowTestResult(m_untestedNodesTest);
            result.addListener(resultWriter);
            m_untestedNodesTest.run(result);
            resultWriter.addResult(result);
        }

        resultWriter.endSuites();

        return EXIT_OK;
    }

    /**
     * Extracts from the passed object the arguments. Returns <code>true</code> if everything went smooth,
     * <code>false</code> if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return <code>true</code> if the members were set according to the command line arguments, <code>false</code>, if
     *         an error message was printed and the application must exit.
     * @throws CoreException if preferences cannot be imported
     * @throws FileNotFoundException if the specified preferences file does not exist
     */
    private boolean extractCommandLineArgs(final Object args) throws FileNotFoundException, CoreException {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if ((stringArgs.length > 0) && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments. Was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '" + args.toString() + "'");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {
            if (stringArgs[i] == null) {
                i++;
            } else if (stringArgs[i].equals("-pattern")) {
                System.err.println("-pattern is now depreacted try using -include instead which matches against the " +
                		"path from the workflow root of each workflow");
                if ((m_workflowNamePattern != null) || (m_workflowPathPattern != null)) {
                    System.err.println("Multiple -pattern/-include arguments not allowed");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing pattern for tests to run.");
                    return false;
                }
                m_workflowNamePattern = stringArgs[i++];
            } else if (stringArgs[i].equals("-include")) {
                if ((m_workflowNamePattern != null) || (m_workflowPathPattern != null)) {
                    System.err.println("Multiple -pattern/-include arguments not allowed");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing pattern for tests to run.");
                    return false;
                }
                m_workflowPathPattern = stringArgs[i++];
            } else if (stringArgs[i].equals("-root")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -root.");
                    return false;
                }
                m_rootDirs.add(new File(stringArgs[i++]));
            } else if (stringArgs[i].equals("-server")) {
                if (m_serverUri != null) {
                    System.err.println("Multiple -server arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <url> for option -server.");
                    return false;
                }
                m_serverUri = stringArgs[i++];
            } else if (stringArgs[i].equals("-xmlResult")) {
                if (m_xmlResultFile != null) {
                    System.err.println("Multiple -xmlResult arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <file_name> for option -xmlResult.");
                    return false;
                }
                m_xmlResultFile = stringArgs[i++];
            } else if (stringArgs[i].equals("-xmlResultDir")) {
                if (m_xmlResultDir != null) {
                    System.err.println("Multiple -xmlResultDir arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <directory_name> for option -xmlResultDir.");
                    return false;
                }
                m_xmlResultDir = stringArgs[i++];
            } else if (stringArgs[i].equals("-save")) {
                if (m_runConfiguration.getSaveLocation() != null) {
                    System.err.println("Multiple -save arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <directory_name> for option -save.");
                    return false;
                }
                m_runConfiguration.setSaveLocation(new File(stringArgs[i++]));
            } else if (stringArgs[i].equals("-timeout")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <seconds> for option -timeout.");
                    return false;
                }
                m_runConfiguration.setTimeout(Integer.parseInt(stringArgs[i++]));
            } else if (stringArgs[i].equals("-stacktraceOnTimeout")) {
                m_runConfiguration.setStacktraceOnTimeout(true);
                i++;
            } else if (stringArgs[i].equals("-untestedNodes")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <regex> for option -untestedNodes.");
                    return false;
                }
                m_untestedNodesTest = new UntestedNodesTest(Pattern.compile(stringArgs[i++]));
            } else if (stringArgs[i].equals("-memLeaks")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <bytes> for option -memLeaks.");
                    return false;
                }
                m_runConfiguration.setAllowedMemoryIncrease(Integer.parseInt(stringArgs[i++]));
            } else if (stringArgs[i].equals("-dialogs")) {
                m_runConfiguration.setTestDialogs(true);
                i++;
            } else if (stringArgs[i].equals("-views")) {
                m_runConfiguration.setTestViews(true);
                i++;
            } else if (stringArgs[i].equals("-logMessages")) {
                m_runConfiguration.setCheckLogMessages(true);
                i++;
            } else if (stringArgs[i].equals("-ignoreNodeMessages")) {
                m_runConfiguration.setCheckNodeMessages(false);
                i++;
            } else if (stringArgs[i].equals("-deprecated")) {
                m_runConfiguration.setReportDeprecatedNodes(true);
                i++;
            } else if (stringArgs[i].equals("-loadSaveLoad")) {
                m_runConfiguration.setLoadSaveLoad(true);
                i++;
            } else if (stringArgs[i].equals("-preferences")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <file_name> for option -preferences.");
                    return false;
                }
                File prefsFile = new File(stringArgs[i++]);
                BatchExecutor.setPreferences(prefsFile);
            } else if (stringArgs[i].equals("-workflow.variable")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing variable declaration for option -workflow.variable.");
                    return false;
                }

                String[] parts = BatchExecutor.splitWorkflowVariableArg(stringArgs[i]);
                FlowVariable var = null;
                try {
                    var = BatchExecutor.createWorkflowVariable(parts);
                } catch (Exception e) {
                    System.err.println("Couldn't parse -workflow.variable argument: " + stringArgs[i] + ": "
                        + e.getMessage());
                    return false;
                }
                m_runConfiguration.addFlowVariable(var);
                i++;
            } else {
                System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
                return false;
            }
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -include <regex>: only tests matching the regular expression <regex> will be run. "
                + "The complete path of each testcase starting from the testflows' root directory is matched, "
                + "e.g. '/Misc/Workflow'.");
        System.err.println("    -root <dir_name>: optional, specifies the root dir where all testcases are located in."
                + " Multiple root arguments may be present.");
        System.err.println("    -server <uri>: optional, a KNIME server from which workflows should be downloaded"
                + " first.");
        System.err.println("                   Example: " + "knimefs://<user>:<password>@host[:port]/workflowGroup1");
        System.err.println("    -xmlResult <file_name>: specifies a single XML  file where the test results are"
                + " written to.");
        System.err.println("    -xmlResultDir <directory_name>: specifies the directory "
                + " into which each test result is written to as an XML files. Either -xmlResult or -xmlResultDir must"
                + " be provided.");
        System.err.println("    -loadSaveLoad: optional, loads, saves, and loads the workflow before execution.");
        System.err.println("    -deprecated: optional, reports deprecated nodes in workflows as failures.");
        System.err.println("    -views: optional, opens all views during a workflow test.");
        System.err.println("    -dialogs: optional, additional tests all node dialogs.");
        System.err.println("    -logMessages: optional, checks for required or unexpected log messages.");
        System.err.println("    -ignoreNodeMessages: optional, ignores any warning messages on nodes.");
        System.err.println("    -untestedNodes <regex>: optional, checks for untested nodes, only node factories "
                + "matching the regular expression are reported");
        System.err.println("    -save <directory_name>: optional, specifies the directory "
                + " into which each testflow is saved after execution. If not specified the workflows are not saved.");
        System.err.println("    -timeout <seconds>: optional, specifies the timeout for each individual workflow.");
        System.err.println("    -stacktraceOnTimeout: optional, if specified output a full stack trace in case of"
                + " timeouts.");
        System.err.println("    -memLeaks <bytes>: optional, specifies the maximum allowed increaes in heap usage for "
                + "each testflow. If not specified no test for memory leaks is performed.");
        System.err.println("    -preferences <file_name>: optional, specifies an exported preferences file that should"
                + " be used to initialize preferences");
        System.err.println("    -workflow.variable <variable-declaration>: optional, defines or overwrites workflow "
                +  "variable 'name' with value 'value' (possibly enclosed by quotes). The 'type' must be one "
                +  "of \"String\", \"int\" or \"double\".");

    }

    @Override
    public void stop() {
        m_stopped = true;
    }

    private File downloadWorkflows() throws IOException, CoreException, URISyntaxException {
        File tempDir = FileUtil.createTempDir("KNIME Testflow");
        try {
            WorkflowDownloadApplication.downloadWorkflows(m_serverUri, tempDir);
            return tempDir;
        } catch (NoClassDefFoundError err) {
            Status status =
                    new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                            "Workflow download from server not available, it seems no server client is installed", err);
            throw new CoreException(status);
        }
    }
}
