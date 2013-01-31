/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History: 01.08.2007 (ohl): created
 */
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.repository.RepositoryManager;

/**
 *
 */
public class KNIMETestingApplication implements IApplication {
    private boolean m_analyzeLogFile = false;

    private File m_analyzeOutputDir = null;

    private String m_testNamePattern = null;

    private String m_rootDir = null;

    // if this is not null, tests are running - and can be stopped in here.
    private TestResult m_results = null;

    private boolean m_saveTests = false;

    private boolean m_testViews;

    private boolean m_testDialogs;

    private File m_saveLocation = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        // we need a display, initialized as early as possible, otherwise closing JFrames may result
        // in X errors (BadWindow) under Linux
        PlatformUI.createDisplay();

        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager is likely to print many errors
        // - though it will still function)
        // Under Linux this must be not be true, otherwise the views will not
        // open!
        // Suddenly under windows this causes a headless exception (at least
        // if started without command line arguments)
        // if (!System.getProperty("os.name").equals("Linux")
        // && System.getProperty("java.awt.headless") == null) {
        // System.setProperty("java.awt.headless", "true");
        // }

        // make sure the logfile doesn't get split.
        System.setProperty(KNIMEConstants.PROPERTY_MAX_LOGFILESIZE, "-1");

        // this is just to load the repository plug-in
        RepositoryManager.INSTANCE.toString();

        context.applicationRunning();

        Object args =
                context.getArguments()
                        .get(IApplicationContext.APPLICATION_ARGS);

        boolean correct = extractCommandLineArgs(args);
        if (!correct) {
            // it's not really okay -
            // but what is an error code everybody understands?
            return EXIT_OK;
        }

        if ((m_testNamePattern == null) || (m_rootDir == null)) {
            // if no (or not enough) command line arguments were specified:

            final AtomicBoolean okayBoolean = new AtomicBoolean(false);
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    okayBoolean.set(getParametersFromUser());
                }
            });
            if (!okayBoolean.get()) {
                // it's not really okay -
                // but what is an error code everybody understands?
                return EXIT_OK;
            }
        }

        if (m_saveTests) {
            m_saveLocation = getRuntimeWorkspace();
        }


        // Go!
        runRegressionTests(m_testNamePattern);

        if (m_analyzeLogFile) {
            analyzeLogFile();
        }

        return EXIT_OK;
    }

    private boolean getParametersFromUser() {

        assert m_testNamePattern == null;

        String outPath = null;
        if (m_analyzeOutputDir != null) {
            outPath = m_analyzeOutputDir.getAbsolutePath();
        } else {
            // set the runtime work space as default
            outPath = Platform.getLocation().toString();
        }
        TestingDialog dlg =
                new TestingDialog(null, m_testNamePattern, findTestCases(),
                        m_analyzeLogFile, outPath);
        dlg.pack();
        dlg.setVisible(true);
        dlg.dispose();
        if (!dlg.closedViaOK()) {
            System.out.println("Canceled.");
            return false;
        }

        m_testNamePattern = dlg.getTestNamePattern();
        m_saveTests = dlg.getSaveTests();
        m_rootDir = dlg.getTestRootDir();
        m_analyzeLogFile = dlg.getAnalyzeLogFile();
        m_testDialogs = dlg.getTestDialogs();
        m_testViews = dlg.getTestViews();
        String outDir = dlg.getAnalysisOutputDir();
        if ((outDir == null) || (outDir.length() == 0)) {
            m_analyzeOutputDir = null; // use Java temp
        } else {
            m_analyzeOutputDir = new File(outDir);
        }
        if (m_rootDir == null) {
            System.err
                    .println("No root directory for the testflows specified.");
            return false;
        }

        return true;
    }

    /**
     * tries to guess a starting directory for the testcase search. Any dir with
     * "_testflow" in its name would be a good guess.
     *
     * @return
     */
    private File findTestCases() {
        try {
            // see if the testcases are somewhere located next to the plugins
            // (which is the case in a developer environment)
            URL devWorkSpace =
                    FileLocator.toFileURL(FileLocator.find(KNIMECorePlugin
                            .getDefault().getBundle(), new Path("/"), null));
            File loc = new File(devWorkSpace.getFile().toString());
            if ((!loc.exists()) || (loc.getParentFile() == null)) {
                // weird, but lets return something...
                return loc;
            }

            // go one level up and try finding a dir with suffix _testflows
            loc = loc.getParentFile();
            File[] dirs = loc.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    return false;
                }
            });
            // return the first match
            for (File dir : dirs) {
                if (dir.getName().endsWith("_testflows")) {
                    return dir;
                }
            }

            // no testflows found.
            // Find out if we are in the Eclipse/KNIME installation directory
            for (File dir : dirs) {
                if (dir.getName().startsWith("org.eclipse")) {
                    // eclispe plugins are only in installation directories
                    return null;
                }
            }

            // return the workspace dir
            return loc;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Runs the tests from the repository that match the specified pattern. If
     * the pattern is null (or empty) it pops open a dialog asking the user for
     * the tests to run.
     * @throws IOException if an I/O error occurs
     */
    private void runRegressionTests(final String testPattern) throws IOException {
        // override current encryption key supplier
        KnimeEncryption.setEncryptionKeySupplier(new EncryptionKeySupplier() {
            /**
             * @return <code>KNIME</code> always {@inheritDoc}
             */
            @Override
            public String getEncryptionKey() {
                return "KNIME";
            }
        });

        KnimeTestRegistry registry =
                new KnimeTestRegistry(testPattern, new File(m_rootDir),
                        m_saveLocation, m_testDialogs, m_testViews);
        Test tests = registry.collectTestCases(FullWorkflowTest.factory);

        System.out.println("=============  Running...  ==================");

        if (tests.countTestCases() > 0) {
            m_results = new TestResult();

            // run'em
            tests.run(m_results);

            if (m_results.shouldStop()) {
                System.out.println("  #### TESTS INTERRUPTED! ####");
            }

            printTestResult();

        } else {
            System.out.println("Nothing to test.");
        }
        System.out.println("=============================================");

    }

    private void printTestResult() {

        System.out.println("=============================================");
        System.out.println("Testing results:");
        System.out.println("----------------");
        System.out.println("Tests run:" + m_results.runCount());
        System.out.println("Errors: " + m_results.errorCount());
        System.out.println("Failures: " + m_results.failureCount());
        int good =
                m_results.runCount() - m_results.errorCount()
                        - m_results.failureCount();
        double percent = (int)(good / (m_results.runCount() / 100.0));
        System.out.println("Succeeded: " + good + " (" + percent + "%)");

        System.out.println("-------------------");
        if (m_results.errorCount() > 0) {
            System.out.println("Errors:");
            Enumeration<TestFailure> errs = m_results.errors();
            while (errs.hasMoreElements()) {
                TestFailure err = errs.nextElement();
                Test errTest = err.failedTest();
                System.out.print("     " + errTest.toString());
                System.out.print("   , Msg: " + err.exceptionMessage());
                System.out.println();
            }
        }
        if (m_results.failureCount() > 0) {
            System.out.println("Failures:");
            Enumeration<TestFailure> fails = m_results.failures();
            while (fails.hasMoreElements()) {
                TestFailure fail = fails.nextElement();
                Test failTest = fail.failedTest();
                System.out.print("     " + failTest.toString());
                System.out.print("   , Msg: " + fail.exceptionMessage());
                System.out.println();
            }
        }
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

            // "-save" saves the testflows after execution
            if ("-save".equals(stringArgs[i])) {
                i++;
                m_saveTests = true;
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

            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        return true;
    }

    private File getRuntimeWorkspace() {
        return new File(ResourcesPlugin.getWorkspace().getRoot()
                .getLocationURI());
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -pattern <reg_exp>: optional, "
                + "only test matching <reg_exp> will be run.");
        System.err.println("                        If not specified a dialog "
                + "opens, waiting for user input.");
        System.err.println("    -save: saves the testflows after execution "
                + "in the runtime workspace");
        System.err.println("    -analyze <dir_name>: optional, "
                + "analyzes the log file after the run.");
        System.err.println("                         The result files will "
                + "be placed in a directory in the " + "specified dir.");
        System.err.println("                         If "
                + "<dir_name> is omitted the Java temp dir is used.");
        System.err.println("    -root <dir_name>: optional, specifies the"
                + " root dir where all testcases are located in.");
        System.err.println("IF -pattern OR -root IS OMITTED A DIALOG OPENS"
                + " REQUESTING USER INPUT.");

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
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (m_results != null) {
            m_results.stop();
        }
    }
}
