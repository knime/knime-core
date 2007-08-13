/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.repository.RepositoryManager;

/**
 * 
 */
public class KNIMETestingApplication implements IApplication {

    private boolean m_analyzeLogFile = false;

    private File m_analyzeOutputDir = null;

    private String m_testNamePattern = null;

    // if this is not null, tests are running - and can be stopped in here.
    private TestResult m_results = null;

    /**
     * {@inheritDoc}
     */
    public Object start(IApplicationContext context) throws Exception {
        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager is likely to print many errors
        // - though it will still function)
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }

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

        if (m_testNamePattern == null) {
            // if no (or not enough) command line arguments were specified:
            boolean okay = getParametersFromUser();
            if (!okay) {
                // it's not really okay -
                // but what is an error code everybody understands?
                return EXIT_OK;
            }
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
                new TestingDialog(null, m_testNamePattern, m_analyzeLogFile,
                        outPath);
        dlg.setVisible(true);
        dlg.dispose();
        if (!dlg.closedViaOK()) {
            System.out.println("Canceled.");
            return false;
        }

        m_testNamePattern = dlg.getTestNamePattern();
        m_analyzeLogFile = dlg.getAnalyzeLogFile();
        String outDir = dlg.getAnalysisOutputDir();
        if ((outDir == null) || (outDir.length() == 0)) {
            m_analyzeOutputDir = null; // use Java temp
        } else {
            m_analyzeOutputDir = new File(outDir);
        }

        return true;
    }

    /**
     * Runs the tests from the repository that match the specified pattern. If
     * the pattern is null (or empty) it pops open a dialog asking the user for
     * the tests to run.
     */
    private void runRegressionTests(final String testPattern) {

        KnimeTestRegistry registry = new KnimeTestRegistry(testPattern);
        Test tests = (TestSuite)registry.collectTestCases();

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
                new File(KNIMEConstants.getKNIMEHomeDir() + File.separator
                        + NodeLogger.LOG_FILE);

        try {
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

            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -pattern <reg_exp>: optional, "
                + "only test matching <reg_exp> will be run.");
        System.err.println("                        If not specified a dialog "
                + "opens, waiting for user input.");

        System.err.println("    -analyze <dir_name>: optional, "
                + "analyzes the log file after the run.");
        System.err.println("                         The result files will "
                + "be placed in a directory in the " + "specified dir.");
        System.err.println("                         If "
                + "<dir_name> is omitted the Java temp dir is used.");
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        if (m_results != null) {
            m_results.stop();
        }
    }
}
