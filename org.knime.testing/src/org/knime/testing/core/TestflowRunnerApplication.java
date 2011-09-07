/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.Test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.repository.RepositoryManager;

import com.knime.enterprise.client.filesystem.util.WorkflowDownloadApplication;

/**
 * This application executes the testflows and writes the results into an XML
 * file identical to the one produced by ANT's &lt;junit> task. This can then be
 * analyzed further.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestflowRunnerApplication implements IApplication {
    private boolean m_analyzeLogFile = false;

    private String m_testNamePattern;

    private String m_rootDir;

    private String m_serverUri;

    private String m_xmlResult;

    private File m_analyzeOutputDir;

    private static Test testSuite;

    /**
     * This method is called by {@link JUnitTestRunner} to retrieve the tests to
     * run.
     *
     * @return a test suite
     */
    public static Test suite() {
        return testSuite;
    }

    @Override
    public Object start(final IApplicationContext context) throws Exception {
        // make sure the logfile doesn't get split.
        System.setProperty(KNIMEConstants.PROPERTY_MAX_LOGFILESIZE, "-1");

        // this is to load the repository plug-in
        RepositoryManager.INSTANCE.toString();

        context.applicationRunning();

        Object args =
                context.getArguments()
                        .get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_testNamePattern == null)
                || ((m_rootDir == null) && (m_serverUri == null))
                || (m_xmlResult == null)) {
            printUsage();
            return EXIT_OK;
        }


        if (m_serverUri != null) {
            downloadWorkflows();
        }

        KnimeTestRegistry registry =
                new KnimeTestRegistry(m_testNamePattern, new File(m_rootDir),
                        null);
        testSuite = registry.collectTestCases();
        // TODO add analysis of log file via junit tests

        JUnitTest junitTest =
                new JUnitTest(TestflowRunnerApplication.class.getName());

        JUnitTestRunner runner =
                new JUnitTestRunner(junitTest, false, false, false, this
                        .getClass().getClassLoader());
        XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
        OutputStream out = new FileOutputStream(new File(m_xmlResult));
        formatter.setOutput(out);
        runner.addFormatter(formatter);
        runner.run();
        out.close();

        if (m_analyzeLogFile) {
            analyzeLogFile();
        }
        return EXIT_OK;
    }

    private boolean analyzeLogFile() {

        if (m_analyzeOutputDir != null) {
            // create the output folder, if it doesn't exist
            if (!m_analyzeOutputDir.exists()) {
                boolean success = m_analyzeOutputDir.mkdirs();
                if (!success) {
                    System.err.println("Couldn't create output dir "
                            + "for analysis results. "
                            + "Using Java temp dir instead!");
                    m_analyzeOutputDir = null;
                }
            } else {
                // output dir exists - make sure its a dir and not a file
                if (!m_analyzeOutputDir.isDirectory()) {
                    System.err.println("Specified output location is not"
                            + "a directory. Please specify a directory!");
                    return false; // exit!
                }
            }
        }

        File logfile =
                new File(KNIMEConstants.getKNIMEHomeDir(), NodeLogger.LOG_FILE);

        try {
            // extract the tail of the logfile that contains the last run:
            logfile = extractLastTestRun(logfile);
            new AnalyzeLogFile(logfile, m_analyzeOutputDir);
            return true;

        } catch (IOException ioe) {
            System.err.println("Couldn't access logfile! (in "
                    + logfile.getAbsolutePath() + ")");
            return false;
        }
    }

    /**
     * Copies the part of the specified log file that contains the log from the
     * last (possibly still running) KNIME run.
     *
     * @param logFile the log file to analyze and copy the last run from
     * @return a file in the same dir as the specified log file containing the
     *         last run of the specified file.
     */
    private File extractLastTestRun(final File logFile) throws IOException {

        final String startLine = "# Welcome to KNIME";

        File copyFile =
                new File(logFile.getParent(), "KNIMELastRunLogCopy.log");

        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        String line;
        BufferedWriter writer = new BufferedWriter(new FileWriter(copyFile));

        while ((line = reader.readLine()) != null) {
            if (line.contains(startLine) && line.endsWith("#")) {
                // (re-) open the output file, overriding any previous content
                writer = new BufferedWriter(new FileWriter(copyFile));
            }
            writer.write(line + "\n");
        }
        writer.close();

        return copyFile;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything
     * went smooth, false if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line
     *         arguments, false, if an error message was printed and the
     *         application must exit.
     */
    private boolean extractCommandLineArgs(final Object args) {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments."
                    + " (was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '"
                    + args.toString() + "')");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {
            // the "-pattern" argument sets the test name pattern (reg exp)
            if ((stringArgs[i] != null) && stringArgs[i].equals("-pattern")) {
                if (m_testNamePattern != null) {
                    System.err.println("You can't specify multiple patterns"
                            + " at the command line");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing pattern for tests to run.");
                    printUsage();
                    return false;
                }
                m_testNamePattern = stringArgs[i++];
                continue;
            }

            // "-analyze" triggers analysis of log file after running the test
            if ((stringArgs[i] != null) && stringArgs[i].equals("-analyze")) {
                if (m_analyzeLogFile) {
                    System.err.println("You can't specify multiple -analyze "
                            + "options at the command line");
                    return false;
                }

                i++;
                m_analyzeLogFile = true;

                if ((i < stringArgs.length) && (stringArgs[i] != null)
                        && (stringArgs[i].length() > 0)
                        && (stringArgs[i].charAt(0) != '-')) {
                    // if the next argument is not an option, use it as path
                    m_analyzeOutputDir = new File(stringArgs[i++]);
                }

                continue;
            }

            // "-root" specifies the root dir of all testcases
            if ((stringArgs[i] != null) && stringArgs[i].equals("-root")) {
                if (m_rootDir != null) {
                    System.err.println("You can't specify multiple -root "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -root.");
                    printUsage();
                    return false;
                }
                m_rootDir = stringArgs[i++];
                continue;
            }

            // "-server" specifies a workflow group on a server
            if ((stringArgs[i] != null) && stringArgs[i].equals("-server")) {
                if (m_serverUri != null) {
                    System.err.println("You can't specify multiple -server "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <url> for option -server.");
                    printUsage();
                    return false;
                }
                m_serverUri = stringArgs[i++];
                continue;
            }

            // "-xmlResult" specifies the result file
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResult")) {
                if (m_xmlResult != null) {
                    System.err.println("You can't specify multiple -xmlResult "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err
                            .println("Missing <file_name> for option -xmlResult.");
                    printUsage();
                    return false;
                }
                m_xmlResult = stringArgs[i++];
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

        System.err.println("    -pattern <reg_exp>: "
                + "only test matching <reg_exp> will be run.");
        System.err.println("    -root <dir_name>: optional, specifies the"
                + " root dir where all testcases are located in.");
        System.err.println("    -server <uri>: optional, a KNIME server "
                + "from which workflows should be downloaded first.");
        System.err.println("                   Example: "
                + "knimefs://<user>:<password>@host[:port]/workflowGroup1");
        System.err.println("    -analyze <dir_name>: optional, "
                + "analyzes the log file after the run.");
        System.err.println("                         The result files will "
                + "be placed in a directory in the " + "specified dir.");
        System.err.println("                         If "
                + "<dir_name> is omitted the Java temp dir is used.");
        System.err.println("    -xmlResult <file_name>: specifies the XML "
                + " file where the test results are written to.");
    }

    @Override
    public void stop() {
    }

    private void downloadWorkflows() throws Exception {
        File tempDir = FileUtil.createTempDir("KNIME Testflow");
        WorkflowDownloadApplication.downloadWorkflows(m_serverUri, tempDir);
        m_rootDir = tempDir.getCanonicalPath();
    }
}
