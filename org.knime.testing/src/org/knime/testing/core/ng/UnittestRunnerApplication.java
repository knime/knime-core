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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror.JUnitTestRunnerMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
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
    private volatile boolean m_stopped;

    private File m_destDir;

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

        Collection<Class<?>> allTests = AllJUnitTests.getAllJunitTests();
        int maxNameLength = 0;
        for (Class<?> testClass : allTests) {
            maxNameLength = Math.max(maxNameLength, testClass.getName().length());
        }

        final PrintStream sysout = System.out; // we save and use the copy because some test may re-assign it
        final PrintStream syserr = System.err;
        // run the tests
        for (Class<?> testClass : allTests) {
            if (m_stopped) {
                syserr.println("Tests aborted");
                break;
            }

            sysout.printf("=> Running %-" + maxNameLength + "s ...", testClass.getName());
            long startTime = System.currentTimeMillis();
            JUnitTest junitTest = new JUnitTest(testClass.getName());
            final JUnitTestRunner runner =
                    new JUnitTestRunner(junitTest, false, false, false, testClass.getClassLoader());
            XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
            OutputStream out = new FileOutputStream(new File(m_destDir, testClass.getName() + ".xml"));
            formatter.setOutput(out);
            runner.addFormatter(formatter);

            Writer stdout = new Writer() {
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
            Writer stderr = new Writer() {

                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    runner.handleErrorOutput(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            };

            NodeLogger.addWriter(stdout, LEVEL.DEBUG, LEVEL.FATAL);
            NodeLogger.addWriter(stderr, LEVEL.ERROR, LEVEL.FATAL);

            try {
                System.setOut(new PrintStream(new WriterOutputStream(stdout), false, "UTF-8"));
                System.setErr(new PrintStream(new WriterOutputStream(stderr), false, "UTF-8"));
                runner.run();
                System.out.flush();
                System.err.flush();
            } finally {
                System.setOut(sysout);
                System.setErr(syserr);
            }
            NodeLogger.removeWriter(stderr);
            NodeLogger.removeWriter(stdout);

            out.close();

            long duration = System.currentTimeMillis() - startTime;
            switch (runner.getRetCode()) {
                case JUnitTestRunnerMirror.SUCCESS:
                    sysout.printf("%-7s (%.3f s)%n", "OK", (duration / 1000.0));
                    break;
                case JUnitTestRunnerMirror.FAILURES:
                    sysout.printf("%-7s (%.3f s)%n", "FAILURE", (duration / 1000.0));
                    break;
                case JUnitTestRunnerMirror.ERRORS:
                    sysout.printf("%-7s (%.3f s)%n", "ERROR", (duration / 1000.0));
                    break;
                default:
                    sysout.printf("%-7s (%.3f s)%n", "UNKNOWN", (duration / 1000.0));
                    break;
            }
        }

        return EXIT_OK;
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
