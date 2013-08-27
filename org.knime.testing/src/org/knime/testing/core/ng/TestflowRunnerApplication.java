/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 */
package org.knime.testing.core.ng;

import java.io.File;
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
import org.knime.core.util.FileUtil;
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
    private String m_testNamePattern;

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
        // we need a display, initialized as early as possible, otherwise closing JFrames may result
        // in X errors (BadWindow) under Linux
        PlatformUI.createDisplay();

        // make sure the logfile doesn't get split.
        System.setProperty(KNIMEConstants.PROPERTY_MAX_LOGFILESIZE, "-1");

        Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_testNamePattern == null)
                || (m_rootDirs.isEmpty() && (m_serverUri == null))
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

        final Display display = Display.getCurrent();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> callabale = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.currentThread().setName("Testflow executor");
                try {
                    return runAllTests(resultWriter);
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
     * @throws IOException if an I/O error occurs
     * @throws TransformerException if the results cannot be written properly
     */
    private int runAllTests(final AbstractXMLResultWriter resultWriter) throws IOException, TransformerException {
        TestflowCollector registry = new TestflowCollector(m_testNamePattern, m_rootDirs);
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
            WorkflowTestResult result = WorkflowTestSuite.runTest(testFlow, resultWriter);
            if (result.errorCount() > 0) {
                sysout.println("ERROR");
            } else if (result.failureCount() > 0) {
                sysout.println("FAILURE");
            } else {
                sysout.println("OK");
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
     */
    private boolean extractCommandLineArgs(final Object args) {
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
            // the "-pattern" argument sets the test name pattern (reg exp)
            if ((stringArgs[i] != null) && stringArgs[i].equals("-pattern")) {
                if (m_testNamePattern != null) {
                    System.err.println("Multiple -pattern arguments not allowed");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing pattern for tests to run.");
                    printUsage();
                    return false;
                }
                m_testNamePattern = stringArgs[i++];
                continue;
            }

            // "-root" specifies the root dir of all testcases
            if ((stringArgs[i] != null) && stringArgs[i].equals("-root")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -root.");
                    printUsage();
                    return false;
                }
                m_rootDirs.add(new File(stringArgs[i++]));
                continue;
            }

            // "-server" specifies a workflow group on a server
            if ((stringArgs[i] != null) && stringArgs[i].equals("-server")) {
                if (m_serverUri != null) {
                    System.err.println("Multiple -server arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <url> for option -server.");
                    printUsage();
                    return false;
                }
                m_serverUri = stringArgs[i++];
                continue;
            }

            // "-xmlResult" specifies the result file
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResult")) {
                if (m_xmlResultFile != null) {
                    System.err.println("Multiple -xmlResult arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <file_name> for option -xmlResult.");
                    printUsage();
                    return false;
                }
                m_xmlResultFile = stringArgs[i++];
                continue;
            }

            // "-xmlResultDir" specifies the result directory
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResultDir")) {
                if (m_xmlResultDir != null) {
                    System.err.println("Multiple -xmlResultDir arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <directory_name> for option -xmlResultDir.");
                    printUsage();
                    return false;
                }
                m_xmlResultDir = stringArgs[i++];
                continue;
            }

            // "-save" specifies the destination directory for saved workflows
            if ((stringArgs[i] != null) && stringArgs[i].equals("-save")) {
                if (m_runConfiguration.getSaveLocation() != null) {
                    System.err.println("Multiple -save arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <directory_name> for option -save.");
                    printUsage();
                    return false;
                }
                m_runConfiguration.setSaveLocation(new File(stringArgs[i++]));
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-timeout")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <seconds> for option -timeout.");
                    printUsage();
                    return false;
                }
                m_runConfiguration.setTimeout(Integer.parseInt(stringArgs[i++]));
                continue;
            }


            if ((stringArgs[i] != null) && stringArgs[i].equals("-untestedNodes")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <regex> for option -untestedNodes.");
                    printUsage();
                    return false;
                }
                m_untestedNodesTest = new UntestedNodesTest(Pattern.compile(stringArgs[i++]));
                continue;
            }


            if ((stringArgs[i] != null) && stringArgs[i].equals("-dialogs")) {
                m_runConfiguration.setTestDialogs(true);
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-views")) {
                m_runConfiguration.setTestViews(true);
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-logMessages")) {
                m_runConfiguration.setCheckLogMessages(true);
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-deprecated")) {
                m_runConfiguration.setReportDeprecatedNodes(true);
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-loadSaveLoad")) {
                m_runConfiguration.setLoadSaveLoad(true);
                i++;
                continue;
            }


            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -pattern <regex>: only test matching the regular expression <regex> will be run.");
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
        System.err.println("    -untestedNodes <regex>: optional, checks for untested nodes, only node factories "
                + "matching the regular expression are reported");
        System.err.println("    -save <directory_name>: optional, specifies the directory "
                + " into which each testflow is saved after execution. If not specified the workflows are not saved.");
        System.err.println("    -timeout <seconds>: optional, specifies the timeout for each individual workflow.");
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
