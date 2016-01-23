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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Formatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror.JUnitTestRunnerMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.workflow.BatchExecutor;

/**
 * This application runs all Unit tests it can find. It collects all classes by querying implementations of
 * {@link org.knime.testing.core.AbstractTestcaseCollector} that are registered at the extension point.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class UnittestRunnerApplication implements IApplication {
    private static class NoSysoutFormatter extends XMLJUnitResultFormatter {
        /**
         * {@inheritDoc}
         */
        @Override
        public void setSystemOutput(final String out) {
            // do nothig
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSystemError(final String out) {
            // do nothing
        }
    }


    private volatile boolean m_stopped;

    private volatile boolean m_leftDispatchLoop = false;

    private File m_destDir;

    private boolean m_outputToSeparateFile;

    private Pattern m_includePattern = Pattern.compile(".+");

    private void dispatchLoop(final Display display) {
        while (!m_stopped) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        m_leftDispatchLoop = true;
    }


    @Override
    public Object start(final IApplicationContext context) throws Exception {
        PlatformUI.createDisplay(); // create a display because some tests may need it
        context.applicationRunning();
        Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_destDir == null)) {
            printUsage();
            return EXIT_OK;
        }
        if (!m_destDir.isDirectory() && !m_destDir.mkdirs()) {
            throw new IOException("Could not create destination directory '" + m_destDir + "'");
        }

        final Collection<Class<?>> allTests = AllJUnitTests.getAllJunitTests();
        int l = 0;
        for (Class<?> testClass : allTests) {
            l = Math.max(l, testClass.getName().length());
        }
        final int maxNameLength = l;

        final Display display = Display.getCurrent();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> callabale = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.currentThread().setName("Unittest executor");
                try {
                    runAllTests(allTests, maxNameLength);
                    return EXIT_OK;
                } finally {
                    stop();
                    while (!m_leftDispatchLoop) {
                        display.wake();
                        Thread.sleep(100);
                    }
                }
            }
        };
        Future<Integer> result = executor.submit(callabale);
        dispatchLoop(display);

        return result.get();
    }


    private void runAllTests(final Collection<Class<?>> allTests, final int maxNameLength) throws IOException {
        final PrintStream sysout = System.out; // we save and use the copy because some test may re-assign it
        final PrintStream syserr = System.err;
        // run the tests
        long globalStartTime = System.currentTimeMillis();
        for (Class<?> testClass : allTests) {
            if (m_stopped) {
                syserr.println("Tests aborted");
                break;
            } else if (!m_includePattern.matcher(testClass.getName()).matches()) {
                continue;
            }


            sysout.printf("=> Running %-" + maxNameLength + "s ...", testClass.getName());
            long startTime = System.currentTimeMillis();
            JUnitTest junitTest = new JUnitTest(testClass.getName());
            final JUnitTestRunner runner =
                    new JUnitTestRunner(junitTest, false, false, false, testClass.getClassLoader());

            XMLJUnitResultFormatter formatter;
            Writer stdout;
            if (m_outputToSeparateFile) {
                formatter = new NoSysoutFormatter();
                stdout = new FileWriter(new File(m_destDir, testClass.getName() + "-output.txt"));
            } else {
                formatter = new XMLJUnitResultFormatter();
                stdout = new Writer() {
                    @Override
                    public void write(final char[] cbuf, final int off, final int len) throws IOException {
                        runner.handleOutput(new String(cbuf, off, len));
                    }

                    @Override
                    public void flush() throws IOException {
                    }

                    @Override
                    public void close() throws IOException {
                    }
                };
            }
            OutputStream xmlOut = new FileOutputStream(new File(m_destDir, "TEST-" + testClass.getName() + ".xml"));
            formatter.setOutput(xmlOut);
            runner.addFormatter(formatter);

            NodeLogger.addWriter(stdout, LEVEL.DEBUG, LEVEL.FATAL);

            NodeLogger logger = NodeLogger.getLogger(testClass);
            try {
                System.setOut(new PrintStream(new WriterOutputStream(stdout), false, "UTF-8"));
                System.setErr(new PrintStream(new WriterOutputStream(stdout), false, "UTF-8"));
                logger.info("================= Starting testcase " + junitTest.getName() + " =================");
                logMemoryStatus(logger);
                runner.run();
                System.out.flush();
                System.err.flush();
            } finally {
                logMemoryStatus(logger);
                logger.info("================= Finished testcase " + junitTest.getName() + " =================");
                System.setOut(sysout);
                System.setErr(syserr);
            }
            NodeLogger.removeWriter(stdout);

            xmlOut.close();

            long duration = System.currentTimeMillis() - startTime;
            long totalRuntime = System.currentTimeMillis() - globalStartTime;
            switch (runner.getRetCode()) {
                case JUnitTestRunnerMirror.SUCCESS:
                    sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "OK", (duration / 1000.0), (totalRuntime / 1000.0));
                    break;
                case JUnitTestRunnerMirror.FAILURES:
                    sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "FAILURE", (duration / 1000.0), (totalRuntime / 1000.0));
                    break;
                case JUnitTestRunnerMirror.ERRORS:
                    sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "ERROR", (duration / 1000.0), (totalRuntime / 1000.0));
                    break;
                default:
                    sysout.printf("%-7s (%7.3f s -- %8.3f s)%n", "UNKNOWN", (duration / 1000.0), (totalRuntime / 1000.0));
                    break;
            }
        }
    }

    private void logMemoryStatus(final NodeLogger logger) {
        MemoryUsage usage = WorkflowTest.getHeapUsage();

        Formatter formatter = new Formatter();
        formatter.format("===== Memory statistics: %1$,.3f MB max, %2$,.3f MB used, %3$,.3f MB free ====",
            usage.getMax() / 1024.0 / 1024.0, usage.getUsed() / 1024.0 / 1024.0,
            (usage.getMax() - usage.getUsed()) / 1024.0 / 1024.0);
        logger.info(formatter.out().toString());
    }


    @Override
    public void stop() {
        m_stopped = true;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything went smooth, false if the application
     * must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line arguments, false, if an error message was
     *         printed and the application must exit.
     * @throws CoreException if preferences cannot be imported
     * @throws FileNotFoundException if the specified preferences file does not exist
     */
    private boolean extractCommandLineArgs(final Object args) throws FileNotFoundException, CoreException {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
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
            } else if ("-xmlResultDir".equals(stringArgs[i])) {
                if (m_destDir != null) {
                    System.err.println("Multiple -xmlResultDir arguments are not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -xmlResultDir.");
                    printUsage();
                    return false;
                }
                m_destDir = new File(stringArgs[i++]);
            } else if (stringArgs[i].equals("-preferences")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <file_name> for option -preferences.");
                    return false;
                }
                File prefsFile = new File(stringArgs[i++]);
                BatchExecutor.setPreferences(prefsFile);
            } else if (stringArgs[i].equals("-outputToSeparateFile")) {
                i++;
                m_outputToSeparateFile = true;
            } else if (stringArgs[i].equals("-include")) {
                i++;
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <pattern> for option -include.");
                    printUsage();
                    return false;
                }
                m_includePattern = Pattern.compile(stringArgs[i++]);
            } else {
                System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
                return false;
            }
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -xmlResultDir <dir_name>: specifies the directory into which the test results are "
                + "written.");
        System.err.println("    -preferences <file_name>: optional, specifies an exported preferences file that should"
                + " be used to initialize preferences");
        System.err.println("    -outputToSeparateFile: optional, specifies that system out and system err are written "
            + "to a separate text file instead of being included in the XML result file (similar to Surefire)");
        System.err.println("    -include <pattern>: optional, specifies a regular expression that matches all test classes  "
                + "that should should be included in the test run; default is to include all classes");
    }

    private static class WriterOutputStream extends OutputStream {
        private final Writer m_writer;

        public WriterOutputStream(final Writer writer) {
            m_writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            m_writer.write(b);
        }
    }
}
